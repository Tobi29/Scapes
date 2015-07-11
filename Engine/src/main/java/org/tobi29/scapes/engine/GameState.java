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
    protected Scene scene, newScene;
    protected FBO fboScene, fboFront, fboBack;

    protected GameState(ScapesEngine engine, Scene scene) {
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

    public void disposeState(GL gl) {
        dispose(gl);
        scene.removeAllGui();
        if (fboScene != null) {
            fboScene.dispose(gl);
        }
        if (fboFront != null) {
            fboFront.dispose(gl);
        }
        if (fboBack != null) {
            fboBack.dispose(gl);
        }
        gl.getShaderManager().clearCache(gl);
        gl.getTextureManager().clearCache(gl);
    }

    public abstract void dispose(GL gl);

    public abstract void init(GL gl);

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

    public void step(double delta) {
        if (scene != null) {
            scene.stepGui(engine);
        }
        stepComponent(delta);
    }

    public abstract void stepComponent(double delta);

    public void render(GL gl, double delta, boolean updateSize) {
        if (newScene != null) {
            if (scene != null && scene != newScene) {
                scene.dispose(gl);
            }
            newScene.setState(this);
            scene = newScene;
            newScene = null;
            scene.init(gl);
            if (fboScene != null) {
                fboScene.dispose(gl);
                fboScene = null;
            }
            if (fboFront != null) {
                fboFront.dispose(gl);
                fboFront = null;
            }
            if (fboBack != null) {
                fboBack.dispose(gl);
                fboBack = null;
            }
        }
        int sceneWidth = scene.getWidth(gl.getSceneWidth());
        int sceneHeight = scene.getHeight(gl.getSceneHeight());
        if (fboScene == null) {
            fboScene = new FBO(sceneWidth, sceneHeight,
                    scene.getColorAttachments(), true, true, false, gl);
            scene.initFBO(0, fboScene);
        }
        if (updateSize) {
            if (fboScene != null) {
                fboScene.setSize(sceneWidth, sceneHeight, gl);
                scene.initFBO(0, fboScene);
            }
            if (fboFront != null) {
                fboFront.setSize(sceneWidth, sceneHeight, gl);
                scene.initFBO(1, fboFront);
            }
            if (fboBack != null) {
                fboBack.setSize(sceneWidth, sceneHeight, gl);
                scene.initFBO(2, fboBack);
            }
        }
        OpenGL openGL = gl.getOpenGL();
        openGL.checkError("Initializing-Scene-Rendering");
        fboScene.activate(gl);
        openGL.viewport(0, 0, sceneWidth, sceneHeight);
        openGL.clearDepth();
        scene.renderScene(gl);
        fboScene.deactivate(gl);
        openGL.checkError("Scene-Rendering");
        gl.setProjectionOrthogonal(0.0f, 0.0f, 800.0f, 512.0f);
        int renderPasses = scene.getRenderPasses() - 1;
        if (renderPasses == 0) {
            openGL.viewport(0, 0, gl.getContentWidth(), gl.getContentHeight());
            renderPostProcess(gl, fboScene, fboScene, renderPasses);
        } else if (renderPasses == 1) {
            if (fboFront == null) {
                fboFront = new FBO(sceneWidth, sceneHeight,
                        scene.getColorAttachments(), false, true, false, gl);
                scene.initFBO(1, fboFront);
            }
            fboFront.activate(gl);
            renderPostProcess(gl, fboScene, fboScene, 0);
            fboFront.deactivate(gl);
            openGL.viewport(0, 0, gl.getContentWidth(), gl.getContentHeight());
            renderPostProcess(gl, fboFront, fboScene, renderPasses);
        } else {
            if (fboFront == null) {
                fboFront = new FBO(sceneWidth, sceneHeight,
                        scene.getColorAttachments(), false, true, false, gl);
                scene.initFBO(1, fboFront);
            }
            if (fboBack == null) {
                fboBack = new FBO(sceneWidth, sceneHeight,
                        scene.getColorAttachments(), false, true, false, gl);
                scene.initFBO(2, fboBack);
            }
            fboFront.activate(gl);
            renderPostProcess(gl, fboScene, fboScene, 0);
            fboFront.deactivate(gl);
            for (int i = 1; i < renderPasses; i++) {
                fboBack.activate(gl);
                renderPostProcess(gl, fboFront, fboScene, i);
                fboBack.deactivate(gl);
                FBO fboSwap = fboFront;
                fboFront = fboBack;
                fboBack = fboSwap;
            }
            openGL.viewport(0, 0, gl.getContentWidth(), gl.getContentHeight());
            renderPostProcess(gl, fboFront, fboScene, renderPasses);
        }
        openGL.checkError("Post-Processing");
        Shader shader =
                gl.getShaderManager().getShader("Engine:shader/Gui", gl);
        scene.renderGui(gl, shader, delta);
        engine.globalGUI().render(gl, shader, gl.getDefaultFont(), delta);
        GuiController guiController = engine.guiController();
        if (guiController.isSoftwareMouse() && !isMouseGrabbed()) {
            gl.setProjectionOrthogonal(0.0f, 0.0f, gl.getContentWidth(),
                    gl.getContainerHeight());
            gl.getTextureManager().bind("Engine:image/Cursor", gl);
            MatrixStack matrixStack = gl.getMatrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate((float) guiController.getCursorX(),
                    (float) guiController.getCursorY(), 0.0f);
            CURSOR.render(gl, shader);
            matrixStack.pop();
        }
        openGL.checkError("Gui-Rendering");
        scene.postRender(gl, delta);
        openGL.checkError("Post-Render");
    }

    public void renderPostProcess(GL gl, FBO fbo, FBO depthFBO, int i) {
        OpenGL openGL = gl.getOpenGL();
        openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f, 1.0f);
        TextureFBOColor[] texturesColor = fbo.getTexturesColor();
        for (int j = 1; j < texturesColor.length; j++) {
            openGL.activeTexture(j + 1);
            texturesColor[j].bind(gl);
        }
        openGL.activeTexture(1);
        depthFBO.getTextureDepth().bind(gl);
        openGL.activeTexture(0);
        texturesColor[0].bind(gl);
        vao.render(gl, scene.postProcessing(gl, i));
    }

    public FBO getFBOScene() {
        return fboScene;
    }
}
