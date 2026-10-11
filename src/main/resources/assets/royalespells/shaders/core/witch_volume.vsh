#version 150
// SPDX-License-Identifier: MIT
in vec3 Position;
uniform mat4 Projection;
uniform mat4 VolumeToView;
out vec3 localPosition;
void main() {
    localPosition = Position;
    gl_Position = Projection * VolumeToView * vec4(Position, 1.0);
}
