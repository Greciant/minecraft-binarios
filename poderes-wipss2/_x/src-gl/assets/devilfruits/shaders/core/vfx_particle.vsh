#version 150

// Cinematic particles (see VfxFormat): flipbook frames A/B, heat or colour sprite, soft depth fade, main atlas or explosion pack.
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vColor;
out vec2 vUvA;
out vec2 vUvB;
out float vBlend;
out float vSoft;
out float vAdd;
out float vHeat;
out float vEmissive;
out float vPalette;
out float vPack;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vUvA = UV0;
    vUvB = vec2(UV1) / 32767.0;
    vBlend = Normal.x;
    vSoft = Normal.y * 4.0;
    vAdd = Normal.z;
    vHeat = float(UV2.x) / 4096.0;
    vEmissive = float(UV2.y & 4095) / 256.0;
    vPalette = float((UV2.y >> 12) & 7);
    vPack = float((UV2.y >> 15) & 1);
}
