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
package org.tobi29.scapes.client.states.scenes;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldSkybox;
import org.tobi29.scapes.client.gui.GuiComponentChat;
import org.tobi29.scapes.client.states.GameStateGameSP;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.graphics.BlurOffset;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.entity.particle.*;
import org.tobi29.scapes.entity.skin.ClientSkinStorage;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SceneScapesVoxelWorld extends Scene {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SceneScapesVoxelWorld.class);
    private static final String BLUR_OFFSET, BLUR_WEIGHT, SAMPLE_OFFSET,
            SAMPLE_WEIGHT;
    private static final int BLUR_LENGTH, SAMPLE_LENGTH;

    static {
        float[] blurOffset = BlurOffset.gaussianBlurOffset(5, 0.01f);
        float[] blurWeight = BlurOffset.gaussianBlurWeight(5, sample -> FastMath
                .pow(FastMath.cos(sample * FastMath.PI), 0.1));
        BLUR_LENGTH = blurOffset.length;
        BLUR_OFFSET = ArrayUtil.join(blurOffset);
        BLUR_WEIGHT = ArrayUtil.join(blurWeight);
        float[] sampleOffset = BlurOffset.gaussianBlurOffset(11, 0.5f);
        float[] sampleWeight = BlurOffset.gaussianBlurWeight(11,
                sample -> FastMath
                        .pow(FastMath.cos(sample * FastMath.PI), 0.1));
        for (int i = 0; i < sampleOffset.length; i++) {
            sampleOffset[i] = sampleOffset[i] + 0.5f;
        }
        SAMPLE_LENGTH = sampleOffset.length;
        SAMPLE_OFFSET = ArrayUtil.join(sampleOffset);
        SAMPLE_WEIGHT = ArrayUtil.join(sampleWeight);
    }

    public final float animationDistance;
    public final boolean fxaa, bloom;
    private final Model model;
    private final WorldClient world;
    private final Cam cam;
    private final GuiWidgetDebugValues.Element cameraPositionXDebug,
            cameraPositionYDebug, cameraPositionZDebug, lightDebug,
            blockLightDebug, sunLightDebug;
    private final TerrainTextureRegistry terrainTextureRegistry;
    private final ClientSkinStorage skinStorage;
    private final Framebuffer skyboxFBO;
    private final Optional<Framebuffer> exposureFBO;
    private final ParticleSystem particles;
    private final WorldSkybox skybox;
    private final Shader shaderExposure, shaderFXAA, shaderComposite1,
            shaderComposite2, shaderTextured;
    private float brightness;
    private float renderDistance, fov;
    private int flashDir;
    private long flashTime, flashStart;
    private boolean mouseGrabbed, chunkGeometryDebug, wireframe;

    public SceneScapesVoxelWorld(WorldClient world, Cam cam) {
        this.world = world;
        this.cam = cam;
        particles = new ParticleSystem(world, 60.0);
        terrainTextureRegistry = world.game().terrainTextureRegistry();
        skinStorage = new ClientSkinStorage(world.game().engine(),
                world.game().engine().graphics().textures()
                        .get("Scapes:image/entity/mob/Player"));
        GuiWidgetDebugValues debugValues = world.game().engine().debugValues();
        cameraPositionXDebug = debugValues.get("Camera-Position-X");
        cameraPositionYDebug = debugValues.get("Camera-Position-Y");
        cameraPositionZDebug = debugValues.get("Camera-Position-Z");
        lightDebug = debugValues.get("Camera-Light");
        blockLightDebug = debugValues.get("Camera-Block-Light");
        sunLightDebug = debugValues.get("Camera-Sun-Light");
        model = VAOUtility.createVTI(world.game().engine(),
                new float[]{0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f,
                        0.0f, 1.0f, 0.0f, 0.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
        TagStructure scapesTag =
                world.game().engine().tagStructure().getStructure("Scapes");
        animationDistance = scapesTag.getFloat("AnimationDistance");
        fxaa = scapesTag.getBoolean("FXAA");
        bloom = scapesTag.getBoolean("Bloom");
        skybox = world.environment().createSkybox(world);
        skyboxFBO = world.game().engine().graphics()
                .createFramebuffer(1, 1, 1, false, true, false);
        if (scapesTag.getBoolean("AutoExposure")) {
            exposureFBO = Optional.of(world.game().engine().graphics()
                    .createFramebuffer(1, 1, 1, false, true, false));
        } else {
            exposureFBO = Optional.empty();
        }
        GraphicsSystem graphics = world.game().engine().graphics();
        shaderExposure = graphics.createShader("Scapes:shader/Exposure",
                information -> information.supplyPreCompile(shader -> {
                    shader.supplyProperty("BLUR_OFFSET", SAMPLE_OFFSET);
                    shader.supplyProperty("BLUR_WEIGHT", SAMPLE_WEIGHT);
                    shader.supplyProperty("BLUR_LENGTH", SAMPLE_LENGTH);
                }));
        shaderFXAA = graphics.createShader("Scapes:shader/FXAA");
        shaderComposite1 = graphics.createShader("Scapes:shader/Composite1",
                information -> information.supplyPreCompile(shader -> {
                    shader.supplyProperty("BLUR_OFFSET", BLUR_OFFSET);
                    shader.supplyProperty("BLUR_WEIGHT", BLUR_WEIGHT);
                    shader.supplyProperty("BLUR_LENGTH", BLUR_LENGTH);
                }));
        shaderComposite2 = graphics.createShader("Scapes:shader/Composite2",
                information -> information.supplyPreCompile(shader -> {
                    shader.supplyProperty("ENABLE_BLOOM", bloom);
                    shader.supplyProperty("ENABLE_EXPOSURE",
                            exposureFBO.isPresent());
                    shader.supplyProperty("BLUR_OFFSET", BLUR_OFFSET);
                    shader.supplyProperty("BLUR_WEIGHT", BLUR_WEIGHT);
                    shader.supplyProperty("BLUR_LENGTH", BLUR_LENGTH);
                }));
        shaderTextured = graphics.createShader("Engine:shader/Textured");
        // TODO: Move somewhere better
        particles.register(new ParticleEmitterBlock(particles,
                terrainTextureRegistry.texture()));
        particles.register(new ParticleEmitter3DBlock(particles));
        particles.register(new ParticleEmitterFallenBodyPart(particles));
        particles.register(new ParticleEmitterTransparent(particles,
                world.game().particleTransparentAtlas().texture()));
    }

    public float fov() {
        return fov;
    }

    public float fogR() {
        return skybox.fogR();
    }

    public float fogG() {
        return skybox.fogG();
    }

    public float fogB() {
        return skybox.fogB();
    }

    public float fogDistance() {
        return skybox.fogDistance();
    }

    public float renderDistance() {
        return renderDistance;
    }

    public boolean isMouseGrabbed() {
        return mouseGrabbed;
    }

    public void damageShake(double damage) {
        flashTime =
                System.currentTimeMillis() + (int) FastMath.ceil(damage) * 10 +
                        100;
        flashStart = System.currentTimeMillis();
        Random random = ThreadLocalRandom.current();
        flashDir = 1 - (random.nextInt(2) << 1);
    }

    public MobPlayerClientMain player() {
        return world.player();
    }

    public WorldClient world() {
        return world;
    }

    public ParticleSystem particles() {
        return particles;
    }

    public Cam cam() {
        return cam;
    }

    public ClientSkinStorage skinStorage() {
        return skinStorage;
    }

    @Override
    public void init(GL gl) {
        Gui hud = world.game().hud();
        hud.removeAll();
        hud.add(8, 434, -1, -1,
                p -> new GuiComponentChat(p, world.game().chatHistory()));
        skybox.init(gl);
    }

    @Override
    public void renderScene(GL gl) {
        MobPlayerClientMain player = world.player();
        MobModel playerModel = world.playerModel();
        float blackout = 1.0f -
                FastMath.clamp(1.0f - (float) player.health() * 0.05f, 0.0f,
                        1.0f);
        brightness += (blackout - brightness) * 0.1;
        brightness = FastMath.clamp(brightness, 0, 1);
        mouseGrabbed = !player.hasGui();
        float pitch = playerModel.pitch();
        float tilt = 0.0f;
        float yaw = playerModel.yaw();
        long flashDiff = flashTime - flashStart, flashPos =
                System.currentTimeMillis() - flashStart;
        if (flashDiff > 0.0f && flashPos > 0.0f) {
            double flashDiv = (double) flashPos / flashDiff;
            if (flashDiv < 1.0f) {
                if ((double) flashPos / flashDiff > 0.5f) {
                    tilt += (1 - flashDiv) * flashDir * flashDiff * 0.1f;
                } else {
                    tilt += flashDiv * flashDir * flashDiff * 0.1f;
                }
            }
        }
        cam.setView(playerModel.pos().plus(player.viewOffset()), player.speed(),
                pitch, yaw, tilt);
        cam.setPerspective((float) gl.sceneWidth() / gl.sceneHeight(), fov);
        state.engine().sounds()
                .setListener(cam.position.now(), player.rot(), player.speed());
        terrainTextureRegistry.render(gl);
        renderWorld(gl, cam);
    }

    @Override
    public void postRender(GL gl, double delta) {
        if (exposureFBO.isPresent()) {
            Framebuffer exposureFBO = this.exposureFBO.get();
            state.fbo(0).textureColor(0).bind(gl);
            exposureFBO.activate(gl);
            gl.viewport(0, 0, 1, 1);
            gl.setProjectionOrthogonal(0.0f, 0.0f, 1.0f, 1.0f);
            shaderExposure
                    .setUniform1f(4, (float) FastMath.min(1.0, delta * 0.5));
            model.render(gl, shaderExposure);
            exposureFBO.deactivate(gl);
        }
        MobPlayerClientMain player = world.player();
        float newRenderDistance =
                (float) world.terrain().renderer().actualRenderDistance();
        if (renderDistance > newRenderDistance) {
            renderDistance = newRenderDistance;
        } else {
            double factor = FastMath.min(1.0, delta);
            renderDistance += (newRenderDistance - renderDistance) * factor;
        }
        float newFov = FastMath.min((float) FastMath
                .sqrt(FastMath.sqr(player.speedX()) +
                        FastMath.sqr(player.speedY())) * 2.0f + 90.0f, 120.0f);
        double factor = FastMath.min(1.0, delta * 10.0);
        fov += (newFov - fov) * factor;
        cameraPositionXDebug.setValue(cam.position.doubleX());
        cameraPositionYDebug.setValue(cam.position.doubleY());
        cameraPositionZDebug.setValue(cam.position.doubleZ());
        int xx = FastMath.floor(cam.position.doubleX());
        int yy = FastMath.floor(cam.position.doubleY());
        int zz = FastMath.floor(cam.position.doubleZ());
        lightDebug.setValue(world.terrain().light(xx, yy, zz));
        blockLightDebug.setValue(world.terrain().blockLight(xx, yy, zz));
        sunLightDebug.setValue(world.terrain().sunLight(xx, yy, zz));
        world.game().renderTimestamp(delta);
        world.updateRender(cam, delta);
        skybox.renderUpdate(cam, delta);
        skinStorage.update(player.game().client());
    }

    @Override
    public Shader postProcessing(GL gl, int pass) {
        if (fxaa) {
            if (pass == 0) {
                gl.activeTexture(3);
                gl.textures().bind("Scapes:image/Noise", gl);
                gl.activeTexture(0);
                shaderFXAA.setUniform1i(4, 3);
                return shaderFXAA;
            } else {
                pass--;
            }
        }
        if (pass == 0 && bloom) {
            return shaderComposite1;
        } else {
            if (exposureFBO.isPresent()) {
                gl.activeTexture(3);
                exposureFBO.get().textureColor(0).bind(gl);
                gl.activeTexture(0);
            }
            shaderComposite2.setUniform1f(6, brightness);
            shaderComposite2.setUniform1f(7,
                    (float) FastMath.pow(2.0, skybox.exposure()));
            shaderComposite2.setUniform1i(8, 2);
            shaderComposite2.setUniform1i(9, 3);
            return shaderComposite2;
        }
    }

    @Override
    public int renderPasses() {
        if (fxaa) {
            if (bloom) {
                return 3;
            } else {
                return 2;
            }
        } else if (bloom) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public int colorAttachments() {
        if (bloom) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public void dispose(GL gl) {
        skybox.dispose(gl);
    }

    @Override
    public void dispose() {
        world.dispose();
        particles.dispose();
    }

    public void takePanorama(GL gl) {
        Framebuffer fbo = state.engine().graphics()
                .createFramebuffer(256, 256, 1, true, false, false);
        fbo.activate(gl);
        Image[] panorama = takePanorama(gl, fbo);
        fbo.deactivate(gl);
        if (state instanceof GameStateGameSP) {
            try {
                ((GameStateGameSP) state).source().panorama(panorama);
            } catch (IOException e) {
                LOGGER.warn("Failed to save panorama", e);
            }
        }
    }

    public Image[] takePanorama(GL gl, Framebuffer fbo) {
        world.game().setHudVisible(false);
        Image[] images = new Image[6];
        Cam cam = new Cam(this.cam.near, this.cam.far);
        cam.setPerspective(1.0f, 90.0f);
        for (int i = 0; i < 6; i++) {
            float pitch = 0.0f;
            float yaw = 0.0f;
            if (i == 1) {
                yaw = 90.0f;
            } else if (i == 2) {
                yaw = 180.0f;
            } else if (i == 3) {
                yaw = 270.0f;
            } else if (i == 4) {
                pitch = -90.0f;
            } else if (i == 5) {
                pitch = 90.0f;
            }
            cam.setView(this.cam.position.now(), this.cam.velocity.now(), pitch,
                    yaw, 0.0f);
            gl.clearDepth();
            renderWorld(gl, cam, 256, 256);
            gl.textures().bind(fbo.textureColor(0), gl);
            images[i] = gl.screenShotFBO(fbo);
        }
        world.game().setHudVisible(true);
        return images;
    }

    public void renderWorld(GL gl, Cam cam) {
        renderWorld(gl, cam, gl.sceneWidth(), gl.sceneHeight());
    }

    public void renderWorld(GL gl, Cam cam, int width, int height) {
        if (width != skyboxFBO.width() || height != skyboxFBO.height()) {
            skyboxFBO.setSize(width, height);
        }
        MatrixStack matrixStack = gl.matrixStack();
        gl.viewport(0, 0, width, height);
        skyboxFBO.activate(gl);
        gl.disableDepthTest();
        gl.disableDepthMask();
        gl.setProjectionPerspective(width, height, cam);
        matrixStack.push();
        skybox.render(gl, cam);
        matrixStack.pop();
        skyboxFBO.deactivate(gl);
        gl.viewport(0, 0, width, height);
        gl.setProjectionOrthogonal(0.0f, 0.0f, 1.0f, 1.0f);
        gl.textures().bind(skyboxFBO.textureColor(0), gl);
        model.render(gl, shaderTextured);
        gl.setProjectionPerspective(width, height, cam);
        gl.enableDepthTest();
        gl.enableDepthMask();
        gl.activeTexture(1);
        gl.textures().bind(skyboxFBO.textureColor(0), gl);
        gl.activeTexture(0);
        boolean wireframe = this.wireframe;
        if (wireframe) {
            gl.enableWireframe();
        }
        world.render(gl, cam, animationDistance, chunkGeometryDebug);
        if (wireframe) {
            gl.disableWireframe();
        }
    }

    public void toggleChunkDebug() {
        chunkGeometryDebug = !chunkGeometryDebug;
    }

    public void toggleWireframe() {
        wireframe = !wireframe;
    }

    public TerrainTextureRegistry terrainTextureRegistry() {
        return terrainTextureRegistry;
    }

    public WorldSkybox skybox() {
        return skybox;
    }
}
