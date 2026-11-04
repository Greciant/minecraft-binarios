#version 150

// Writes screen-space offsets (RG) into the half-resolution distortion buffer (additive).
uniform sampler2D AtlasSampler;
uniform sampler2D HeatSampler;
uniform sampler2D DepthSampler;

uniform mat4 ProjMat;
uniform vec2 TargetSize;
uniform float Time;

in vec4 vColor;
in vec2 vUv;
in vec2 vNoise;
in float vMode;

out vec4 fragColor;

float linearDepth(float d) {
    return ProjMat[3][2] / ((d * 2.0 - 1.0) + ProjMat[2][2]);
}

void main() {
    vec4 t = texture(AtlasSampler, vUv);
    vec2 off;
    if (vMode < 0.5) {
        off = (t.rg * 2.0 - 1.0) * t.a;
    } else if (vMode < 1.5) {
        vec2 n = texture(HeatSampler, vNoise * 4.0 + vec2(0.0, -Time * 0.035)).rg * 2.0 - 1.0;
        off = n * t.a;
    } else {
        // 2: glass (the offset rides on the vertex colour: a shard bends what is behind it its own way),
        // 3: the same, soft across the band (v 0..1), for rings of bent air
        float mask = vMode < 2.5 ? 1.0 : sin(clamp(vUv.y, 0.0, 1.0) * 3.14159);
        off = (vColor.rg * 2.0 - 1.0) * mask;
    }
    float self = linearDepth(gl_FragCoord.z);
    float scene = linearDepth(texture(DepthSampler, gl_FragCoord.xy / TargetSize).r);
    if (self > scene + 0.1) {
        discard;
    }
    fragColor = vec4(off * vColor.a * 0.04, 0.0, 1.0);
}
