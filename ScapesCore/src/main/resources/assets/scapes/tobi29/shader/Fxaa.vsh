#version 330

in vec4 attribute_Position;
in vec2 attribute_Texture;

uniform vec2 uniform_ScreenSize;
uniform mat4 uniform_ModelViewProjectionMatrix;

out vec2 varying_Texture;
out vec2 varying_PosPos;

void main(void)  {
    varying_Texture = attribute_Texture;
    vec2 rcpFrame = vec2(1.0 / uniform_ScreenSize.x, 1.0 / uniform_ScreenSize.y);
    varying_PosPos = attribute_Texture - rcpFrame * 0.75;
    gl_Position = uniform_ModelViewProjectionMatrix * attribute_Position;
}
