#version 150

uniform sampler2D uniform_Texture;
uniform sampler2D uniform_BackgroundTexture;
uniform vec2 uniform_ScreenSize;
uniform vec3 uniform_FogColor;
uniform float uniform_FogEnd;

in vec4 varying_Color;
in vec2 varying_Texture;
in float varying_Depth;

out vec4 out_Color;

void main(void) {
    out_Color = texture(uniform_Texture, varying_Texture);
    out_Color.a *= varying_Color.a;
    if (out_Color.a <= 0.01) {
        discard;
    }
    float fog = min(varying_Depth / uniform_FogEnd, 1.0);
    fog *= fog;
    out_Color.rgb = mix(out_Color.rgb * varying_Color.rgb, uniform_FogColor, fog);
    float fogBackground = mix(10.0, 0.0, fog);
    if (fogBackground < 1.0) {
        vec3 background = texture(uniform_BackgroundTexture, gl_FragCoord.xy / uniform_ScreenSize).rgb;
        out_Color.rgb = mix(background, out_Color.rgb, fogBackground);
    }
}
