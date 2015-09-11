/*
 * Copyright 2012-2015 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tobi29.scapes.vanilla.basics.generator;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldSkybox;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.openal.StaticAudio;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.client.MobPlayerClient;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.vanilla.basics.entity.client.MobPlayerClientMainVB;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleRaindrop;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleSnowflake;
import org.tobi29.scapes.vanilla.basics.gui.GuiComponentHotbar;
import org.tobi29.scapes.vanilla.basics.gui.GuiHudCondition;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class WorldSkyboxOverworld implements WorldSkybox {
    private static final float[][] STAR_COLORS =
            {new float[]{0.8f, 0.8f, 0.8f}, new float[]{0.8f, 0.5f, 0.5f},
                    new float[]{0.8f, 0.6f, 0.6f},
                    new float[]{0.6f, 0.6f, 0.8f},
                    new float[]{0.5f, 0.5f, 0.8f}};
    private static final float[][] COLORS =
            {{0.0f, 0.3f, 0.8f}, {0.2f, 0.7f, 1.0f}};
    private final FBO fbo;
    private final VAO billboardMesh, cloudTextureMesh, cloudMesh, skyboxMesh,
            skyboxBottomMesh, starMesh;
    private final ClimateGenerator climateGenerator;
    private final BiomeGenerator biomeGenerator;
    private final WorldClient world;
    private GuiWidgetDebugValues.Element temperatureDebug, humidityDebug,
            weatherDebug, biomeDebug, staminaDebug, hungerDebug, thirstDebug,
            bodyTemperatureDebug, exposureDebug;
    private StaticAudio rainAudio;
    private StaticAudio windAudio;
    private double rainGainWait;
    private int rainDrops;
    private double exposure = 0.3;
    private float fogBrightness;
    private float rainGain, fogR, fogG, fogB, fogDistance;

    public WorldSkyboxOverworld(ClimateGenerator climateGenerator,
            BiomeGenerator biomeGenerator, WorldClient world, GL gl) {
        this.climateGenerator = climateGenerator;
        this.biomeGenerator = biomeGenerator;
        this.world = world;
        long seed = world.seed();
        fbo = new FBO(512, 512, 1, false, false, true, gl);
        billboardMesh = VAOUtility.createVTI(
                new float[]{1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, 1.0f, -1.0f, 1.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
                new int[]{0, 1, 2, 0, 2, 3}, RenderType.TRIANGLES);
        cloudTextureMesh = VAOUtility.createVTI(
                new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f},
                new float[]{0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f},
                new int[]{0, 1, 2, 0, 2, 3}, RenderType.TRIANGLES);
        Mesh cloudMesh = new Mesh(true);
        float lastX = 1, lastY = 0, x, y, rad;
        cloudMesh.color(1.0f, 1.0f, 1.0f, 1.0f);
        for (float dir = -22.5f; dir < 360; dir += 22.5f) {
            rad = (float) (dir * FastMath.DEG_2_RAD);
            x = (float) FastMath.cos(rad);
            y = (float) FastMath.sin(rad);
            cloudMesh.texture(0.5f, 0.5f);
            cloudMesh.vertex(0, 0, 0.2f);
            cloudMesh.texture(0.5f + x * 0.5f, 0.5f + y * 0.5f);
            cloudMesh.vertex(x, y, -0.1f);
            cloudMesh.texture(0.5f + lastX * 0.5f, 0.5f + lastY * 0.5f);
            cloudMesh.vertex(lastX, lastY, -0.1f);
            lastX = x;
            lastY = y;
        }
        this.cloudMesh = cloudMesh.finish();
        lastX = 1.0f;
        lastY = 0.0f;
        Mesh skyboxMesh = new Mesh(true);
        for (float dir = -22.5f; dir < 360; dir += 22.5f) {
            rad = (float) (dir * FastMath.DEG_2_RAD);
            x = (float) FastMath.cos(rad);
            y = (float) FastMath.sin(rad);
            skyboxMesh.color(COLORS[0][0], COLORS[0][1], COLORS[0][2], 1.0f);
            skyboxMesh.vertex(0.0f, 0.0f, 0.1f);
            skyboxMesh.color(COLORS[1][0], COLORS[1][1], COLORS[1][2], 1.0f);
            skyboxMesh.vertex(x, y, 0.0f);
            skyboxMesh.vertex(lastX, lastY, 0.0f);
            skyboxMesh.vertex(lastX, lastY, 0.0f);
            skyboxMesh.vertex(x, y, 0.0f);
            skyboxMesh.vertex(0.0f, 0.0f, -1.0f);
            lastX = x;
            lastY = y;
        }
        this.skyboxMesh = skyboxMesh.finish();
        lastX = 1.0f;
        lastY = 0.0f;
        Mesh skyboxBottomMesh = new Mesh(true);
        skyboxBottomMesh.color(COLORS[1][0], COLORS[1][1], COLORS[1][2], 1.0f);
        for (float dir = -22.5f; dir < 360; dir += 22.5f) {
            rad = (float) (dir * FastMath.DEG_2_RAD);
            x = (float) FastMath.cos(rad);
            y = (float) FastMath.sin(rad);
            skyboxBottomMesh.vertex(lastX, lastY, -0.1f);
            skyboxBottomMesh
                    .color(COLORS[1][0], COLORS[1][1], COLORS[1][2], 0.0f);
            skyboxBottomMesh.vertex(lastX, lastY, 0.0f);
            skyboxBottomMesh.vertex(x, y, 0.0f);
            skyboxBottomMesh.vertex(x, y, 0.0f);
            skyboxBottomMesh
                    .color(COLORS[1][0], COLORS[1][1], COLORS[1][2], 1.0f);
            skyboxBottomMesh.vertex(x, y, -0.1f);
            skyboxBottomMesh.vertex(lastX, lastY, -0.1f);
            skyboxBottomMesh.vertex(lastX, lastY, -0.1f);
            skyboxBottomMesh.vertex(x, y, -0.1f);
            skyboxBottomMesh.vertex(0.0f, 0.0f, -1.0f);
            lastX = x;
            lastY = y;
        }
        this.skyboxBottomMesh = skyboxBottomMesh.finish();
        Mesh starMesh = new Mesh();
        Random random = new Random(seed);
        for (int i = 0; i < 1000; i++) {
            addStar(0.001 + FastMath.sqr(random.nextDouble()) * 0.009,
                    random.nextInt(COLORS.length), starMesh, random);
        }
        for (int i = 0; i < 10; i++) {
            addStar(0.01 + random.nextDouble() * 0.02, 0, starMesh, random);
        }
        this.starMesh = starMesh.finish();
    }

    private static void addStar(double size, int color, Mesh starMesh,
            Random random) {
        double pos1 = random.nextDouble() * 2.0 - 1.0;
        double pos2 = random.nextDouble() * 2.0 - 1.0;
        double pos3 = random.nextDouble() * 2.0 - 1.0;
        double rot = random.nextDouble() * FastMath.TWO_PI;
        double sqrt = 1.0 / Math.sqrt(pos1 * pos1 + pos2 * pos2 + pos3 * pos3);
        pos1 *= sqrt;
        pos2 *= sqrt;
        pos3 *= sqrt;
        double sin = FastMath.sin(rot);
        double cos = FastMath.cos(rot);
        double aTan1 = FastMath.atan2(pos1, pos3);
        double aTan2 =
                FastMath.atan2(Math.sqrt(pos1 * pos1 + pos3 * pos3), pos2);
        double cosATan1 = FastMath.cos(aTan1);
        double sinATan1 = FastMath.sin(aTan1);
        double cosATan2 = FastMath.cos(aTan2);
        double sinATan2 = FastMath.sin(aTan2);
        starMesh.color(STAR_COLORS[color][0], STAR_COLORS[color][1],
                STAR_COLORS[color][2], 1.0f);
        for (int vertex = 0; vertex < 4; vertex++) {
            double xx = (vertex & 2) - 1;
            double yy = (vertex + 1 & 2) - 1;
            starMesh.texture((float) xx, (float) yy);
            xx *= size;
            yy *= size;
            double var1 = xx * cos - yy * sin;
            double var2 = yy * cos + xx * sin;
            double var4 = -var1 * cosATan2;
            double xShift = var4 * sinATan1 - var2 * cosATan1;
            double zShift = var2 * sinATan1 + var4 * cosATan1;
            double yShift = var1 * sinATan2;
            starMesh.vertex((float) (pos1 + xShift), (float) (pos2 + yShift),
                    (float) (pos3 + zShift));
        }
    }

    @Override
    public void update(double delta) {
        MobPlayerClient player = world.player();
        double weather = climateGenerator.weather(FastMath.floor(player.x()),
                FastMath.floor(player.y()));
        rainGainWait -= delta;
        if (rainGainWait <= 0.0) {
            rainGainWait += 0.05;
            rainGain += (rainDrops / 128.0f - rainGain) * 0.04f;
            rainGain = FastMath.clamp(rainGain, 0.0f, 1.0f);
            rainDrops = 0;
            rainAudio.setGain(rainGain);
        }
        windAudio
                .setGain((float) FastMath.clamp(weather * 8.0 - 6.0, 0.0, 1.0));
        Vector3 weatherPos = player.pos()
                .plus(new Vector3d(player.speedX(), player.speedY(),
                        player.speedZ() + 16));
        double temperature = climateGenerator
                .temperature(weatherPos.intX(), weatherPos.intY(),
                        weatherPos.intZ());
        ParticleManager particleManager = world.particleManager();
        double downfallIntensity = FastMath.max(weather * 2.0 - 1.0, 0.0);
        Random random = ThreadLocalRandom.current();
        if (temperature > 0) {
            int amount = FastMath.round(downfallIntensity * 256.0 * delta);
            for (int i = 0; i < amount; i++) {
                particleManager.add(new ParticleRaindrop(particleManager,
                        weatherPos.plus(new Vector3d(
                                random.nextDouble() * 32 - 16,
                                random.nextDouble() * 32 - 16, 0)),
                        Vector3d.ZERO));
            }
        } else {
            int amount = FastMath.round(downfallIntensity * 64.0 * delta);
            for (int i = 0; i < amount; i++) {
                particleManager.add(new ParticleSnowflake(particleManager,
                        weatherPos.plus(new Vector3d(
                                random.nextDouble() * 32 - 16,
                                random.nextDouble() * 32 - 16, 0)),
                        new Vector3d(random.nextDouble() - 0.5,
                                random.nextDouble() - 0.5, 0),
                        random.nextDouble() * 360.0));
            }
        }
        // Debug
        temperatureDebug.setValue(climateGenerator
                .temperature(FastMath.floor(player.x()),
                        FastMath.floor(player.y()),
                        FastMath.floor(player.z())));
        humidityDebug.setValue(climateGenerator
                .humidity(FastMath.floor(player.x()),
                        FastMath.floor(player.y()),
                        FastMath.floor(player.z())));
        weatherDebug.setValue(weather);
        biomeDebug.setValue(biomeGenerator.get(player.x(), player.y()));
        TagStructure conditionTag =
                player.metaData("Vanilla").getStructure("Condition");
        staminaDebug.setValue(conditionTag.getDouble("Stamina"));
        hungerDebug.setValue(conditionTag.getDouble("Hunger"));
        thirstDebug.setValue(conditionTag.getDouble("Thirst"));
        bodyTemperatureDebug
                .setValue(conditionTag.getDouble("BodyTemperature"));
        exposureDebug.setValue(exposure);
    }

    @Override
    public void init(GL gl) {
        ScapesEngine engine = world.game().engine();
        GuiWidgetDebugValues debugValues = engine.debugValues();
        temperatureDebug = debugValues.get("Vanilla-Environment-Temperature");
        humidityDebug = debugValues.get("Vanilla-Environment-Humidity");
        weatherDebug = debugValues.get("Vanilla-Environment-Weather");
        biomeDebug = debugValues.get("Vanilla-Environment-Biome");
        staminaDebug = debugValues.get("Vanilla-Condition-Stamina");
        hungerDebug = debugValues.get("Vanilla-Condition-Hunger");
        thirstDebug = debugValues.get("Vanilla-Condition-Thirst");
        bodyTemperatureDebug =
                debugValues.get("Vanilla-Condition-Body-Temperature");
        exposureDebug = debugValues.get("Vanilla-Exposure");
        rainAudio = engine.sounds().playStaticAudio(
                "VanillaBasics:sound/entity/particle/rain/Rain1.ogg",
                "sound.Weather", 1.0f, 0.0f);
        windAudio = engine.sounds().playStaticAudio(
                "VanillaBasics:sound/entity/particle/rain/Wind1.ogg",
                "sound.Weather", 1.0f, 0.0f);
        MobPlayerClientMain player = world.player();
        if (player instanceof MobPlayerClientMainVB) {
            MobPlayerClientMainVB playerVB = (MobPlayerClientMainVB) player;
            new GuiComponentHotbar(world.scene().hud(), 8, 464, 560, 40,
                    playerVB);
            new GuiHudCondition(world.scene().hud(), playerVB);
        }
    }

    @Override
    public void renderUpdate(Cam cam, double delta) {
        MobPlayerClient player = world.player();
        SceneScapesVoxelWorld scene = world.scene();
        double factor = FastMath.min(1.0, delta * 10.0);
        if (world.terrain().sunLight(scene.cam().position.intX(),
                scene.cam().position.intY(), scene.cam().position.intZ()) > 0) {
            fogBrightness += (1.0f - fogBrightness) * factor;
        } else {
            fogBrightness -= fogBrightness * factor;
        }
        float skyLight = (float) (15.0 - climateGenerator
                .sunLightReduction(scene.cam().position.intX(),
                        scene.cam().position.intY())) / 15.0f;
        if (player.isHeadInWater()) {
            float light = FastMath.clamp(world.terrain()
                    .light(FastMath.floor(player.x()),
                            FastMath.floor(player.y()),
                            FastMath.floor(player.z() + 0.7)) * 0.09333f + 0.2f,
                    0.0f, 1.0f);
            fogR = 0.1f * light;
            fogG = 0.5f * light;
            fogB = 0.8f * light;
            fogDistance = 0.1f;
        } else {
            double latitude = climateGenerator.latitude(cam.position.doubleY());
            double elevation = climateGenerator.sunElevationD(latitude);
            float sunsetLight = (float) FastMath
                    .abs(FastMath.clamp(elevation * 2.0, -1.0, 1.0));
            fogR = FastMath.mix(1.2f, skyLight * fogBrightness, sunsetLight);
            fogG = FastMath.mix(0.4f, 0.9f * skyLight * fogBrightness,
                    sunsetLight);
            fogB = FastMath.mix(0.2f, 0.9f * skyLight * fogBrightness,
                    sunsetLight);
            fogDistance = 1.0f;
        }
        TagStructure conditionTag =
                world.player().metaData("Vanilla").getStructure("Condition");
        double temperature = conditionTag.getDouble("BodyTemperature");
        double heatstroke = FastMath.max((temperature - 37.1) * 7.5, 0.0) + 1.0;
        exposure += (heatstroke * 0.3 - exposure) * factor;
    }

    @Override
    public void render(GL gl, Cam cam) {
        MobPlayerClient player = world.player();
        SceneScapesVoxelWorld scene = world.scene();
        MatrixStack matrixStack = gl.matrixStack();
        float skyLight = (float) (15.0 - climateGenerator
                .sunLightReduction(scene.cam().position.intX(),
                        scene.cam().position.intY())) / 15.0f;
        float skyboxLight = skyLight * fogBrightness;
        float weather = (float) climateGenerator
                .weather(FastMath.floor(player.x()),
                        FastMath.floor(player.y()));
        float sunElevation = (float) (climateGenerator
                .sunElevation(cam.position.doubleX(), cam.position.doubleY()) *
                FastMath.RAD_2_DEG);
        float sunAzimuth = (float) (climateGenerator
                .sunAzimuth(cam.position.doubleX(), cam.position.doubleY()) *
                FastMath.RAD_2_DEG);
        // Sky
        gl.textures().unbind(gl);
        gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f);
        ShaderManager shaderManager = gl.shaders();
        Shader shader = shaderManager.get("VanillaBasics:shader/Skybox", gl);
        shader.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shader.setUniform1f(5, scene.fogDistance());
        shader.setUniform4f(6, skyboxLight * skyboxLight, skyboxLight,
                skyboxLight, 1.0f);
        skyboxMesh.render(gl, shader);
        // Stars
        if (skyLight < 1.0f) {
            Random random = ThreadLocalRandom.current();
            gl.setBlending(BlendingMode.ADD);
            shader = shaderManager.get("VanillaBasics:shader/Glow", gl);
            float brightness =
                    FastMath.max(1.0f - skyLight - random.nextFloat() * 0.1f,
                            0.0f);
            shader.setUniform4f(4, brightness, brightness, brightness, 1.0f);
            Matrix matrix = matrixStack.push();
            matrix.rotate(sunAzimuth + 180.0f, 0.0f, 0.0f, 1.0f);
            matrix.rotate(-sunElevation, 1.0f, 0.0f, 0.0f);
            starMesh.render(gl, shader);
            matrixStack.pop();
        } else {
            gl.setBlending(BlendingMode.ADD);
        }
        // Sun
        Matrix matrix = matrixStack.push();
        matrix.rotate(sunAzimuth + 180.0f, 0.0f, 0.0f, 1.0f);
        matrix.rotate(-sunElevation, 1.0f, 0.0f, 0.0f);
        matrix.scale(1.0f, 1.0f, 1.0f);
        shader = shaderManager.get("VanillaBasics:shader/Glow", gl);
        shader.setUniform4f(4, fogR * 1.0f, fogG * 1.1f, fogB * 1.1f, 1.0f);
        billboardMesh.render(gl, shader);
        matrix.scale(0.2f, 1.0f, 0.2f);
        shader.setUniform4f(4, fogR * 1.6f, fogG * 1.6f, fogB * 1.3f, 1.0f);
        billboardMesh.render(gl, shader);
        matrixStack.pop();
        // Moon
        shader = shaderManager.get("Engine:shader/Textured", gl);
        gl.textures().bind("VanillaBasics:image/Moon", gl);
        matrix = matrixStack.push();
        matrix.rotate(sunAzimuth, 0.0f, 0.0f, 1.0f);
        matrix.rotate(sunElevation, 1.0f, 0.0f, 0.0f);
        matrix.scale(0.1f, 1.0f, 0.1f);
        billboardMesh.render(gl, shader);
        matrixStack.pop();
        gl.setBlending(BlendingMode.NORMAL);
        shader = shaderManager.get("VanillaBasics:shader/Skybox", gl);
        // Clouds
        gl.textures().bind(fbo.texturesColor()[0], gl);
        cloudMesh.render(gl, shader);
        gl.textures().unbind(gl);
        // Bottom
        skyboxBottomMesh.render(gl, shader);
        float cloudTime = (System.currentTimeMillis() % 1000000) / 1000000.0f;
        fbo.activate(gl);
        gl.viewport(0, 0, fbo.width(), fbo.height());
        gl.clear(0.0f, 0.0f, 0.0f, 0.0f);
        gl.setProjectionOrthogonal(0, 0, 1, 1);
        shader = shaderManager.get("VanillaBasics:shader/Clouds", gl);
        shader.setUniform1f(4, cloudTime);
        shader.setUniform1f(5, weather);
        shader.setUniform2f(6,
                (float) (scene.cam().position.doubleX() / 2048.0 % 1024.0),
                (float) (scene.cam().position.doubleY() / 2048.0 % 1024.0));
        cloudTextureMesh.render(gl, shader);
        fbo.deactivate(gl);
    }

    @Override
    public void dispose(GL gl) {
        fbo.dispose(gl);
        rainAudio.dispose();
        windAudio.dispose();
    }

    @Override
    public float exposure() {
        return (float) exposure;
    }

    @Override
    public float fogR() {
        return fogR;
    }

    @Override
    public float fogG() {
        return fogG;
    }

    @Override
    public float fogB() {
        return fogB;
    }

    @Override
    public float fogDistance() {
        return fogDistance;
    }

    public void addRaindrop() {
        rainDrops++;
    }
}
