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
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.server.format.WorldFormat;

import java.io.IOException;
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
    private Optional<Path[]> save = Optional.empty();

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
            try {
                changeBackground(save.get(), gl);
            } catch (IOException e) {
                LOGGER.warn("Failed to load background", e);
            }
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
        List<Path[]> saves = new ArrayList<>();
        try {
            Path path = state.engine().home().resolve("saves");
            for (Path directory : Files.newDirectoryStream(path)) {
                if (Files.isDirectory(directory) &&
                        !Files.isHidden(directory) &&
                        directory.getFileName().toString()
                                .endsWith(WorldFormat.filenameExtension())) {
                    saveBackground(directory).ifPresent(saves::add);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read saves: {}", e.toString());
        }
        Random random = ThreadLocalRandom.current();
        if (saves.isEmpty()) {
            int r = random.nextInt(2);
            for (int i = 0; i < 6; i++) {
                setBackground(gl.textures().get("Scapes:image/gui/panorama/" +
                        r + "/Panorama" + i), i, gl);
            }
        } else {
            try {
                changeBackground(saves.get(random.nextInt(saves.size())), gl);
            } catch (IOException e) {
                LOGGER.warn("Failed to load save background: {}", e.toString());
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

    private Optional<Path[]> saveBackground(Path path) {
        Path[] save = new Path[6];
        for (int i = 0; i < 6; i++) {
            Path background = path.resolve("Panorama" + i + ".png");
            if (Files.exists(background)) {
                save[i] = background;
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(save);
    }

    private void changeBackground(Path[] save, GL gl) throws IOException {
        for (int i = 0; i < 6; i++) {
            setBackground(FileUtil.readReturn(save[i],
                    input -> new TextureFile(input, 0, TextureFilter.LINEAR,
                            TextureFilter.LINEAR, TextureWrap.CLAMP,
                            TextureWrap.CLAMP)), i, gl);
        }
    }
}
