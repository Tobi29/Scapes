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
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.shader.ShaderCompileInformation;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureFile;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.graphics.BlurOffset;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SceneMenu extends Scene {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SceneMenu.class);
    private static final String BLUR_OFFSET;
    private static final String BLUR_WEIGHT;
    private static final int BLUR_LENGTH;
    private final Cam cam;
    private float speed = 0.6f, yaw;
    private Directory background;
    private Texture[] textures;
    private VAO vao;

    static {
        float[] blurOffset = BlurOffset.gaussianBlurOffset(15, 0.04f);
        float[] blurWeight = BlurOffset.gaussianBlurWeight(15,
                sample -> FastMath.cos(sample * FastMath.PI));
        BLUR_LENGTH = blurOffset.length;
        BLUR_OFFSET = ArrayUtil.join(blurOffset);
        BLUR_WEIGHT = ArrayUtil.join(blurWeight);
    }

    public SceneMenu() {
        cam = new Cam(0.4f, 2.0f);
        Random random = ThreadLocalRandom.current();
        yaw = random.nextFloat() * 360.0f;
    }

    public void changeBackground(Directory file) throws IOException {
        if (!Objects.equals(background, file)) {
            background = file;
            for (int i = 0; i < 6; i++) {
                textures[i].dispose(state.getEngine().getGraphics());
                textures[i] = new TextureFile(
                        background.getResource("Panorama" + i + ".png").read(),
                        0, TextureFilter.LINEAR, TextureFilter.LINEAR,
                        TextureWrap.CLAMP, TextureWrap.CLAMP);
            }
        }
    }

    @Override
    public void init(GraphicsSystem graphics) {
        ShaderManager shaderManager = graphics.getShaderManager();
        ShaderCompileInformation information =
                shaderManager.getCompileInformation("Scapes:shader/Menu1");
        information.supplyExternal("BLUR_OFFSET", BLUR_OFFSET);
        information.supplyExternal("BLUR_WEIGHT", BLUR_WEIGHT);
        information.supplyExternal("BLUR_LENGTH", BLUR_LENGTH);
        information =
                shaderManager.getCompileInformation("Scapes:shader/Menu2");
        information.supplyExternal("BLUR_OFFSET", BLUR_OFFSET);
        information.supplyExternal("BLUR_WEIGHT", BLUR_WEIGHT);
        information.supplyExternal("BLUR_LENGTH", BLUR_LENGTH);
        vao = VAOUtility.createVTI(
                new float[]{-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, -1.0f, -1.0f},
                new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
                new int[]{0, 1, 2, 2, 1, 3}, RenderType.TRIANGLES);
        if (textures == null) {
            textures = loadTextures(graphics);
        }
    }

    @Override
    public void renderScene(GraphicsSystem graphics) {
        cam.setPerspective(
                (float) graphics.getSceneWidth() / graphics.getSceneHeight(),
                90.0f);
        cam.setView(0.0f, yaw, 0.0f);
        graphics.setProjectionPerspective(graphics.getSceneWidth(),
                graphics.getSceneHeight(), cam);
        Shader shader = graphics.getShaderManager()
                .getShader("Engine:shader/Textured", graphics);
        OpenGL openGL = graphics.getOpenGL();
        MatrixStack matrixStack = graphics.getMatrixStack();
        for (int i = 0; i < 6; i++) {
            Matrix matrix = matrixStack.push();
            if (i == 1) {
                matrix.rotate(90.0f, 0.0f, 0.0f, 1.0f);
            } else if (i == 2) {
                matrix.rotate(180.0f, 0.0f, 0.0f, 1.0f);
            } else if (i == 3) {
                matrix.rotate(270.0f, 0.0f, 0.0f, 1.0f);
            } else if (i == 4) {
                matrix.rotate(90.0f, 1.0f, 0.0f, 0.0f);
            } else if (i == 5) {
                matrix.rotate(-90.0f, 1.0f, 0.0f, 0.0f);
            }
            textures[i].bind(graphics);
            openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                    1.0f);
            vao.render(graphics, shader);
            matrixStack.pop();
        }
    }

    @Override
    public void postRender(GraphicsSystem graphics, double delta) {
        yaw -= speed * delta;
        yaw %= 360;
    }

    @Override
    public Shader postProcessing(GraphicsSystem graphics, int pass) {
        return graphics.getShaderManager()
                .getShader("Scapes:shader/Menu" + (pass + 1), graphics);
    }

    @Override
    public int getRenderPasses() {
        return 2;
    }

    @Override
    public void dispose(GraphicsSystem graphics) {
        for (int i = 0; i < 6; i++) {
            textures[i].dispose(graphics);
        }
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    protected Texture[] loadTextures(GraphicsSystem graphics) {
        Texture[] textures = new Texture[6];
        Random random = ThreadLocalRandom.current();
        int r = random.nextInt(2);
        for (int i = 0; i < 6; i++) {
            textures[i] = graphics.getTextureManager()
                    .getTexture("Scapes:image/gui/panorama/" +
                            r + "/Panorama" + i);
        }
        Directory world = null;
        try {
            Directory directory =
                    state.getEngine().getFiles().getDirectory("File:saves");
            List<Directory> saves = directory
                    .listDirectories(dir -> dir.getName().endsWith(".spkg"));
            if (!saves.isEmpty()) {
                world = saves.get(random.nextInt(saves.size()));
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read saves: {}", e.toString());
        }
        if (world != null) {
            background = world;
            for (int i = 0; i < 6; i++) {
                try {
                    textures[i] = new TextureFile(
                            background.getResource("Panorama" + i + ".png")
                                    .read(), 0, TextureFilter.LINEAR,
                            TextureFilter.LINEAR, TextureWrap.CLAMP,
                            TextureWrap.CLAMP);
                } catch (IOException e) {
                }
            }
        }
        return textures;
    }
}
