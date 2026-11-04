#version 150

// Final cinematic composite: distortion, chromatic aberration, zoom blur, bloom (screen blend, never past white),
// warm grade, desaturation, flash and vignette.
uniform sampler2D SceneSampler;
uniform sampler2D DistortSampler;
uniform sampler2D EmissiveSampler;
uniform sampler2D BloomSampler;

uniform vec2 RadialCenter;
uniform float RadialStrength;
uniform float Chromatic;
uniform float BloomIntensity;
uniform float DistortOn;
uniform vec4 Flash;
uniform vec4 Grade;
uniform float Vignette;
uniform float Desaturate;
uniform vec2 InvSize;

in vec2 texCoord;

out vec4 fragColor;

vec3 sceneAt(vec2 uv, vec2 ca) {
    return vec3(texture(SceneSampler, uv + ca).r, texture(SceneSampler, uv).g, texture(SceneSampler, uv - ca).b);
}

void main() {
    vec2 uv = texCoord;
    vec2 off = DistortOn > 0.5 ? texture(DistortSampler, uv).rg : vec2(0.0);
    vec2 suv = clamp(uv + off, InvSize, 1.0 - InvSize);
    vec2 dir = suv - RadialCenter;
    vec2 ca = dir * Chromatic;
    vec3 col;
    if (RadialStrength > 0.0005) {
        col = vec3(0.0);
        for (int i = 0; i < 10; i++) {
            float k = 1.0 - RadialStrength * float(i) / 9.0;
            col += sceneAt(RadialCenter + dir * k, ca);
        }
        col /= 10.0;
    } else {
        col = sceneAt(suv, ca);
    }
    if (BloomIntensity > 0.0) {
        vec3 glow = (texture(EmissiveSampler, suv).rgb * 0.45 + texture(BloomSampler, suv).rgb) * BloomIntensity;
        col = col + (1.0 - col) * (1.0 - exp(-glow));
    }
    if (Grade.a > 0.0) {
        vec3 tinted = clamp(col * Grade.rgb, 0.0, 1.0);
        tinted = mix(tinted, tinted * tinted * (3.0 - 2.0 * tinted), 0.3);
        col = mix(col, tinted, Grade.a);
    }
    if (Desaturate > 0.0) {
        col = mix(col, vec3(dot(col, vec3(0.299, 0.587, 0.114))), Desaturate);
    }
    if (Flash.a > 0.0) {
        float edge = 0.55 + 0.45 * smoothstep(0.15, 0.75, length(uv - 0.5));
        col = mix(col, Flash.rgb, Flash.a * edge);
    }
    if (Vignette > 0.0) {
        float v = smoothstep(0.9, 0.3, length((uv - 0.5) * vec2(1.0, 0.8)));
        col *= mix(1.0, v, Vignette);
    }
    fragColor = vec4(col, 1.0);
}
