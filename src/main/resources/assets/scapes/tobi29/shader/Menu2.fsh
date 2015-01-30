#version 150

#define BLUR_WEIGHT _SCAPES_ENGINE_EXTERNAL
#define BLUR_LENGTH _SCAPES_ENGINE_EXTERNAL

const float blurWeight[BLUR_LENGTH] = float[BLUR_LENGTH](BLUR_WEIGHT);

uniform sampler2D uniform_Texture;

in vec2 varying_TextureOffset[BLUR_LENGTH];

out vec4 out_Color;

void main(void)  {
    vec3 color = vec3(0.0);
    for (int i = 0; i < BLUR_LENGTH; i++) {
        color += texture(uniform_Texture, varying_TextureOffset[i]).rgb * blurWeight[i];
    }
    out_Color.rgb = color;
    out_Color.a = 1.0;
}
