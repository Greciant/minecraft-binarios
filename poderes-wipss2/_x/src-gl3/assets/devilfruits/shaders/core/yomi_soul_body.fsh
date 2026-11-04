#version 150

// The Soul King made of soul fire (user, M6 review): one body of flame rising through the whole ghost - a 3D noise in the
// ghost's own frame, so it runs on across the cubes' faces and seams instead of being painted onto each cube - coloured
// through the soul-fire ramp, a little hotter than the flames round it. The Blockbench texture still shapes it: its
// light and shade make the fire hotter or cooler (the ribs, the crown, the brows read), and its darkest texels - the
// eye sockets, the nose, the inside of the maw - stay black. Hotter towards the silhouette, and the edges burn ragged.
#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec3 Origin;
uniform float Yaw;
uniform float Time;
uniform float Burn;

in float vertexDistance;
in vec2 texCoord0;
in vec3 worldPos;
in vec3 worldNormal;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i + vec3(0, 0, 0)), hash(i + vec3(1, 0, 0)), f.x),
                   mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x),
                   mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return v;
}

// the soul-fire ramp (LUT row SOUL_FIRE), a touch brighter: teal soot, deep green, green, neon, lime, lemon white
vec3 ramp(float h) {
    h = clamp(h, 0.0, 1.0);
    vec3 c = mix(vec3(0.02, 0.10, 0.07), vec3(0.05, 0.40, 0.12), smoothstep(0.0, 0.3, h));
    c = mix(c, vec3(0.20, 0.85, 0.20), smoothstep(0.25, 0.5, h));
    c = mix(c, vec3(0.49, 1.00, 0.23), smoothstep(0.45, 0.7, h));
    c = mix(c, vec3(0.80, 1.00, 0.45), smoothstep(0.68, 0.88, h));
    c = mix(c, vec3(0.98, 1.00, 0.86), smoothstep(0.86, 1.0, h));
    return c;
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) {
        discard;
    }
    // the ghost's own frame (blocks): turned with it, so the fire stays on its body as it swings round
    vec3 p = worldPos - Origin;
    float cy = cos(Yaw), sy = sin(Yaw);
    p = vec3(cy * p.x + sy * p.z, p.y, -sy * p.x + cy * p.z);
    float t = Time * 0.05;
    // tall rising tongues: stretched up, carried upwards, the domain stirred so they lick and curl
    vec3 q = vec3(p.x * 2.6, p.y * 1.25 - t * 3.0, p.z * 2.6);
    vec3 warp = vec3(fbm(q + vec3(0.0, t * 0.7, 0.0)), fbm(q + vec3(5.2, 1.3 - t * 0.5, 2.8)), 0.0) - 0.5;
    // the fbm sits round 0.5: spread it out, then sharpen it into tongues - deep valleys, hot licking ridges
    float flame = clamp((fbm(q + warp * 1.8) - 0.3) / 0.4, 0.0, 1.0);
    float tongue = pow(flame, 2.0);
    float fine = noise(q * 3.1 + vec3(0.0, -t * 3.5, 0.0));

    // the painted forms shape the heat; the darkest texels are the sockets, the nose and the maw: they stay dark
    float lum = dot(tex.rgb, vec3(0.3, 0.59, 0.11));
    float dark = 1.0 - smoothstep(0.12, 0.165, lum);

    vec3 n = normalize(worldNormal);
    vec3 v = normalize(-worldPos);
    float rim = 1.0 - abs(dot(n, v));
    rim = rim * rim;

    // a light body (user, M6 review: no dark green on it): the valleys stay a bright green, the painted shade barely cools it
    float heat = 0.46 + tongue * 0.6 + fine * 0.1 + (lum - 0.45) * 0.06 + rim * 0.25;
    heat *= Burn;

    // the silhouette burns ragged: grazing faces flicker away where the flame is thin
    if (rim > 0.55 && flame * 0.6 + fine * 0.3 < (rim - 0.55) * 1.9 + 0.2) {
        discard;
    }

    vec3 col = ramp(heat);
    // a thin ember lip round the dark hollows, then the hollow itself black
    float lip = smoothstep(0.12, 0.15, lum) * (1.0 - smoothstep(0.15, 0.2, lum));
    col += vec3(0.25, 0.6, 0.15) * lip * 0.5;
    col = mix(col, vec3(0.006, 0.02, 0.012), dark);

    vec4 color = vec4(col, 1.0) * ColorModulator;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
