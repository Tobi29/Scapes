#version 330

in vec4 attribute_Position;

uniform mat4 uniform_ModelViewProjectionMatrix;

void main(void)  {
    gl_Position = uniform_ModelViewProjectionMatrix * attribute_Position;
}
