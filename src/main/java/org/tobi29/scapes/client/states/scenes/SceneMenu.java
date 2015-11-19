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
import org.tobi29.scapes.engine.opengl.texture.TextureCustom;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.graphics.BlurOffset;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.basic.BasicWorldSource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SceneMenu extends Scene {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SceneMenu.class);
    private static final String BLUR_OFFSET;
    private static final String BLUR_WEIGHT;
    private static final int BLUR_LENGTH;

    static {
        float[] blurOffset = BlurOffset.gaussianBlurOffset(15, 0.04f);
        float[] blurWeight = BlurOffset.gaussianBlurWeight(15,
                sample -> FastMath.cos(sample * FastMath.PI));
        BLUR_LENGTH = blurOffset.length;
        BLUR_OFFSET = ArrayUtil.join(blurOffset);
        BLUR_WEIGHT = ArrayUtil.join(blurWeight);
    }

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

    public void changeBackground(Path path) {
        save = saveBackground(path);
    }

    @Override
    public void init(GL gl) {
        ShaderManager shaderManager = gl.shaders();
        ShaderCompileInformation information =
                shaderManager.compileInformation("Scapes:shader/Menu1");
        information.supplyExternal("BLUR_OFFSET", BLUR_OFFSET);
        information.supplyExternal("BLUR_WEIGHT", BLUR_WEIGHT);
        information.supplyExternal("BLUR_LENGTH", BLUR_LENGTH);
        information = shaderManager.compileInformation("Scapes:shader/Menu2");
        information.supplyExternal("BLUR_OFFSET", BLUR_OFFSET);
        information.supplyExternal("BLUR_WEIGHT", BLUR_WEIGHT);
        information.supplyExternal("BLUR_LENGTH", BLUR_LENGTH);
        vao = VAOUtility.createVTI(
                new float[]{-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, -1.0f, -1.0f},
                new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
                new int[]{0, 1, 2, 2, 1, 3}, RenderType.TRIANGLES);
        loadTextures(gl);
    }

    @Override
    public void renderScene(GL gl) {
        if (save.isPresent()) {
            changeBackground(save.get(), gl);
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
    public void dispose(GL gl) {
        for (int i = 0; i < 6; i++) {
            Texture texture = textures[i];
            if (texture != null) {
                texture.dispose(gl);
            }
        }
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    protected void loadTextures(GL gl) {
        if (texturesLoaded) {
            return;
        }
        texturesLoaded = true;
        List<Path> saves = new ArrayList<>();
        try {
            Path path = state.engine().home().resolve("saves");
            try (DirectoryStream<Path> stream = Files
                    .newDirectoryStream(path)) {
                for (Path file : stream) {
                    if (Files.isDirectory(file) && !Files.isHidden(file)) {
                        saves.add(file);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read saves: {}", e.toString());
        }
        Random random = ThreadLocalRandom.current();
        if (saves.isEmpty()) {
            defaultBackground(gl);
        } else {
            Optional<Image[]> images =
                    saveBackground(saves.get(random.nextInt(saves.size())));
            if (images.isPresent()) {
                changeBackground(images.get(), gl);
            } else {
                defaultBackground(gl);
            }
        }
    }

    protected void setBackground(Texture replace, int i, GL gl) {
        Texture texture = textures[i];
        if (texture != null) {
            texture.dispose(gl);
        }
        textures[i] = replace;
    }

    private Optional<Image[]> saveBackground(Path path) {
        try (WorldSource source = new BasicWorldSource(path)) {
            return source.panorama();
        } catch (IOException e) {
            LOGGER.warn("Failed to load save background", e);
            return Optional.empty();
        }
    }

    private void changeBackground(Image[] images, GL gl) {
        for (int i = 0; i < 6; i++) {
            Image image = images[i];
            setBackground(new TextureCustom(image.width(), image.height(),
                            image.buffer(), 0, TextureFilter.LINEAR,
                            TextureFilter.LINEAR, TextureWrap.CLAMP, TextureWrap.CLAMP),
                    i, gl);
        }
    }

    private void defaultBackground(GL gl) {
        Random random = ThreadLocalRandom.current();
        int r = random.nextInt(2);
        for (int i = 0; i < 6; i++) {
            setBackground(gl.textures().get("Scapes:image/gui/panorama/" +
                    r + "/Panorama" + i), i, gl);
        }
    }
}
