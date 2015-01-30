#version 150

#define BLUR_WEIGHT _SCAPES_ENGINE_EXTERNAL
#define BLUR_LENGTH _SCAPES_ENGINE_EXTERNAL

const float blurWeight[BLUR_LENGTH] = float[BLUR_LENGTH](BLUR_WEIGHT);

uniform sampler2D uniform_Texture;
uniform float uniform_Brightness;

in vec2 varying_Texture;
in vec2 varying_TextureOffset[BLUR_LENGTH];

out vec4 out_Color;
out vec4 out_Luminance;

void main(void) {
    vec3 color = texture(uniform_Texture, varying_Texture).rgb;
    vec3 bloom = vec3(0.0);
    for (int i = 0; i < BLUR_LENGTH; i++) {
        bloom += texture(uniform_Texture, varying_TextureOffset[i]).rgb * blurWeight[i];
    }
    out_Color.rgb = color;
    out_Color.a = 1.0;
    out_Luminance.rgb = bloom;
    out_Luminance.a = 1.0;
}
