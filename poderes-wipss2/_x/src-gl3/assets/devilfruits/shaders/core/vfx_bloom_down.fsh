#version 150

// 13-tap downsample (Jimenez, "Next Generation Post Processing in Call of Duty: Advanced Warfare", 2014),
// with a soft-knee threshold on the first pass.
uniform sampler2D Source;
uniform vec2 SrcTexel;
uniform float Threshold;
uniform float FirstPass;

in vec2 texCoord;

out vec4 fragColor;

vec3 s(float x, float y) {
    return texture(Source, texCoord + vec2(x, y) * SrcTexel).rgb;
}

void main() {
    vec3 a = s(-2.0, 2.0), b = s(0.0, 2.0), c = s(2.0, 2.0);
    vec3 d = s(-2.0, 0.0), e = s(0.0, 0.0), f = s(2.0, 0.0);
    vec3 g = s(-2.0, -2.0), h = s(0.0, -2.0), i = s(2.0, -2.0);
    vec3 j = s(-1.0, 1.0), k = s(1.0, 1.0), l = s(-1.0, -1.0), m = s(1.0, -1.0);
    vec3 col = e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
    if (FirstPass > 0.5) {
        float br = max(col.r, max(col.g, col.b));
        float knee = Threshold * 0.5 + 1.0e-4;
        float soft = clamp(br - Threshold + knee, 0.0, 2.0 * knee);
        soft = soft * soft / (4.0 * knee);
        col *= max(soft, br - Threshold) / max(br, 1.0e-4);
    }
    fragColor = vec4(min(col, vec3(64.0)), 1.0);
}
