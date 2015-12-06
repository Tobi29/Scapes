#version 330

in vec4 varying_Color;
in vec2 varying_Texture;

layout(location = 0) out vec4 out_Color;

void main(void) {
    vec2 relative = varying_Texture - vec2(0.5);
    relative *= relative;
    float a = max(1.0 - (relative.x + relative.y) * 4.0, 0.0);
    if (a <= 0.01) {
        discard;
    }
    a *= a;
    a *= a;
    out_Color.rgb = varying_Color.rgb;
    out_Color.a = a * a * varying_Color.a;
}