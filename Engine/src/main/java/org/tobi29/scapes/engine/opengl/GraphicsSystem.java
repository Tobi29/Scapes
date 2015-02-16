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

package org.tobi29.scapes.engine.opengl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureManager;
import org.tobi29.scapes.engine.utils.Sync;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.matrix.Matrix4f;
import org.tobi29.scapes.engine.utils.platform.Platform;

import java.io.IOException;

public class GraphicsSystem {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GraphicsSystem.class);
    private static final VAO CURSOR;
    private final ScapesEngine engine;
    private final FontRenderer defaultFont;
    private final Container container;
    private final TextureManager textureManager;
    private final ShaderManager shaderManager;
    private final Sync sync;
    private final GuiWidgetDebugValues.Element fpsDebug, widthDebug,
            heightDebug, textureDebug, vaoDebug;
    private final MatrixStack matrixStack;
    private final Matrix4f projectionMatrix, modelViewProjectionMatrix;
    private final OpenGL openGL;
    private boolean triggerScreenshot;
    private float resolutionMultiplier = 1.0f;
    private int containerWidth = 1, containerHeight = 1, contentWidth = 1,
            contentHeight = 1;

    static {
        CURSOR = VAOUtility.createVCTI(
                new float[]{-16.0f, -16.0f, 0.0f, 16.0f, -16.0f, 0.0f, -16.0f,
                        16.0f, 0.0f, 16.0f, 16.0f, 0.0f},
                new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f},
                new int[]{0, 1, 2, 1, 2, 3}, RenderType.TRIANGLES);
    }

    public GraphicsSystem(ScapesEngine engine, Container container) {
        this.engine = engine;
        this.container = container;
        openGL = container.getOpenGL();
        matrixStack = new MatrixStack(64);
        projectionMatrix = new Matrix4f();
        modelViewProjectionMatrix = new Matrix4f();
        textureManager = new TextureManager(engine);
        shaderManager = new ShaderManager(engine);
        resolutionMultiplier = engine.getConfig().getResolutionMultiplier();
        Platform platform = Platform.getPlatform();
        try {
            platform.loadFont(engine.getFiles()
                    .getResource("Engine:font/QuicksandPro-Regular.otf"));
        } catch (IOException e) {
            LOGGER.warn("Failed to load default font", e);
        }
        defaultFont = new FontRenderer(
                platform.getGlyphRenderer("Quicksand Pro", 64));
        GuiWidgetDebugValues debugValues = engine.getDebugValues();
        fpsDebug = debugValues.get("Graphics-Fps");
        widthDebug = debugValues.get("Graphics-Width");
        heightDebug = debugValues.get("Graphics-Height");
        textureDebug = debugValues.get("Graphics-Textures");
        vaoDebug = debugValues.get("Graphics-VAOs");
        sync = new Sync(engine.getConfig().getFPS(), 5000000000L, false,
                "Rendering");
    }

    public FontRenderer getDefaultFont() {
        return defaultFont;
    }

    public void dispose() {
        textureManager.clearCache(this);
        defaultFont.dispose(this);
        VAO.disposeAll(this);
        container.dispose();
    }

    public Container getContainer() {
        return container;
    }

    public TextureManager getTextureManager() {
        return textureManager;
    }

    public ShaderManager getShaderManager() {
        return shaderManager;
    }

    public ScapesEngine getEngine() {
        return engine;
    }

    public OpenGL getOpenGL() {
        return openGL;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getModelViewProjectionMatrix() {
        projectionMatrix.multiply(matrixStack.current().getModelViewMatrix(),
                modelViewProjectionMatrix);
        return modelViewProjectionMatrix;
    }

    public void init() {
        container.init();
        sync.init();
    }

    public void setProjectionPerspective(float width, float height, Cam cam) {
        projectionMatrix.identity();
        projectionMatrix
                .perspective(cam.fov, width / height, cam.near, cam.far);
        Matrix matrix = matrixStack.current();
        matrix.identity();
        Matrix4f viewMatrix = matrix.getModelViewMatrix();
        viewMatrix.rotate(-cam.tilt, 0.0f, 0.0f, 1.0f);
        viewMatrix.rotate(-cam.pitch - 90.0f, 1.0f, 0.0f, 0.0f);
        viewMatrix.rotate(-cam.yaw + 90.0f, 0.0f, 0.0f, 1.0f);
        openGL.enableCulling();
        openGL.enableDepthTest();
        openGL.setBlending(BlendingMode.NORMAL);
    }

    public void setProjectionOrthogonal(float x, float y, float width,
            float height) {
        projectionMatrix.identity();
        projectionMatrix
                .orthogonal(x, x + width, y + height, y, -1024.0f, 1024.0f);
        Matrix matrix = matrixStack.current();
        matrix.identity();
        openGL.disableCulling();
        openGL.disableDepthTest();
        openGL.setBlending(BlendingMode.NORMAL);
    }

    public void step() {
        GameState state = engine.getNewState();
        if (state == null) {
            state = engine.getState();
        }
        container.renderTick(state.forceRender());
        sync.capTPS();
    }

    public void render() {
        try {
            boolean fboSizeDirty;
            if (container.getContainerResized() || resolutionMultiplier !=
                    engine.getConfig().getResolutionMultiplier()) {
                resolutionMultiplier =
                        engine.getConfig().getResolutionMultiplier();
                containerWidth = container.getContainerWidth();
                containerHeight = container.getContainerHeight();
                contentWidth = container.getContentWidth();
                contentHeight = container.getContentHeight();
                fboSizeDirty = true;
                widthDebug.setValue(containerWidth);
                heightDebug.setValue(containerHeight);
            } else {
                fboSizeDirty = false;
            }
            double delta = sync.getSpeedFactor();
            engine.step();
            GameState state = engine.getState();
            state.render(this, fboSizeDirty);
            Shader shader = shaderManager.getShader("Engine:shader/Gui", this);
            engine.getGlobalGui().render(this, shader, defaultFont, delta);
            GuiController guiController = engine.getGuiController();
            if (guiController.isSoftwareMouse() && !state.isMouseGrabbed()) {
                setProjectionOrthogonal(0.0f, 0.0f, contentWidth,
                        contentHeight);
                textureManager.bind("Engine:image/Cursor", this);
                Matrix matrix = matrixStack.push();
                matrix.translate((float) guiController.getCursorX(),
                        (float) guiController.getCursorY(), 0.0f);
                CURSOR.render(this, shader);
                matrixStack.pop();
            }
            VAO.disposeUnused(this);
            Texture.disposeUnused(this);
            fpsDebug.setValue(sync.getTPS());
            textureDebug.setValue(Texture.getTextureCount());
            vaoDebug.setValue(VAO.getVAOCount());
            if (triggerScreenshot) {
                triggerScreenshot = false;
                try {
                    openGL.screenShot(
                            engine.getFiles().getFile("File:screenshots/" +
                                    System.currentTimeMillis() +
                                    ".png"), this);
                } catch (IOException e) {
                    LOGGER.warn("Failed to save screenshot: {}", e.toString());
                }
            }
        } catch (GraphicsException e) {
            LOGGER.warn("Graphics error during rendering: {}", e.toString());
        }
    }

    public Sync getSync() {
        return sync;
    }

    public int getSceneWidth() {
        return (int) (contentWidth / resolutionMultiplier);
    }

    public int getSceneHeight() {
        return (int) (contentHeight / resolutionMultiplier);
    }

    public int getContentWidth() {
        return contentWidth;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    public int getContainerWidth() {
        return containerWidth;
    }

    public int getContainerHeight() {
        return containerHeight;
    }

    public void triggerScreenshot() {
        triggerScreenshot = true;
    }

    @OpenGLFunction
    public void reset() {
        Texture.disposeAll(this);
        VAO.disposeAll(this);
        FBO.disposeAll(this);
        shaderManager.clearCache(this);
    }
}
