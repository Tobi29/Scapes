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

package org.tobi29.scapes.engine.opengl.texture;

import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.OpenGLFunction;
import org.tobi29.scapes.engine.utils.graphics.MipMapGenerator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Texture {
    protected static final List<Texture> TEXTURES = new ArrayList<>();
    protected static final Queue<Texture> DISPOSE_TEXTURES =
            new ConcurrentLinkedQueue<>();
    protected final int mipmaps;
    protected ByteBuffer buffer;
    protected boolean dirtyFilter = true;
    protected int textureID = -1, width = -1, height = -1;
    protected TextureFilter minFilter = TextureFilter.NEAREST, magFilter =
            TextureFilter.NEAREST;
    protected TextureWrap wrapS = TextureWrap.REPEAT, wrapT =
            TextureWrap.REPEAT;

    protected Texture(int width, int height, ByteBuffer buffer, int mipmaps,
            TextureFilter minFilter, TextureFilter magFilter, TextureWrap wrapS,
            TextureWrap wrapT) {
        this.width = width;
        this.height = height;
        this.buffer = buffer;
        this.mipmaps = mipmaps;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.wrapS = wrapS;
        this.wrapT = wrapT;
    }

    @OpenGLFunction
    public static void disposeUnused(GL gl) {
        while (!DISPOSE_TEXTURES.isEmpty()) {
            DISPOSE_TEXTURES.poll().dispose(gl);
        }
    }

    @OpenGLFunction
    public static void disposeAll(GL gl) {
        while (!TEXTURES.isEmpty()) {
            TEXTURES.get(0).dispose(gl);
        }
    }

    public static int getTextureCount() {
        return TEXTURES.size();
    }

    @OpenGLFunction
    public void bind(GL gl) {
        if (textureID == -1) {
            store(gl);
        }
        OpenGL openGL = gl.getOpenGL();
        openGL.bindTexture(textureID);
        if (dirtyFilter) {
            openGL.minFilter(minFilter, mipmaps > 0);
            openGL.magFilter(magFilter);
            openGL.wrapS(wrapS);
            openGL.wrapT(wrapT);
            dirtyFilter = false;
        }
    }

    protected void store(GL gl) {
        OpenGL openGL = gl.getOpenGL();
        buffer.rewind();
        textureID = openGL.createTexture();
        openGL.bindTexture(textureID);
        if (mipmaps > 0) {
            openGL.bufferTextureMipMap(width, height, MipMapGenerator
                    .generateMipMaps(buffer, width, height, mipmaps,
                            minFilter == TextureFilter.LINEAR));
        } else {
            openGL.bufferTexture(width, height, true, buffer);
        }
        dirtyFilter = true;
        TEXTURES.add(this);
    }

    @OpenGLFunction
    public void dispose(GL gl) {
        OpenGL openGL = gl.getOpenGL();
        if (textureID != -1) {
            openGL.deleteTexture(textureID);
            textureID = -1;
        }
        TEXTURES.remove(this);
    }

    public void markDisposed() {
        DISPOSE_TEXTURES.add(this);
    }

    public TextureFilter getFilterMag() {
        return magFilter;
    }

    public TextureFilter getFilterMin() {
        return minFilter;
    }

    public void setFilter(TextureFilter magFilter, TextureFilter minFilter) {
        this.magFilter = magFilter;
        this.minFilter = minFilter;
        dirtyFilter = true;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public TextureWrap getWrapS() {
        return wrapS;
    }

    public TextureWrap getWrapT() {
        return wrapT;
    }

    public void setWrap(TextureWrap wrapS, TextureWrap wrapT) {
        this.wrapS = wrapS;
        this.wrapT = wrapT;
        dirtyFilter = true;
    }

    public int getTextureID() {
        return textureID;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
