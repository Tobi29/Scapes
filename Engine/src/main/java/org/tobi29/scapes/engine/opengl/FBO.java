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

import org.tobi29.scapes.engine.opengl.texture.TextureFBOColor;
import org.tobi29.scapes.engine.opengl.texture.TextureFBODepth;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.ArrayList;
import java.util.List;

public class FBO {
    private static final List<FBO> FBOS = new ArrayList<>();
    private static int currentBO;
    private final TextureFBOColor[] texturesColor;
    private final boolean depth, hdr, alpha;
    private int framebufferID;
    private int width;
    private int height;
    private int lastFBO;
    private TextureFBODepth textureDepth;

    public FBO(int width, int height, int colorAttachments, boolean depth,
            boolean hdr, boolean alpha, GraphicsSystem graphics) {
        this.width = FastMath.max(width, 1);
        this.height = FastMath.max(height, 1);
        texturesColor = new TextureFBOColor[colorAttachments];
        this.depth = depth;
        this.hdr = hdr;
        this.alpha = alpha;
        init(graphics);
    }

    @OpenGLFunction
    public static void disposeAll(GraphicsSystem graphics) {
        while (!FBOS.isEmpty()) {
            FBOS.get(0).dispose(graphics);
        }
    }

    @OpenGLFunction
    private void init(GraphicsSystem graphics) {
        for (int i = 0; i < texturesColor.length; i++) {
            texturesColor[i] =
                    new TextureFBOColor(width, height, TextureFilter.LINEAR,
                            TextureFilter.LINEAR, TextureWrap.CLAMP,
                            TextureWrap.CLAMP, alpha, hdr);
            texturesColor[i].bind(graphics);
        }
        if (depth) {
            textureDepth =
                    new TextureFBODepth(width, height, TextureFilter.LINEAR,
                            TextureFilter.LINEAR, TextureWrap.CLAMP,
                            TextureWrap.CLAMP);
            textureDepth.bind(graphics);
        }
        OpenGL openGL = graphics.getOpenGL();
        framebufferID = openGL.createFBO();
        activate(graphics);
        if (depth) {
            openGL.attachDepth(textureDepth.getTextureID());
        }
        for (int i = 0; i < texturesColor.length; i++) {
            openGL.attachColor(texturesColor[i].getTextureID(), i);
        }
        openGL.clear(0.0f, 0.0f, 0.0f, 0.0f);
        deactivate(graphics);
        FBOS.add(this);
    }

    @OpenGLFunction
    public void deactivate(GraphicsSystem graphics) {
        OpenGL openGL = graphics.getOpenGL();
        openGL.bindFBO(lastFBO);
        currentBO = lastFBO;
        lastFBO = 0;
    }

    @OpenGLFunction
    public void activate(GraphicsSystem graphics) {
        if (framebufferID == -1) {
            init(graphics);
        }
        lastFBO = currentBO;
        currentBO = framebufferID;
        OpenGL openGL = graphics.getOpenGL();
        openGL.bindFBO(framebufferID);
        openGL.drawbuffersFbo(texturesColor.length);
    }

    @OpenGLFunction
    public void setSize(int width, int height, GraphicsSystem graphics) {
        dispose(graphics);
        this.width = FastMath.max(width, 1);
        this.height = FastMath.max(height, 1);
        init(graphics);
    }

    @OpenGLFunction
    public void dispose(GraphicsSystem graphics) {
        if (framebufferID != -1) {
            OpenGL openGL = graphics.getOpenGL();
            openGL.deleteFBO(framebufferID);
            for (TextureFBOColor textureColor : texturesColor) {
                textureColor.dispose(graphics);
            }
            if (depth) {
                textureDepth.dispose(graphics);
            }
            framebufferID = -1;
        }
        FBOS.remove(this);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TextureFBOColor[] getTexturesColor() {
        return texturesColor;
    }

    public TextureFBODepth getTextureDepth() {
        if (!depth) {
            throw new IllegalStateException("FBO has no depth buffer");
        }
        return textureDepth;
    }
}
