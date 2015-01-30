#version 150

#define BLUR_OFFSET _SCAPES_ENGINE_EXTERNAL
#define BLUR_LENGTH _SCAPES_ENGINE_EXTERNAL

const float blurOffset[BLUR_LENGTH] = float[BLUR_LENGTH](BLUR_OFFSET);

in vec4 attribute_Position;
in vec2 attribute_Texture;

uniform mat4 uniform_ModelViewProjectionMatrix;

out vec2 varying_TextureOffset[BLUR_LENGTH];

void main(void)  {
    for (int i = 0; i < BLUR_LENGTH; i++) {
        varying_TextureOffset[i] = vec2(attribute_Texture.x, attribute_Texture.y + blurOffset[i]);
    }
    gl_Position = uniform_ModelViewProjectionMatrix * attribute_Position;
}
