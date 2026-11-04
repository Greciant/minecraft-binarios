#version 150

uniform sampler2D MainSampler;
uniform sampler2D NoiseSampler;
uniform sampler2D LutSampler;
uniform sampler2D DepthSampler;

uniform mat4 ProjMat;
uniform vec2 TargetSize;
uniform float Pass;
uniform float SoftScale;
uniform float MinAlpha;
uniform float EmissiveScale;
uniform float AlphaCut;       // solid layers (sand bodies) write depth: their faint fringe must not

in vec4 vColor;
in vec2 vUv;
in float vDissolve;
in float vFresnel;
in float vHeat;
in float vEmissive;
in float vPalette;
in vec3 vPos;
in vec3 vNormal;

out vec4 fragColor;

float linearDepth(float d) {
    return ProjMat[3][2] / ((d * 2.0 - 1.0) + ProjMat[2][2]);
}

void main() {
    vec4 t = texture(MainSampler, vUv);
    float cover;
    vec3 rgb;
    float occl;
    if (vHeat >= 0.0) {
        float heat = clamp(t.r * vHeat, 0.0, 1.0);
        vec4 lut = texture(LutSampler, vec2(heat * (255.0 / 256.0) + 0.5 / 256.0, (vPalette + 0.5) / 8.0));
        cover = t.a * vColor.a * smoothstep(0.0, 0.22, heat);
        rgb = lut.rgb * vColor.rgb * cover;
        occl = lut.a * 0.3;
    } else {
        cover = t.a * vColor.a;
        rgb = t.rgb * vColor.rgb * vColor.a;
        occl = 1.0;
    }
    vec3 burn = vec3(0.0);
    if (vDissolve > 0.0) {
        float n = texture(NoiseSampler, vUv * 0.61 + vec2(0.37, 0.11)).r;
        float k = smoothstep(vDissolve, vDissolve + 0.12, n);
        cover *= k;
        rgb *= k;
        if (vHeat < 0.0) {
            // colour surfaces (cocoons, shells) burn away with a glowing rim in their palette's hot colour
            float rim = k * (1.0 - smoothstep(vDissolve + 0.03, vDissolve + 0.18, n));
            burn = texture(LutSampler, vec2(0.86, (vPalette + 0.5) / 8.0)).rgb * rim * 1.6 * vColor.a;
        }
    }
    if (vFresnel > 0.0) {
        float rim = 1.0 - abs(dot(normalize(vNormal), normalize(-vPos)));
        float f = mix(1.0, rim * rim * 1.8, vFresnel);
        cover *= f;
        rgb *= f;
    }
    rgb += burn;
    if (cover <= AlphaCut && dot(burn, burn) < 0.0001) {
        discard;
    }
    float self = linearDepth(gl_FragCoord.z);
    float scene = linearDepth(texture(DepthSampler, gl_FragCoord.xy / TargetSize).r);
    if (Pass > 0.5 && self > scene + 0.05) {
        discard;
    }
    if (SoftScale > 0.0) {
        float fade = clamp((scene - self) / (0.35 * SoftScale), 0.0, 1.0);
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
