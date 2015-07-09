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

package org.tobi29.scapes.engine;

import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.texture.TextureFBOColor;
import org.tobi29.scapes.engine.utils.Sync;

public abstract class GameState {
    protected static final VAO CURSOR;

    static {
        CURSOR = VAOUtility.createVCTI(
                new float[]{-16.0f, -16.0f, 0.0f, 16.0f, -16.0f, 0.0f, -16.0f,
                        16.0f, 0.0f, 16.0f, 16.0f, 0.0f},
                new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f},
                new int[]{0, 1, 2, 1, 2, 3}, RenderType.TRIANGLES);
    }

    protected final VAO vao;
    protected final ScapesEngine engine;
    protected final FontRenderer font;
    protected Scene scene, newScene;
    protected FBO fboScene, fboFront, fboBack;

    protected GameState(ScapesEngine engine, Scene scene) {
        this(engine, scene, engine.getGraphics().getDefaultFont());
    }

    protected GameState(ScapesEngine engine, Scene scene, FontRenderer font) {
        this.font = font;
        this.engine = engine;
        this.scene = scene;
        newScene = scene;
        vao = VAOUtility.createVTI(
                new float[]{0.0f, 512.0f, 0.0f, 800.0f, 512.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 800.0f, 0.0f, 0.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f},
                new int[]{0, 1, 2, 3, 2, 1}, RenderType.TRIANGLES);
    }

    public ScapesEngine getEngine() {
        return engine;
    }

    public void disposeState() {
        dispose();
        scene.removeAllGui();
        if (fboScene != null) {
            fboScene.dispose(engine.getGraphics());
        }
        if (fboFront != null) {
            fboFront.dispose(engine.getGraphics());
        }
        if (fboBack != null) {
            fboBack.dispose(engine.getGraphics());
        }
        engine.getGraphics().getShaderManager()
                .clearCache(engine.getGraphics());
        engine.getGraphics().getTextureManager()
                .clearCache(engine.getGraphics());
    }

    public abstract void dispose();

    public abstract void init();

    public abstract boolean isMouseGrabbed();

    public abstract boolean isThreaded();

    public void add(Gui gui) {
        if (gui != null) {
            scene.addGui(gui);
        }
    }

    public void remove(Gui gui) {
        if (gui != null) {
            scene.removeGui(gui);
        }
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        if (this.scene == newScene) {
            this.scene = null;
        }
        newScene = scene;
    }

    public void step(Sync sync) {
        if (scene != null) {
            scene.stepGui(engine);
        }
        stepComponent(sync);
    }

    public abstract void stepComponent(Sync sync);

    public void render(GraphicsSystem graphics, boolean updateSize) {
        if (newScene != null) {
            if (scene != null && scene != newScene) {
                scene.dispose(graphics);
            }
            newScene.setState(this);
            scene = newScene;
            newScene = null;
            scene.init(graphics);
            if (fboScene != null) {
                fboScene.dispose(graphics);
                fboScene = null;
            }
            if (fboFront != null) {
                fboFront.dispose(graphics);
                fboFront = null;
            }
            if (fboBack != null) {
                fboBack.dispose(graphics);
                fboBack = null;
            }
        }
        int sceneWidth = scene.getWidth(graphics.getSceneWidth());
        int sceneHeight = scene.getHeight(graphics.getSceneHeight());
        if (fboScene == null) {
            fboScene = new FBO(sceneWidth, sceneHeight,
                    scene.getColorAttachments(), true, true, false, graphics);
            scene.initFBO(0, fboScene);
        }
        if (updateSize) {
            if (fboScene != null) {
                fboScene.setSize(sceneWidth, sceneHeight, graphics);
                scene.initFBO(0, fboScene);
            }
            if (fboFront != null) {
                fboFront.setSize(sceneWidth, sceneHeight, graphics);
                scene.initFBO(1, fboFront);
            }
            if (fboBack != null) {
                fboBack.setSize(sceneWidth, sceneHeight, graphics);
                scene.initFBO(2, fboBack);
            }
        }
        double delta = graphics.getSync().getSpeedFactor();
        OpenGL openGL = graphics.getOpenGL();
        openGL.checkError("Initializing-Scene-Rendering");
        fboScene.activate(graphics);
        openGL.viewport(0, 0, sceneWidth, sceneHeight);
        openGL.clearDepth();
        scene.renderScene(graphics);
        fboScene.deactivate(graphics);
        openGL.checkError("Scene-Rendering");
        graphics.setProjectionOrthogonal(0.0f, 0.0f, 800.0f, 512.0f);
        int renderPasses = scene.getRenderPasses() - 1;
        if (renderPasses == 0) {
            openGL.viewport(0, 0, graphics.getContentWidth(),
                    graphics.getContentHeight());
            renderPostProcess(graphics, fboScene, fboScene, renderPasses);
        } else if (renderPasses == 1) {
            if (fboFront == null) {
                fboFront = new FBO(sceneWidth, sceneHeight,
                        scene.getColorAttachments(), false, true, false,
                        graphics);
                scene.initFBO(1, fboFront);
            }
            fboFront.activate(graphics);
            renderPostProcess(graphics, fboScene, fboScene, 0);
            fboFront.deactivate(graphics);
            openGL.viewport(0, 0, graphics.getContentWidth(),
                    graphics.getContentHeight());
            renderPostProcess(graphics, fboFront, fboScene, renderPasses);
        } else {
            if (fboFront == null) {
                fboFront = new FBO(sceneWidth, sceneHeight,
                        scene.getColorAttachments(), false, true, false,
                        graphics);
                scene.initFBO(1, fboFront);
            }
            if (fboBack == null) {
                fboBack = new FBO(sceneWidth, sceneHeight,
                        scene.getColorAttachments(), false, true, false,
                        graphics);
                scene.initFBO(2, fboBack);
            }
            fboFront.activate(graphics);
            renderPostProcess(graphics, fboScene, fboScene, 0);
            fboFront.deactivate(graphics);
            for (int i = 1; i < renderPasses; i++) {
                fboBack.activate(graphics);
                renderPostProcess(graphics, fboFront, fboScene, i);
                fboBack.deactivate(graphics);
                FBO fboSwap = fboFront;
                fboFront = fboBack;
                fboBack = fboSwap;
            }
            openGL.viewport(0, 0, graphics.getContentWidth(),
                    graphics.getContentHeight());
            renderPostProcess(graphics, fboFront, fboScene, renderPasses);
        }
        openGL.checkError("Post-Processing");
        Shader shader = graphics.getShaderManager()
                .getShader("Engine:shader/Gui", graphics);
        scene.renderGui(graphics, shader, delta);
        engine.getGlobalGui()
                .render(graphics, shader, graphics.getDefaultFont(), delta);
        GuiController guiController = engine.getGuiController();
        if (guiController.isSoftwareMouse() && !isMouseGrabbed()) {
            graphics.setProjectionOrthogonal(0.0f, 0.0f,
                    graphics.getContentWidth(), graphics.getContainerHeight());
            graphics.getTextureManager().bind("Engine:image/Cursor", graphics);
            MatrixStack matrixStack = graphics.getMatrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate((float) guiController.getCursorX(),
                    (float) guiController.getCursorY(), 0.0f);
            CURSOR.render(graphics, shader);
            matrixStack.pop();
        }
        openGL.checkError("Gui-Rendering");
        scene.postRender(graphics, delta);
        openGL.checkError("Post-Render");
    }

    public void renderPostProcess(GraphicsSystem graphics, FBO fbo,
            FBO depthFBO, int i) {
        OpenGL openGL = graphics.getOpenGL();
        openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f);
        TextureFBOColor[] texturesColor = fbo.getTexturesColor();
        for (int j = 1; j < texturesColor.length; j++) {
            openGL.activeTexture(j + 1);
            texturesColor[j].bind(graphics);
        }
        openGL.activeTexture(1);
        depthFBO.getTextureDepth().bind(graphics);
        openGL.activeTexture(0);
        texturesColor[0].bind(graphics);
        vao.render(graphics, scene.postProcessing(graphics, i));
    }

    public FBO getFBOScene() {
        return fboScene;
    }

    public FontRenderer getFont() {
        return font;
    }
}
