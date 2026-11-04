#version 150

// A Logia body (devilfruits, cinematic tiers): the vanilla translucent entity look with the body opened up where the
// element shows - round holes along the lines blows went through (capsules from entry to exit, radius by the size of
// what passed), limbs turning into the element from their ends back, with a frontier of the element's grains. Shapes
// follow the model, not the skin's pixels; the grain noise is fixed to the skin so it never swims.
#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D LimbSampler;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform vec4 H0A;
uniform vec4 H1A;
uniform vec4 H2A;
uniform vec4 H3A;
uniform vec4 H4A;
uniform vec4 H5A;
uniform vec4 H6A;
uniform vec4 H7A;
uniform vec4 H0B;
uniform vec4 H1B;
uniform vec4 H2B;
uniform vec4 H3B;
uniform vec4 H4B;
uniform vec4 H5B;
uniform vec4 H6B;
uniform vec4 H7B;
uniform float HoleCount;
uniform vec4 L0R;
uniform vec4 L1R;
uniform vec4 L2R;
uniform vec4 L3R;
uniform vec4 L4R;
uniform vec4 L5R;
uniform vec4 L0T;
uniform vec4 L1T;
uniform vec4 L2T;
uniform vec4 L3T;
uniform vec4 L4T;
uniform vec4 L5T;
uniform vec3 EdgeColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 bodyPos;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

// fine grain, about three cells to a skin texel, on the skin's own coordinates
float grain(vec2 uv) {
    vec2 p = uv * 192.0;
    return vnoise(p) * 0.6 + vnoise(p * 2.7 + 17.0) * 0.4;
}

float capsule(vec3 p, vec3 a, vec3 b) {
    vec3 ab = b - a;
    float t = clamp(dot(p - a, ab) / max(dot(ab, ab), 1.0e-6), 0.0, 1.0);
    return length(p - (a + ab * t));
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    vec4 color = tex * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    float n = grain(texCoord0);
    // > 0: this point of the body is element (gone); the frontier just below 0
    float cut = -1.0;

    vec4 ha[8] = vec4[](H0A, H1A, H2A, H3A, H4A, H5A, H6A, H7A);
    vec4 hb[8] = vec4[](H0B, H1B, H2B, H3B, H4B, H5B, H6B, H7B);
    int count = int(HoleCount + 0.5);
    for (int i = 0; i < 8; i++) {
        if (i >= count) {
            break;
        }
        float r = ha[i].w;
        if (r <= 0.0) {
            continue;
        }
        float d = capsule(bodyPos, ha[i].xyz, hb[i].xyz);
        cut = max(cut, (r - d) / max(r, 0.06) + (n - 0.5) * 0.5);
    }

    int limb = int(texture(LimbSampler, texCoord0).r * 255.0 + 0.5) - 1;
    if (limb >= 0) {
        vec4 lr[6] = vec4[](L0R, L1R, L2R, L3R, L4R, L5R);
        vec4 lt[6] = vec4[](L0T, L1T, L2T, L3T, L4T, L5T);
        float level = lr[limb].w;
        if (level >= 0.999) {
            discard;
        }
        if (level > 0.0) {
            vec3 axis = lt[limb].xyz - lr[limb].xyz;
            float along = clamp(dot(bodyPos - lr[limb].xyz, axis) / max(dot(axis, axis), 1.0e-6), 0.0, 1.0);
            // it turns into the element from its end back towards where it joins the body
            float front = 1.0 - level * 1.15;
            cut = max(cut, (along - front) * 3.5 + (n - 0.5) * 0.9);
        }
    }
    if (cut > 0.0) {
        discard;
    }

    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    // seen through an opening, the inside of the body is the element itself (not the skin's inner faces)
    if (!gl_FrontFacing && cut > -0.85) {
        color.rgb = clamp(EdgeColor * (0.75 + 0.7 * n), 0.0, 1.0);
    }
    // the frontier: grains of the element where the body opens, light and dark
    float rim = smoothstep(-0.3, 0.0, cut);
    if (rim > 0.0) {
        vec3 grains = clamp(EdgeColor * (0.85 + 0.95 * n), 0.0, 1.0);
        color.rgb = mix(color.rgb, grains, rim * 0.92);
    }
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
