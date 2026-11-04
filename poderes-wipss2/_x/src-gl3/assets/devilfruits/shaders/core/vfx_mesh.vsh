#version 150

// Cinematic meshes and ribbons (see VfxFormat): scrolling texture, LUT colour, dissolve, fresnel rim.
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vColor;
out vec2 vUv;
out float vDissolve;
out float vFresnel;
out float vHeat;
out float vEmissive;
out float vPalette;
out vec3 vPos;
out vec3 vNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vUv = UV0;
    vDissolve = float(UV1.x) / 32767.0;
    vFresnel = float(UV1.y) / 32767.0;
    vHeat = float(UV2.x) / 4096.0;
    vEmissive = float(UV2.y & 4095) / 256.0;
    vPalette = float((UV2.y >> 12) & 7);
    vPos = Position;
    vNormal = Normal;
}
