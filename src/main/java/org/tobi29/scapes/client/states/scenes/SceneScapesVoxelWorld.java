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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldSkybox;
import org.tobi29.scapes.client.gui.GuiComponentChat;
import org.tobi29.scapes.client.gui.GuiComponentGraph;
import org.tobi29.scapes.client.gui.GuiHud;
import org.tobi29.scapes.engine.gui.GuiComponentTextButton;
import org.tobi29.scapes.engine.gui.GuiWidget;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.shader.ShaderCompileInformation;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.graphics.BlurOffset;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.entity.skin.ClientSkinStorage;

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
    private final VAO vao;
    private final WorldClient world;
    private final Cam cam;
    private final GuiWidgetDebugValues.Element cameraPositionXDebug,
            cameraPositionYDebug, cameraPositionZDebug, lightDebug,
            blockLightDebug, sunLightDebug;
    private final ClientSkinStorage skinStorage;
    private final GuiWidgetDebugClient debugWidget;
    private final GuiWidgetPerformanceClient performanceWidget;
    private final GuiComponentChat chat = new GuiComponentChat(8, 416, 0, 0);
    private float brightness;
    private float renderDistance, fov;
    private int flashDir;
    private long flashTime, flashStart;
    private boolean guiHide, mouseGrabbed, wireframe;
    private Image[] panorama;
    private FBO skyboxFBO, exposureFBO;
    private GuiHud hud;
    private WorldSkybox skybox;
    private TerrainTextureRegistry terrainTextureRegistry;
    private boolean chunkGeometryDebug;

    public SceneScapesVoxelWorld(WorldClient world, Cam cam) {
        this.world = world;
        this.cam = cam;
        GuiWidgetDebugValues debugValues =
                world.getGame().getEngine().getDebugValues();
        cameraPositionXDebug = debugValues.get("Camera-Position-X");
        cameraPositionYDebug = debugValues.get("Camera-Position-Y");
        cameraPositionZDebug = debugValues.get("Camera-Position-Z");
        lightDebug = debugValues.get("Camera-Light");
        blockLightDebug = debugValues.get("Camera-Block-Light");
        sunLightDebug = debugValues.get("Camera-Sun-Light");
        debugWidget = new GuiWidgetDebugClient();
        addGui(debugWidget);
        debugWidget.setVisible(false);
        performanceWidget = new GuiWidgetPerformanceClient();
        addGui(performanceWidget);
        performanceWidget.setVisible(false);
        skinStorage = new ClientSkinStorage(
                world.getGame().getEngine().getGraphics().getTextureManager()
                        .getTexture("Scapes:image/entity/mob/Player"));
        vao = VAOUtility.createVTI(
                new float[]{0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f,
                        0.0f, 1.0f, 0.0f, 0.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
        TagStructure scapesTag = world.getGame().getEngine().getTagStructure()
                .getStructure("Scapes");
        animationDistance = scapesTag.getFloat("AnimationDistance");
        fxaa = scapesTag.getBoolean("FXAA");
        bloom = scapesTag.getBoolean("Bloom");
    }

    public float getFogR() {
        return skybox.getFogR();
    }

    public float getFogG() {
        return skybox.getFogG();
    }

    public float getFogB() {
        return skybox.getFogB();
    }

    public float getFogDistance() {
        return skybox.getFogDistance();
    }

    public float getRenderDistance() {
        return renderDistance;
    }

    public boolean isGuiHidden() {
        return guiHide;
    }

    public boolean isMouseGrabbed() {
        return mouseGrabbed;
    }

    public Image[] getPanorama() {
        return panorama;
    }

    public void damageShake(double damage) {
        flashTime = System.currentTimeMillis() +
                (int) FastMath.ceil(damage) * 10 + 100;
        flashStart = System.currentTimeMillis();
        Random random = ThreadLocalRandom.current();
        flashDir = 1 - (random.nextInt(2) << 1);
    }

    public void setGuiHide(boolean guiHide) {
        this.guiHide = guiHide;
    }

    public void chat(String line) {
        chat.addLine(line);
    }

    public MobPlayerClientMain getPlayer() {
        return world.getPlayer();
    }

    public WorldClient getWorld() {
        return world;
    }

    public Cam getCam() {
        return cam;
    }

    public ClientSkinStorage getSkinStorage() {
        return skinStorage;
    }

    @Override
    public void renderGui(GraphicsSystem graphics, Shader shader,
            double delta) {
        if (!guiHide) {
            super.renderGui(graphics, shader, delta);
        }
    }

    @Override
    public void init(GraphicsSystem graphics) {
        MobPlayerClientMain player = world.getPlayer();
        skyboxFBO = new FBO(1, 1, 1, false, true, false, graphics);
        exposureFBO = new FBO(1, 1, 1, false, true, false, graphics);
        long time = System.currentTimeMillis();
        terrainTextureRegistry = new TerrainTextureRegistry(state.getEngine());
        for (Material type : world.getRegistry().getMaterials()) {
            if (type != null) {
                type.registerTextures(terrainTextureRegistry);
            }
        }
        int size = terrainTextureRegistry.init();
        time = System.currentTimeMillis() - time;
        LOGGER.info("Created terrain texture atlas with {} textures in {} ms.",
                size, time);
        for (Material type : world.getRegistry().getMaterials()) {
            if (type != null) {
                type.createModels(terrainTextureRegistry);
            }
        }
        hud = new GuiHud(player);
        hud.add(chat);
        addGui(hud);
        ShaderManager shaderManager = graphics.getShaderManager();
        ShaderCompileInformation information =
                shaderManager.getCompileInformation("Scapes:shader/Terrain");
        information.supplyDefine("ENABLE_ANIMATIONS", animationDistance > 0.0f);
        information =
                shaderManager.getCompileInformation("Scapes:shader/Exposure");
        information.supplyExternal("SAMPLE_OFFSET", SAMPLE_OFFSET);
        information.supplyExternal("SAMPLE_WEIGHT", SAMPLE_WEIGHT);
        information.supplyExternal("SAMPLE_LENGTH", SAMPLE_LENGTH);
        if (bloom) {
            information = shaderManager
                    .getCompileInformation("Scapes:shader/Composite1");
            information.supplyExternal("BLUR_OFFSET", BLUR_OFFSET);
            information.supplyExternal("BLUR_WEIGHT", BLUR_WEIGHT);
            information.supplyExternal("BLUR_LENGTH", BLUR_LENGTH);
        }
        information =
                shaderManager.getCompileInformation("Scapes:shader/Composite2");
        information.supplyDefine("ENABLE_BLOOM", bloom);
        if (bloom) {
            information.supplyExternal("BLUR_OFFSET", BLUR_OFFSET);
            information.supplyExternal("BLUR_WEIGHT", BLUR_WEIGHT);
            information.supplyExternal("BLUR_LENGTH", BLUR_LENGTH);
        }
        skybox = world.getEnvironment().createSkybox(world);
        skybox.init(graphics, cam);
    }

    @Override
    public void renderScene(GraphicsSystem graphics) {
        MobPlayerClientMain player = world.getPlayer();
        MobModel playerModel = world.getPlayerModel();
        float blackout = 1.0f -
                FastMath.clamp(1.0f - (float) player.getLives() * 0.05f, 0.0f,
                        1.0f);
        brightness += (blackout - brightness) * 0.1;
        brightness = FastMath.clamp(brightness, 0, 1);
        mouseGrabbed = !player.hasGui();
        float pitch = playerModel.getPitch();
        float tilt = 0.0f;
        float yaw = playerModel.getYaw();
        long flashDiff = flashTime - flashStart, flashPos =
                System.currentTimeMillis() - flashStart;
        if (flashDiff > 0.0f && flashPos > 0.0f) {
            double flashDiv = (double) flashPos / flashDiff;
            if (flashDiv < 1.0f) {
                if ((double) flashPos / flashDiff > 0.5f) {
                    tilt += (1 - flashDiv) * flashDir *
                            flashDiff * 0.1f;
                } else {
                    tilt += flashDiv * flashDir * flashDiff *
                            0.1f;
                }
            }
        }
        cam.setView(playerModel.getPos().plus(player.getViewOffset()),
                player.getSpeed(), pitch, yaw, tilt);
        cam.setPerspective(
                (float) graphics.getSceneWidth() / graphics.getSceneHeight(),
                fov);
        graphics.getEngine().getSounds()
                .setListener(cam.position.now(), player.getRot(),
                        player.getSpeed());
        terrainTextureRegistry.render(graphics);
        renderWorld(graphics, cam);
    }

    @Override
    public void postRender(GraphicsSystem graphics, double delta) {
        OpenGL openGL = graphics.getOpenGL();
        state.getFBOScene().getTexturesColor()[0].bind(graphics);
        exposureFBO.activate(graphics);
        openGL.viewport(0, 0, 1, 1);
        graphics.setProjectionOrthogonal(0.0f, 0.0f, 1.0f, 1.0f);
        Shader shader = graphics.getShaderManager()
                .getShader("Scapes:shader/Exposure", graphics);
        shader.setUniform1f(4, (float) FastMath.min(1.0, delta * 0.5));
        vao.render(graphics, shader);
        exposureFBO.deactivate(graphics);
        MobPlayerClientMain player = world.getPlayer();
        float newRenderDistance =
                (float) world.getTerrain().renderer().getActualRenderDistance();
        if (renderDistance > newRenderDistance) {
            renderDistance = newRenderDistance;
        } else {
            double factor = FastMath.min(1.0, delta);
            renderDistance += (newRenderDistance - renderDistance) * factor;
        }
        if (!Float.isFinite(renderDistance)) {
            renderDistance = 0.0f;
        }
        float newFov = FastMath.min((float) FastMath
                        .sqrt(FastMath.sqr(player.getXSpeed()) +
                                FastMath.sqr(player.getYSpeed())) * 2.0f +
                        90.0f, 120.0f);
        double factor = FastMath.min(1.0, delta * 10.0);
        fov += (newFov - fov) * factor;
        if (!Float.isFinite(fov)) {
            fov = 90.0f;
        }
        cameraPositionXDebug.setValue(cam.position.doubleX());
        cameraPositionYDebug.setValue(cam.position.doubleY());
        cameraPositionZDebug.setValue(cam.position.doubleZ());
        int xx = FastMath.floor(cam.position.doubleX());
        int yy = FastMath.floor(cam.position.doubleY());
        int zz = FastMath.floor(cam.position.doubleZ());
        lightDebug.setValue(world.getTerrain().light(xx, yy, zz));
        blockLightDebug.setValue(world.getTerrain().blockLight(xx, yy, zz));
        sunLightDebug.setValue(world.getTerrain().sunLight(xx, yy, zz));
        performanceWidget.graphRender.addStamp(delta);
        world.updateRender(cam, delta);
        skybox.renderUpdate(graphics, cam, delta);
        skinStorage.update(graphics, player.getGame().getClient());
    }

    @Override
    public Shader postProcessing(GraphicsSystem graphics, int pass) {
        ShaderManager shaderManager = graphics.getShaderManager();
        if (fxaa) {
            if (pass == 0) {
                OpenGL openGL = graphics.getOpenGL();
                openGL.activeTexture(3);
                graphics.getTextureManager()
                        .bind("Scapes:image/Noise", graphics);
                openGL.activeTexture(0);
                Shader shader =
                        shaderManager.getShader("Scapes:shader/Fxaa", graphics);
                shader.setUniform1i(4, 3);
                shader.setUniform2f(5, graphics.getSceneWidth(),
                        graphics.getSceneHeight());
                return shader;
            } else {
                pass--;
            }
        }
        if (pass == 0 && bloom) {
            return shaderManager
                    .getShader("Scapes:shader/Composite1", graphics);
        } else {
            OpenGL openGL = graphics.getOpenGL();
            openGL.activeTexture(3);
            exposureFBO.getTexturesColor()[0].bind(graphics);
            openGL.activeTexture(0);
            Shader shader = shaderManager
                    .getShader("Scapes:shader/Composite2", graphics);
            shader.setUniform1f(6, brightness);
            shader.setUniform1f(7,
                    (float) FastMath.pow(2.0, skybox.getExposure()));
            shader.setUniform1i(8, 2);
            shader.setUniform1i(9, 3);
            return shader;
        }
    }

    @Override
    public int getRenderPasses() {
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
    public int getColorAttachments() {
        if (bloom) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public void dispose(GraphicsSystem graphics) {
        FBO fbo = new FBO(256, 256, 1, true, false, false, graphics);
        fbo.activate(graphics);
        panorama = takePanorama(graphics, fbo);
        fbo.deactivate(graphics);
        skybox.dispose(graphics, cam);
        fbo.dispose(graphics);
        skyboxFBO.dispose(graphics);
        exposureFBO.dispose(graphics);
        world.dispose();
        terrainTextureRegistry.dispose(graphics);
        skinStorage.dispose(graphics);
    }

    public Image[] takePanorama(GraphicsSystem graphics, FBO fbo) {
        guiHide = true;
        Image[] images = new Image[6];
        Cam cam = new Cam(this.cam.near, this.cam.far);
        cam.setPerspective(1.0f, 90.0f);
        OpenGL openGL = graphics.getOpenGL();
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
            openGL.clearDepth();
            renderWorld(graphics, cam, 256, 256);
            images[i] = graphics.getOpenGL().screenShotFBO(graphics, fbo, 0);
        }
        guiHide = false;
        return images;
    }

    public void renderWorld(GraphicsSystem graphics, Cam cam) {
        renderWorld(graphics, cam, graphics.getSceneWidth(),
                graphics.getSceneHeight());
    }

    public void renderWorld(GraphicsSystem graphics, Cam cam, int width,
            int height) {
        if (width != skyboxFBO.getWidth() || height != skyboxFBO.getHeight()) {
            skyboxFBO.setSize(width, height, graphics);
        }
        OpenGL openGL = graphics.getOpenGL();
        MatrixStack matrixStack = graphics.getMatrixStack();
        openGL.viewport(0, 0, width, height);
        skyboxFBO.activate(graphics);
        openGL.disableDepthTest();
        openGL.disableDepthMask();
        graphics.setProjectionPerspective(width, height, cam);
        matrixStack.push();
        skybox.render(graphics, cam);
        matrixStack.pop();
        skyboxFBO.deactivate(graphics);
        openGL.viewport(0, 0, width, height);
        graphics.setProjectionOrthogonal(0.0f, 0.0f, 1.0f, 1.0f);
        graphics.getTextureManager()
                .bind(skyboxFBO.getTexturesColor()[0], graphics);
        Shader shader = graphics.getShaderManager()
                .getShader("Engine:shader/Textured", graphics);
        vao.render(graphics, shader);
        graphics.setProjectionPerspective(width, height, cam);
        openGL.enableDepthTest();
        openGL.enableDepthMask();
        openGL.activeTexture(1);
        graphics.getTextureManager()
                .bind(skyboxFBO.getTexturesColor()[0], graphics);
        openGL.activeTexture(0);
        boolean wireframe = this.wireframe;
        if (wireframe) {
            graphics.getOpenGL().enableWireframe();
        }
        world.render(graphics, cam, animationDistance, chunkGeometryDebug);
        if (wireframe) {
            graphics.getOpenGL().disableWireframe();
        }
    }

    public void update(double delta) {
        world.update(delta);
        performanceWidget.graphUpdate.addStamp(delta);
    }

    public void setHudVisible(boolean visible) {
        hud.setVisible(visible);
    }

    public void toggleDebug() {
        debugWidget.setVisible(!debugWidget.isVisible());
    }

    public TerrainTextureRegistry getTerrainTextureRegistry() {
        return terrainTextureRegistry;
    }

    public WorldSkybox getSkybox() {
        return skybox;
    }

    public GuiComponentChat getChat() {
        return chat;
    }

    public GuiHud getHud() {
        return hud;
    }

    private static class GuiWidgetPerformanceClient extends GuiWidget {
        private final GuiComponentGraph graphRender, graphUpdate;

        private GuiWidgetPerformanceClient() {
            super(32, 32, 240, 80, "Performance Graph");
            graphRender =
                    new GuiComponentGraph(0, 0, width, height, 0.0f, 1.0f, 0.0f,
                            1.0f);
            add(graphRender);
            graphUpdate =
                    new GuiComponentGraph(0, 0, width, height, 0.0f, 0.0f, 1.0f,
                            1.0f);
            add(graphUpdate);
        }
    }

    private class GuiWidgetDebugClient extends GuiWidget {
        private GuiWidgetDebugClient() {
            super(32, 32, 160, 120, "Debug Values");
            GuiComponentTextButton geometryButton =
                    new GuiComponentTextButton(10, 10, 140, 15, 12, "Geometry");
            geometryButton.addLeftClick(
                    event -> chunkGeometryDebug = !chunkGeometryDebug);
            add(geometryButton);
            GuiComponentTextButton wireframeButton =
                    new GuiComponentTextButton(10, 30, 140, 15, 12,
                            "Wireframe");
            wireframeButton.addLeftClick(event -> wireframe = !wireframe);
            add(wireframeButton);
            GuiComponentTextButton distanceButton =
                    new GuiComponentTextButton(10, 50, 140, 15, 12,
                            "Static Render Distance");
            distanceButton.addLeftClick(
                    event -> world.getTerrain().toggleStaticRenderDistance());
            add(distanceButton);
            GuiComponentTextButton reloadGeometryButton =
                    new GuiComponentTextButton(10, 70, 140, 15, 12,
                            "Reload Geometry");
            reloadGeometryButton
                    .addLeftClick(event -> world.getTerrain().reloadGeometry());
            add(reloadGeometryButton);
            GuiComponentTextButton performanceButton =
                    new GuiComponentTextButton(10, 90, 140, 15, 12,
                            "Performance");
            performanceButton.addLeftClick(event -> performanceWidget
                    .setVisible(!performanceWidget.isVisible()));
            add(performanceButton);
        }
    }
}
