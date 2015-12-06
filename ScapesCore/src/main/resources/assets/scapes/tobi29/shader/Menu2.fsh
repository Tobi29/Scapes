#version 330

const float blurWeight[$BLUR_LENGTH] = float[]($BLUR_WEIGHT);

uniform sampler2D uniform_Texture;

in vec2 varying_TextureOffset[$BLUR_LENGTH];

layout(location = 0) out vec4 out_Color;

void main(void)  {
    vec3 color = vec3(0.0);
    #repeat $BLUR_LENGTH color += texture(uniform_Texture, varying_TextureOffset[$i]).rgb * blurWeight[$i];
    out_Color.rgb = color;
    out_Color.a = 1.0;
}
