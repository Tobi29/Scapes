#version 150

uniform sampler2D uniform_Texture;
uniform vec3 uniform_FogColor;
uniform float uniform_FogEnd;

in vec4 varying_Color;
in vec2 varying_Texture;
in float varying_Depth;

out vec4 out_Color;

void main(void) {
    vec4 color = texture(uniform_Texture, varying_Texture);
    float fog = min(varying_Depth / uniform_FogEnd, 1.0);
    out_Color.rgb = mix(color.rgb * varying_Color.rgb, uniform_FogColor, fog);
    out_Color.a = color.a * varying_Color.a;
}