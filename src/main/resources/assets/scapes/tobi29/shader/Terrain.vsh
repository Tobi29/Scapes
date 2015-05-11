#version 150

#define ENABLE_ANIMATIONS _SCAPES_ENGINE_DEFINE

const float PI = 3.14159;
const float DEG2RAD = PI / 180.0;
const vec3 BLOCKLIGHT_NORMAL = vec3(0.3, 0.1, 0.6);
const int ANIMATION_NONE = 0;
const int ANIMATION_LEAVES = 1;
const int ANIMATION_WATER = 2;

uniform float uniform_Time;
uniform float uniform_Sunlight;
uniform vec3 uniform_SunlightNormal;
uniform float uniform_CamLight;
uniform float uniform_AnimationDistance;
uniform mat4 uniform_ModelViewMatrix;
uniform mat4 uniform_ModelViewProjectionMatrix;
uniform mat3 uniform_NormalMatrix;

in vec4 attribute_Position;
in vec4 attribute_Color;
in vec2 attribute_Texture;
in vec3 attribute_Normal;
in vec2 attribute_Light;
in float attribute_Animation;

out vec4 varying_Color;
out vec2 varying_Texture;
out float varying_Depth;

float sqr(float x)  {
    return x * x;
}

void main(void)  {
    varying_Texture = attribute_Texture;
    varying_Color = attribute_Color;
    vec4 vertex = attribute_Position;
    varying_Depth = length((uniform_ModelViewMatrix * vertex).xyz);
    #ifdef ENABLE_ANIMATIONS
    if (attribute_Animation != 0) {
        if (varying_Depth < uniform_AnimationDistance) {
            vec3 animPos;
            animPos.x = vertex.x * DEG2RAD;
            animPos.y = vertex.y * DEG2RAD;
            animPos.z = vertex.z * DEG2RAD;
            if (attribute_Animation == ANIMATION_WATER) {
                // Water
                varying_Color.a = mix(1.0, varying_Color.a, sqr(1.0 - min(varying_Depth / 32.0, 1.0)));
                vertex.z -= 0.06;
                vertex.z += sin((uniform_Time * 0.4 + (animPos.x + animPos.y) * 11.25) * 6.0) * 0.04;
                vertex.z += sin((uniform_Time * 0.8 + (animPos.x - animPos.y) * 22.5) * 6.0) * 0.02;
            } else if (attribute_Animation == ANIMATION_LEAVES) {
                // Leaves
                vertex.x += sin((uniform_Time * 0.4 + (animPos.x + animPos.y) * 11.25) * 6.0) * 0.04;
                vertex.y += sin((uniform_Time * 0.3 + (animPos.x - animPos.y) * 11.25) * 6.0) * 0.04;
                vertex.z += sin((uniform_Time * 0.9 + (animPos.x - animPos.y) * 22.5) * 6.0) * 0.04;
            }
        } else if (attribute_Animation == ANIMATION_WATER) {
            // Water Transparency
            varying_Color.a = mix(1.0, varying_Color.a, sqr(1.0 - min(varying_Depth / 32.0, 1.0)));
        }
    }
    #else
    if (attribute_Animation == ANIMATION_WATER) {
        // Water Transparency
        varying_Color.a = mix(1.0, varying_Color.a, sqr(1.0 - min(varying_Depth / 32.0, 1.0)));
    }
    #endif
    float lightSunLevel = max(attribute_Light.y - uniform_Sunlight, 0.0);
    vec3 lightSun = vec3(lightSunLevel);
    vec3 lightBlock = vec3(attribute_Light.x);
    lightBlock *= 0.5;
    lightBlock.gb *= lightBlock.gb;
    lightBlock.b *= lightBlock.b;
    if (varying_Depth < 32.0 * uniform_CamLight) {
        vec3 realCamLight = vec3(clamp((1.0 - varying_Depth * 0.04) * uniform_CamLight, 0.0, 1.0));
        realCamLight.gb *= realCamLight.gb;
        realCamLight.b *= realCamLight.b;
        varying_Color.rgb *= max(max(lightSun, lightBlock), realCamLight);
    } else {
        varying_Color.rgb *= max(lightSun, lightBlock);
    }
    vec3 normal = uniform_NormalMatrix * attribute_Normal;
    float light = (mix(dot(normal, BLOCKLIGHT_NORMAL), dot(normal, uniform_SunlightNormal), lightSunLevel) + 1.2) / 2.2;
    varying_Color.rgb *= light;
    gl_Position = uniform_ModelViewProjectionMatrix * vertex;
}
