#version 150

// The Soul King made of soul fire (devilfruits, Yomi Yomi): the vanilla entity vertex stage, passing on where each
// fragment is - camera-relative, so the fragment stage can work in the ghost's own frame (Origin, Yaw) - and its normal.
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec2 texCoord0;
out vec3 worldPos;
out vec3 worldNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(Position, FogShape);
    texCoord0 = UV0;
    worldPos = Position;
    worldNormal = Normal;
}
