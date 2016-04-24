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
import java8.util.stream.Collectors;
import org.tobi29.scapes.client.SaveStorage;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.scenes.Scene;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.shader.ShaderCompileInformation;
import org.tobi29.scapes.engine.opengl.shader.ShaderManager;
import org.tobi29.scapes.engine.opengl.shader.ShaderPreprocessor;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.opengl.texture.TextureCustom;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.graphics.BlurOffset;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SceneMenu extends Scene {
    private final Texture[] textures = new Texture[6];
    private final Cam cam;
    private float speed = 0.6f, yaw;
    private VAO vao;
    private Optional<Image[]> save = Optional.empty();
    private boolean texturesLoaded;

    public SceneMenu() {
        cam = new Cam(0.4f, 2.0f);
        Random random = ThreadLocalRandom.current();
        yaw = random.nextFloat() * 360.0f;
    }

    private static void blur(GL gl, ShaderPreprocessor shader) {
        double space = gl.sceneSpace();
        int samples = FastMath.round(space * 8.0) + 8;
        float[] blurOffsets = BlurOffset.gaussianBlurOffset(samples, 0.04f);
        float[] blurWeights = BlurOffset.gaussianBlurWeight(samples,
                sample -> FastMath.cos(sample * FastMath.PI));
        int blurLength = blurOffsets.length;
        String blurOffset = ArrayUtil.join(blurOffsets);
        String blurWeight = ArrayUtil.join(blurWeights);
        shader.supplyProperty("BLUR_OFFSET", blurOffset);
        shader.supplyProperty("BLUR_WEIGHT", blurWeight);
        shader.supplyProperty("BLUR_LENGTH", blurLength);
    }

    public void changeBackground(WorldSource source) throws IOException {
        save = saveBackground(source);
    }

    @Override
    public void init(GL gl) {
        ShaderManager shaderManager = gl.shaders();
        ShaderCompileInformation menu1 =
                shaderManager.compileInformation("Scapes:shader/Menu1");
        menu1.supplyPreCompile("Blur", shader -> blur(gl, shader));
        ShaderCompileInformation menu2 =
                shaderManager.compileInformation("Scapes:shader/Menu2");
        menu2.supplyPreCompile("Blur", shader -> blur(gl, shader));
        vao = VAOUtility.createVTI(state.engine(),
                new float[]{-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, -1.0f, -1.0f},
                new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
                new int[]{0, 1, 2, 2, 1, 3}, RenderType.TRIANGLES);
        loadTextures(gl);
    }

    @Override
    public void renderScene(GL gl) {
        if (save.isPresent()) {
            changeBackground(save.get());
            save = Optional.empty();
        }
        cam.setPerspective((float) gl.sceneWidth() / gl.sceneHeight(), 90.0f);
        cam.setView(0.0f, yaw, 0.0f);
        gl.setProjectionPerspective(gl.sceneWidth(), gl.sceneHeight(), cam);
        Shader shader = gl.shaders().get("Engine:shader/Textured", gl);
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < 6; i++) {
            Texture texture = textures[i];
            if (texture != null) {
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
                texture.bind(gl);
                gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 1.0f,
                        1.0f);
                vao.render(gl, shader);
                matrixStack.pop();
            }
        }
    }

    @Override
    public void postRender(GL gl, double delta) {
        yaw -= speed * delta;
        yaw %= 360;
    }

    @Override
    public Shader postProcessing(GL gl, int pass) {
        return gl.shaders().get("Scapes:shader/Menu" + (pass + 1), gl);
    }

    @Override
    public int renderPasses() {
        return 2;
    }

    @Override
    public void dispose() {
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    protected void loadTextures(GL gl) {
        if (texturesLoaded) {
            return;
        }
        texturesLoaded = true;
        ScapesClient game = (ScapesClient) state.engine().game();
        SaveStorage saves = game.saves();
        Random random = ThreadLocalRandom.current();
        try {
            List<String> list = saves.list().collect(Collectors.toList());
            if (list.isEmpty()) {
                throw new IOException("No save available");
            }
            try (WorldSource source = saves
                    .get(list.get(random.nextInt(list.size())))) {
                Optional<Image[]> images = source.panorama();
                if (images.isPresent()) {
                    changeBackground(images.get());
                } else {
                    defaultBackground();
                }
            }
        } catch (IOException e) {
            defaultBackground();
        }
    }

    protected void setBackground(Texture replace, int i) {
        Texture texture = textures[i];
        if (texture != null) {
            texture.markDisposed();
        }
        textures[i] = replace;
    }

    private Optional<Image[]> saveBackground(WorldSource source)
            throws IOException {
        return source.panorama();
    }

    private void changeBackground(Image[] images) {
        for (int i = 0; i < 6; i++) {
            Image image = images[i];
            setBackground(new TextureCustom(state.engine(), image.width(),
                            image.height(), image.buffer(), 0, TextureFilter.LINEAR,
                            TextureFilter.LINEAR, TextureWrap.CLAMP, TextureWrap.CLAMP),
                    i);
        }
    }

    private void defaultBackground() {
        Random random = ThreadLocalRandom.current();
        int r = random.nextInt(2);
        for (int i = 0; i < 6; i++) {
            setBackground(state.engine().graphics().textures()
                    .get("Scapes:image/gui/panorama/" +
                            r + "/Panorama" + i), i);
        }
    }
}
