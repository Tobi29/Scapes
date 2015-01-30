#version 150

uniform sampler2D uniform_Texture;

in vec2 varying_Texture;

out vec4 out_Color;

void main(void)  {
    out_Color.rgb = texture(uniform_Texture, varying_Texture).rgb;
    out_Color.a = 1.0;
}