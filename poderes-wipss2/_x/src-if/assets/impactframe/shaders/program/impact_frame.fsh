#version 150

uniform sampler2D DiffuseSampler;
// Only bound by the full chain. The lite chain leaves UseDepth at 0 and never samples it.
uniform sampler2D DiffuseDepthSampler;
uniform vec2 OutSize;
uniform float Strength;
uniform float LinearIntensity;
uniform float Time;
uniform vec2 Center;        // focus in screen UV: the target, the broken block, the explosion
uniform float FocusRadius;  // focus size in screen heights, 0 when there is no usable focus
uniform float SafeMode;
uniform float Invert;       // 1 on frames that show as a colour negative
uniform float Tier;         // 0 light, 1 normal, 2 heavy, 3 smash
uniform float UseDepth;
uniform vec2 Direction;
uniform float DirectionAmount;
uniform float Halftone;
uniform vec3 Tint;
uniform float TintAmount;
uniform float Letterbox;
uniform vec2 DepthRange;    // near and far plane of the world projection

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);
const float TAU = 6.28318530718;
const float LINE_COUNT = 180.0;

// Hash without sin(), so the result is exact and identical on every GPU.
float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

// All taps use textureLod so they are safe inside non-uniform branches.

// Exposure lift for dark scenes. 1.0 = no change (daylight). Set once at the start of main().
float gGamma = 1.0;

// Looks at the whole frame and works out how much to brighten the darks before thresholding, so a
// night scene gets the same balance of paper and ink, and the same strong outlines, as a day scene.
// It uses the geometric mean of a 4x4 grid, so a few torches or the moon cannot fool it. Scenes whose
// average is at or above 0.4 return exactly 1.0, so daytime looks exactly as it did before.
float sceneGamma() {
    float sum = 0.0;
    for (int y = 0; y < 4; y++) {
        for (int x = 0; x < 4; x++) {
            vec2 uv = (vec2(float(x), float(y)) + 0.5) * 0.25;
            float l = dot(textureLod(DiffuseSampler, uv, 0.0).rgb, LUMA);
            sum += log(max(l, 0.01));
        }
    }
    float m = exp(sum / 16.0);
    if (m >= 0.4) {
        return 1.0;
    }
    return clamp(log(0.4) / log(m), 0.3, 1.0);
}

// Contrast-stretched luminance, used for both the threshold and the colour edge detector.
float levelAt(vec2 uv) {
    float l = pow(dot(textureLod(DiffuseSampler, uv, 0.0).rgb, LUMA), gGamma);
    return clamp((l - 0.5) * 3.0 + 0.5, 0.0, 1.0);
}

// Sobel magnitude over a kernel spread by `texel`.
float colorEdge(vec2 uv, vec2 texel) {
    float tl = levelAt(uv + vec2(-texel.x,  texel.y));
    float t  = levelAt(uv + vec2( 0.0,      texel.y));
    float tr = levelAt(uv + vec2( texel.x,  texel.y));
    float l  = levelAt(uv + vec2(-texel.x,  0.0));
    float r  = levelAt(uv + vec2( texel.x,  0.0));
    float bl = levelAt(uv + vec2(-texel.x, -texel.y));
    float b  = levelAt(uv + vec2( 0.0,     -texel.y));
    float br = levelAt(uv + vec2( texel.x, -texel.y));
    float gx = (tr + 2.0 * r + br) - (tl + 2.0 * l + bl);
    float gy = (tl + 2.0 * t + tr) - (bl + 2.0 * b + br);
    return smoothstep(0.6, 1.1, sqrt(gx * gx + gy * gy));
}

// Inverse view-space depth. It is linear across any flat surface on screen, so the second
// difference below is near zero on floors and walls and large only on real silhouettes.
float invDepth(vec2 uv) {
    float d = textureLod(DiffuseDepthSampler, uv, 0.0).r;
    float n = DepthRange.x;
    float f = DepthRange.y;
    float z = (n * f) / (f - d * (f - n));
    return 1.0 / max(z, 0.001);
}

// Silhouette edge from the world depth buffer. Draws a line wherever one object ends and
// another, or the sky, begins.
float depthEdge(vec2 uv, vec2 texel) {
    float c = invDepth(uv);
    float l = invDepth(uv - vec2(texel.x, 0.0));
    float r = invDepth(uv + vec2(texel.x, 0.0));
    float b = invDepth(uv - vec2(0.0, texel.y));
    float t = invDepth(uv + vec2(0.0, texel.y));
    float lap = abs(l + r - 2.0 * c) + abs(b + t - 2.0 * c);
    return smoothstep(0.10, 0.30, lap / max(c, 0.0001));
}

// Radial speed lines around Center. Each angular bucket holds at most one line, and the line's
// presence, width and dash pattern depend only on the bucket index. Time moves the dashes outward,
// so no line jumps or flickers. `motion` is 0 in safe mode, which freezes the dashes. Bigger tiers
// have more and thicker lines. Lines start outside the focus so they frame the target.
float directionWeight(vec2 d) {
    if (DirectionAmount <= 0.0 || dot(Direction, Direction) < 0.01) {
        return 1.0;
    }
    float align = dot(normalize(d), normalize(Direction));
    return mix(1.0, smoothstep(-0.35, 0.85, align), DirectionAmount);
}

float speedLines(vec2 uv, float aspect, float motion) {
    vec2 d = uv - Center;
    d.x *= aspect;
    float r = length(d);
    float inner = max(0.16, FocusRadius * 1.1);
    if (r < inner) {
        return 0.0;
    }

    float a = atan(d.y, d.x);
    float bucketPos = (a / TAU + 0.5) * LINE_COUNT;
    float bucket = floor(bucketPos);
    float skip = Tier > 2.5 ? 0.38 : (Tier > 1.5 ? 0.55 : 0.72);
    if (hash11(bucket + 17.0) < skip) {
        return 0.0;
    }

    // Distance from the line's centre, measured across the arc in output-height units.
    float offset = abs(bucketPos - bucket - 0.5);
    float perp = offset * r * (TAU / LINE_COUNT);
    float lineWidth = Tier > 2.5 ? 2.4 : 1.5;
    float halfWidth = lineWidth / OutSize.y;
    float line = 1.0 - smoothstep(halfWidth - 1.0 / OutSize.y, halfWidth + 1.0 / OutSize.y, perp);

    float phase = fract(r * 2.4 - Time * 1.6 * motion + hash11(bucket + 91.0) * 4.0);
    float dash = smoothstep(0.0, 0.03, phase) * (1.0 - smoothstep(0.55, 0.58, phase));
    float fade = smoothstep(inner, inner + 0.14, r) * (1.0 - smoothstep(0.85, 1.05, r));
    return line * dash * fade * directionWeight(d);
}

// Expanding shockwave rings from the focus. Heavy gets one ring, smash gets two and thicker.
float shockRing(vec2 uv, float aspect) {
    vec2 d = uv - Center;
    d.x *= aspect;
    float r = length(d);
    float speed = Tier > 2.5 ? 3.0 : 2.2;
    float width = Tier > 2.5 ? 0.020 : 0.012;
    float start = max(FocusRadius * 0.6, 0.02);

    float r1 = start + Time * speed;
    float ring = 1.0 - smoothstep(width * 0.5, width, abs(r - r1));
    ring *= 1.0 - smoothstep(0.7, 1.3, r1);

    if (Tier > 2.5 && Time > 0.07) {
        float r2 = start + (Time - 0.07) * speed;
        float ring2 = 1.0 - smoothstep(width * 0.4, width * 0.8, abs(r - r2));
        ring2 *= 1.0 - smoothstep(0.7, 1.3, r2);
        ring = max(ring, ring2);
    }
    if (r > 0.0001) {
        ring *= max(directionWeight(d), 0.25);
    }
    return ring;
}

void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;

    // The release tail fades the whole effect back to the scene.
    float boundedStrength = clamp(Strength, 0.0, 1.0);
    float amount = LinearIntensity > 0.5 ? boundedStrength : smoothstep(0.0, 0.25, boundedStrength);

    // Must come before the first levelAt() call.
    gGamma = sceneGamma();

    // fwidth needs uniform control flow, so take it before any early return.
    float level = levelAt(texCoord);
    float w = fwidth(level) * 0.7 + 0.01;
    if (amount <= 0.0) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    float safe = clamp(SafeMode, 0.0, 1.0);
    float aspect = OutSize.x / OutSize.y;
    // Bigger tiers draw thicker lines.
    vec2 texel = (2.0 * (1.0 + 0.35 * Tier)) / OutSize;

    // How close this pixel is to the thing being hit: 1 inside it, fading to 0 around it.
    vec2 fd = texCoord - Center;
    fd.x *= aspect;
    float focus = 0.0;
    if (FocusRadius > 0.0) {
        focus = 1.0 - smoothstep(FocusRadius, FocusRadius * 1.9, length(fd));
    }

    // Hard threshold to paper and ink. fwidth gives the anti-aliased edge, about one pixel wide.
    float paper = smoothstep(0.5 - w, 0.5 + w, level);
    vec3 paperCol = mix(vec3(1.0), vec3(0.86), safe);
    vec3 inkCol = mix(vec3(0.0), vec3(0.16), safe);
    paperCol = mix(paperCol, mix(vec3(1.0), Tint, 0.6), TintAmount);
    inkCol = mix(inkCol, Tint * 0.14, TintAmount);
    vec3 art = mix(inkCol, paperCol, paper);

    // Ink outlines: colour edges everywhere, plus silhouette edges from the depth buffer.
    // Around the focus the kernel widens, which puts a thick outline on the target itself.
    float edge = colorEdge(texCoord, texel);
    if (focus > 0.01) {
        edge = max(edge, colorEdge(texCoord, texel * 2.0) * focus);
    }
    if (UseDepth > 0.5) {
        float silhouette = depthEdge(texCoord, texel);
        if (focus > 0.01) {
            silhouette = max(silhouette, depthEdge(texCoord, texel * 2.4) * focus);
        }
        edge = max(edge, silhouette);
    }
    art = mix(art, inkCol, edge * (1.0 - 0.3 * safe));

    if (Halftone > 0.0 && Tier > 0.5) {
        float cell = max(OutSize.y / 110.0, 4.0);
        vec2 g = mat2(0.7071, -0.7071, 0.7071, 0.7071) * (gl_FragCoord.xy / cell);
        float tone = clamp(1.0 - level, 0.0, 1.0);
        float radius = sqrt(tone) * 0.62;
        float dots = 1.0 - smoothstep(radius - 0.08, radius + 0.08, length(fract(g) - 0.5));
        art = mix(art, inkCol, dots * paper * Halftone * (1.0 - edge) * (1.0 - 0.6 * focus));
    }

    // Light impacts keep the real scene visible under thinner ink. Everything else is full ink art.
    float artAmount = Tier < 0.5 ? 0.7 : 1.0;
    vec3 col = mix(scene, art, artAmount);

    // Speed lines and shockwave rings invert what is under them, so they read as white on black
    // and black on white. Light impacts have neither.
    float flip = 0.0;
    if (Tier > 0.5) {
        flip = speedLines(texCoord, aspect, 1.0 - safe);
    }
    if (Tier > 1.5) {
        flip = max(flip, shockRing(texCoord, aspect));
    }
    flip *= mix(1.0, 0.35, safe) * amount;
    col = mix(col, 1.0 - col, clamp(flip, 0.0, 1.0));

    // Colour negative on the frames the timeline marks. Safe mode never inverts.
    if (Invert > 0.5 && safe < 0.5) {
        col = 1.0 - col;
    }

    vec3 result = mix(scene, col, amount);
    float edgeY = min(texCoord.y, 1.0 - texCoord.y);
    float barMask = 1.0 - smoothstep(Letterbox - 1.0 / OutSize.y, Letterbox, edgeY);
    fragColor = vec4(mix(result, vec3(0.0), barMask), 1.0);
}
