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
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureManager;

public class GraphicsSystem {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GraphicsSystem.class);
    private final ScapesEngine engine;
    private final GuiWidgetDebugValues.Element fpsDebug, widthDebug,
            heightDebug, textureDebug, vaoDebug;
    private final OpenGL openGL;
    private final GL gl;
    private boolean triggerScreenshot;
    private double resolutionMultiplier = 1.0;

    public GraphicsSystem(ScapesEngine engine, OpenGL openGL) {
        this.engine = engine;
        this.openGL = openGL;
        resolutionMultiplier = engine.config().getResolutionMultiplier();
        GuiWidgetDebugValues debugValues = engine.debugValues();
        fpsDebug = debugValues.get("Graphics-Fps");
        widthDebug = debugValues.get("Graphics-Width");
        heightDebug = debugValues.get("Graphics-Height");
        textureDebug = debugValues.get("Graphics-Textures");
        vaoDebug = debugValues.get("Graphics-VAOs");
        gl = new GL(engine, openGL);
    }

    public void dispose() {
        GameState state = engine.state();
        state.getScene().dispose(gl);
        state.dispose(gl);
        gl.dispose();
    }

    public ScapesEngine getEngine() {
        return engine;
    }

    public TextureManager getTextureManager() {
        return gl.getTextureManager();
    }

    public ShaderManager getShaderManager() {
        return gl.getShaderManager();
    }

    public void render(double delta) {
        try {
            openGL.checkError("Pre-Render");
            Container container = engine.container();
            int containerWidth = container.getContainerWidth();
            int containerHeight = container.getContainerHeight();
            boolean fboSizeDirty;
            if (container.contentResized() || resolutionMultiplier !=
                    engine.config().getResolutionMultiplier()) {
                resolutionMultiplier =
                        engine.config().getResolutionMultiplier();
                int contentWidth = container.getContentWidth();
                int contentHeight = container.getContentHeight();
                fboSizeDirty = true;
                widthDebug.setValue(contentWidth);
                heightDebug.setValue(contentHeight);
                gl.reshape(contentWidth, contentHeight, containerWidth,
                        containerHeight, resolutionMultiplier);
            } else {
                fboSizeDirty = false;
            }
            engine.step(gl, delta);
            GameState state = engine.state();
            state.render(gl, delta, fboSizeDirty);
            fpsDebug.setValue(1.0 / delta);
            textureDebug.setValue(Texture.getTextureCount());
            vaoDebug.setValue(VAO.getVAOCount());
            if (triggerScreenshot) {
                triggerScreenshot = false;
                openGL.screenShot(engine.home().resolve("screenshots/" +
                        System.currentTimeMillis() +
                        ".png"), gl);
            }
            VAO.disposeUnused(gl);
            Texture.disposeUnused(gl);
        } catch (GraphicsException e) {
            LOGGER.warn("Graphics error during rendering: {}", e.toString());
        }
    }

    public void triggerScreenshot() {
        triggerScreenshot = true;
    }

    @OpenGLFunction
    public void reset() {
        gl.reset();
    }
}
