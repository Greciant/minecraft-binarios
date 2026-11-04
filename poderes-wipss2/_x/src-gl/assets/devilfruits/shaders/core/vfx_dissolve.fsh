#version 150

// Each texel of the skin appears at its own random moment as the threshold rises, glowing white-hot to ember for a
// moment at the burning edge (pixel-art friendly: the noise is snapped to the texture's texels).
#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in float progress;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    vec2 cell = floor(texCoord0 * vec2(textureSize(Sampler0, 0)));
    float n = fract(sin(dot(cell, vec2(12.9898, 78.233))) * 43758.5453);
    if (n > progress) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    float edge = (1.0 - smoothstep(0.0, 0.16, progress - n)) * step(progress, 0.995);
    vec3 ember = mix(vec3(0.91, 0.20, 0.12), vec3(1.0, 0.96, 0.76), edge * edge);
    color.rgb = mix(color.rgb, ember, edge);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
