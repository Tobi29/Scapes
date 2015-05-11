#version 150

#define SAMPLE_OFFSET _SCAPES_ENGINE_EXTERNAL
#define SAMPLE_WEIGHT _SCAPES_ENGINE_EXTERNAL
#define SAMPLE_LENGTH _SCAPES_ENGINE_EXTERNAL

const float sampleOffset[SAMPLE_LENGTH] = float[SAMPLE_LENGTH](SAMPLE_OFFSET);
const float sampleWeight[SAMPLE_LENGTH] = float[SAMPLE_LENGTH](SAMPLE_WEIGHT);

const vec3 lumcoeff = vec3(0.299, 0.587, 0.114);

uniform sampler2D uniform_Texture;
uniform float uniform_Alpha;

out vec4 out_Color;

void main(void)  {
    vec3 color = vec3(0.0);
    for (int i = 0; i < SAMPLE_LENGTH; i++) {
        float currentOffset = sampleOffset[i];
        float currentWeight = sampleWeight[i];
        for (int j = 0; j < SAMPLE_LENGTH; j++) {
            color += texture(uniform_Texture, vec2(sampleOffset[j], currentOffset)).rgb * sampleWeight[j] * currentWeight;
        }
    }
    float luminance = dot(color, lumcoeff);
    out_Color.r = mix(1.0, -1.3, luminance);
    out_Color.a = uniform_Alpha;
}