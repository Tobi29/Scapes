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

package org.tobi29.scapes.engine.opengl.scenes;

import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentIcon;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.texture.Texture;

public class SceneImage extends Scene {
    private final Texture image;
    private final double scale;
    private GuiComponentIcon icon;

    public SceneImage(Texture image, double scale) {
        this.image = image;
        this.scale = scale;
    }

    @Override
    public void init(GraphicsSystem graphics) {
        addGui(new GuiImage(image));
    }

    @Override
    public void renderScene(GraphicsSystem graphics) {
    }

    @Override
    public Shader postProcessing(GraphicsSystem graphics, int pass) {
        return graphics.getShaderManager()
                .getShader("Engine:shader/Textured", graphics);
    }

    @Override
    public void dispose(GraphicsSystem graphics) {
    }

    public GuiComponentIcon getImage() {
        return icon;
    }

    private class GuiImage extends Gui {
        public GuiImage(Texture texture) {
            super(GuiAlignment.CENTER);
            int width = texture.getWidth();
            int height = texture.getHeight();
            double ratio = (double) width / height;
            width = (int) (512 * ratio * scale);
            height = (int) (512 * scale);
            icon = new GuiComponentIcon((800 - width) / 2, (512 - height) / 2,
                    width, height, texture);
            add(icon);
        }
    }
}
