#version 330

uniform sampler2D uniform_Texture;
uniform sampler2D uniform_Noise;
uniform vec2 uniform_ScreenSize;

in vec2 varying_Texture;
in vec2 varying_PosPos;

layout(location = 0) out vec4 out_Color;

#define FxaaInt2 vec2
#define FxaaFloat2 vec2
#define FxaaTexLod0(t, p) texture(t, p)

vec3 fxaa(vec4 posPos, sampler2D tex, vec2 rcpFrame) {
    #define FXAA_REDUCE_MIN   (1.0/128.0)
    #define FXAA_REDUCE_MUL   (1.0/8.0)
    #define FXAA_SPAN_MAX     8.0

    vec3 rgbNW = FxaaTexLod0(tex, posPos.zw).xyz;
    vec3 rgbNE = FxaaTexLod0(tex, posPos.zw + FxaaInt2(1.0,0.0) * rcpFrame.xy).xyz;
    vec3 rgbSW = FxaaTexLod0(tex, posPos.zw + FxaaInt2(0.0,1.0) * rcpFrame.xy).xyz;
    vec3 rgbSE = FxaaTexLod0(tex, posPos.zw + FxaaInt2(1.0,1.0) * rcpFrame.xy).xyz;
    vec3 rgbM  = FxaaTexLod0(tex, posPos.xy).xyz;

    vec3 luma = vec3(0.299, 0.587, 0.114);
    float lumaNW = dot(rgbNW, luma);
    float lumaNE = dot(rgbNE, luma);
    float lumaSW = dot(rgbSW, luma);
    float lumaSE = dot(rgbSE, luma);
    float lumaM  = dot(rgbM,  luma);

    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    vec2 dir; 
    dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
    dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));

    float dirReduce = max(
        (lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL),
        FXAA_REDUCE_MIN);
    float rcpDirMin = 1.0/(min(abs(dir.x), abs(dir.y)) + dirReduce);
    dir = min(FxaaFloat2( FXAA_SPAN_MAX,  FXAA_SPAN_MAX), 
          max(FxaaFloat2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX), 
          dir * rcpDirMin)) * rcpFrame.xy;

    vec3 rgbA = (1.0/2.0) * (
        FxaaTexLod0(tex, posPos.xy + dir * (1.0/3.0 - 0.5)).xyz +
        FxaaTexLod0(tex, posPos.xy + dir * (2.0/3.0 - 0.5)).xyz);
    vec3 rgbB = rgbA * (1.0/2.0) + (1.0/4.0) * (
        FxaaTexLod0(tex, posPos.xy + dir * (0.0/3.0 - 0.5)).xyz +
        FxaaTexLod0(tex, posPos.xy + dir * (3.0/3.0 - 0.5)).xyz);
    float lumaB = dot(rgbB, luma);
    if((lumaB < lumaMin) || (lumaB > lumaMax)) return rgbA;
    return rgbB;
}

vec3 dither(vec3 color, vec2 pixel) {
    vec3 noise = texture(uniform_Noise, pixel).rgb / 512.0 + 0.001953125;
    return color + noise;
}
    
void main() {
  vec2 uv = varying_Texture;
  vec4 color = vec4(0.0);
  vec2 rcpFrame = vec2(1.0 / uniform_ScreenSize.x, 1.0 / uniform_ScreenSize.y);
  color.rgb = fxaa(vec4(varying_Texture, varying_PosPos), uniform_Texture, rcpFrame);
  color.rgb = dither(color.rgb, varying_Texture * uniform_ScreenSize / vec2(512.0));
  color.a = 1.0;
  out_Color = color;
}