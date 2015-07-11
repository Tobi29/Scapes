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

public class TextureFBOColor extends Texture {
    private final boolean alpha, hdr;

    public TextureFBOColor(int width, int height, TextureFilter minFilter,
            TextureFilter magFilter, TextureWrap wrapS, TextureWrap wrapT,
            boolean alpha, boolean hdr) {
        super(width, height, null, 0, minFilter, magFilter, wrapS, wrapT);
        this.alpha = alpha;
        this.hdr = hdr;
    }

    @Override
    protected void store(GL gl) {
        OpenGL openGL = gl.getOpenGL();
        textureID = openGL.createTexture();
        openGL.bindTexture(textureID);
        if (hdr) {
            openGL.bufferTextureFloat(width, height, alpha, null);
        } else {
            openGL.bufferTexture(width, height, alpha, null);
        }
        dirtyFilter = true;
        TEXTURES.add(this);
    }
}
