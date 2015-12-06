#version 330

const vec3 BLOCKLIGHT_NORMAL = vec3(0.3, 0.1, 0.6);

uniform float uniform_Time;
uniform float uniform_Sunlight;
uniform vec3 uniform_SunlightNormal;
uniform float uniform_CamLight;
uniform mat4 uniform_ModelViewMatrix;
uniform mat4 uniform_ModelViewProjectionMatrix;
uniform mat3 uniform_NormalMatrix;

in vec4 attribute_Position;
in vec4 attribute_Color;
in vec2 attribute_Texture;
in vec3 attribute_Normal;
in vec2 attribute_Light;

out float varying_Depth;
out vec4 varying_Color;
out vec2 varying_Texture;

float sqr(float x)  {
    return x * x;
}

void main(void)  {
    varying_Texture = attribute_Texture;
    varying_Color = attribute_Color;
    vec4 vertex = attribute_Position;
    varying_Depth = length(vec3(uniform_ModelViewMatrix * vertex));
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
