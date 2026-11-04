#version 150

uniform sampler2D AtlasSampler;
uniform sampler2D PackSampler;
uniform sampler2D LutSampler;
uniform sampler2D DepthSampler;

uniform mat4 ProjMat;
uniform vec2 TargetSize;
uniform float Pass;          // 0 scene (premultiplied), 1 emissive buffer for bloom
uniform float SoftScale;     // 0 disables the depth fade
uniform float MinAlpha;      // Fabulous: keeps additive pixels from being dropped by the transparency pass
uniform float EmissiveScale;

in vec4 vColor;
in vec2 vUvA;
in vec2 vUvB;
in float vBlend;
in float vSoft;
in float vAdd;
in float vHeat;
in float vEmissive;
in float vPalette;
in float vPack;

out vec4 fragColor;

float linearDepth(float d) {
    return ProjMat[3][2] / ((d * 2.0 - 1.0) + ProjMat[2][2]);
}

void main() {
    vec4 t;
    if (vPack > 0.5) {
        t = mix(texture(PackSampler, vUvA), texture(PackSampler, vUvB), vBlend);
    } else {
        t = mix(texture(AtlasSampler, vUvA), texture(AtlasSampler, vUvB), vBlend);
    }
    float cover = t.a * vColor.a;
    vec3 rgb;
    float occl;
    if (vHeat >= 0.0) {
        float heat = clamp(t.r * vHeat, 0.0, 1.0);
        vec4 lut = texture(LutSampler, vec2(heat * (255.0 / 256.0) + 0.5 / 256.0, (vPalette + 0.5) / 8.0));
        rgb = lut.rgb * vColor.rgb * cover;
        occl = lut.a;
    } else {
        rgb = t.rgb * vColor.rgb * vColor.a;
        occl = 1.0;
    }
    occl *= 1.0 - vAdd;
    if (cover <= 0.002) {
        discard;
    }
    float self = linearDepth(gl_FragCoord.z);
    float scene = linearDepth(texture(DepthSampler, gl_FragCoord.xy / TargetSize).r);
    if (Pass > 0.5 && self > scene + 0.05) {
        discard;
    }
    if (SoftScale > 0.0 && vSoft > 0.0) {
        float fade = clamp((scene - self) / (vSoft * SoftScale), 0.0, 1.0);
        rgb *= fade;
        cover *= fade;
    }
    if (Pass > 0.5) {
        fragColor = vec4(rgb * vEmissive * EmissiveScale, cover * occl);
        return;
    }
    float alpha = max(cover * occl, min(1.0, max(rgb.r, max(rgb.g, rgb.b))) * MinAlpha);
    fragColor = vec4(rgb, alpha);
}
