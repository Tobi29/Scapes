#version 150

in vec4 attribute_Position;
in vec2 attribute_Texture;

uniform mat4 uniform_ModelViewProjectionMatrix;

out vec2 varying_Texture;

void main(void)  {
    varying_Texture = attribute_Texture;
    gl_Position = uniform_ModelViewProjectionMatrix * attribute_Position;
}
