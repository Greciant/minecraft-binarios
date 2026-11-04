#version 150

// 9-tap tent upsample, added onto the next larger mip.
uniform sampler2D Source;
uniform vec2 SrcTexel;
uniform float Radius;
uniform float Intensity;

in vec2 texCoord;

out vec4 fragColor;

vec3 s(float x, float y) {
    return texture(Source, texCoord + vec2(x, y) * SrcTexel * Radius).rgb;
}

void main() {
    vec3 col = s(-1.0, 1.0) + 2.0 * s(0.0, 1.0) + s(1.0, 1.0)
             + 2.0 * s(-1.0, 0.0) + 4.0 * s(0.0, 0.0) + 2.0 * s(1.0, 0.0)
             + s(-1.0, -1.0) + 2.0 * s(0.0, -1.0) + s(1.0, -1.0);
    fragColor = vec4(col / 16.0 * Intensity, 1.0);
}
