#version 330

#define ENABLE_BLOOM _SCAPES_ENGINE_DEFINE

const vec3 lumcoeff = vec3(0.299, 0.587, 0.114);

in vec2 varying_Texture;
in float varying_AutoExposure;

uniform sampler2D uniform_Texture;
uniform sampler2D uniform_Luminance;
uniform float uniform_Brightness;
uniform float uniform_Exposure;

#ifdef ENABLE_BLOOM
    #define BLUR_WEIGHT _SCAPES_ENGINE_EXTERNAL
    #define BLUR_LENGTH _SCAPES_ENGINE_EXTERNAL

    const float blurWeight[BLUR_LENGTH] = float[BLUR_LENGTH](BLUR_WEIGHT);

    in vec2 varying_TextureOffset[BLUR_LENGTH];
#endif

layout(location = 0) out vec4 out_Color;

vec3 toneMap(vec3 color, float exposure) {
    color *= vec3(exposure);
    color = max(vec3(0.0), color - 0.004);
    color = (color * (6.2 * color + 0.5)) / (color * (6.2 * color + 1.7) + 0.06);
    return color * color;
}

vec3 colorBoost(vec3 color, float amount, float luminance) {
    float thresh = max((luminance - 0.5) * 80.0, 0.0);
    return color + color * vec3(thresh * amount);
}

void main(void) {
    vec3 color = texture(uniform_Texture, varying_Texture).rgb;
    color = toneMap(color, uniform_Exposure + varying_AutoExposure);
    float luminance = dot(color, lumcoeff);
    #ifdef ENABLE_BLOOM
        vec3 bloom = vec3(0.0);
        for (int i = 0; i < BLUR_LENGTH; i++) {
            bloom += texture(uniform_Luminance, varying_TextureOffset[i]).rgb * blurWeight[i];
        }
        bloom = toneMap(bloom, uniform_Exposure);
        float luminanceBloom = dot(bloom, lumcoeff);
        if (luminanceBloom > luminance) {
            color += bloom * (luminanceBloom - luminance);
        }
    #endif
    color = colorBoost(color, 0.02, luminance);
    color *= uniform_Brightness;
    out_Color.rgb = color;
    out_Color.a = 1.0;
}
