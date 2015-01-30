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

import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;

import java.io.IOException;
import java.io.InputStream;

public class TextureFile extends Texture {
    public TextureFile(InputStream streamIn) throws IOException {
        this(streamIn, 4);
    }

    public TextureFile(InputStream streamIn, int mipmaps) throws IOException {
        this(streamIn, mipmaps, TextureFilter.NEAREST, TextureFilter.NEAREST,
                TextureWrap.REPEAT, TextureWrap.REPEAT);
    }

    public TextureFile(InputStream streamIn, int mipmaps,
            TextureFilter minFilter, TextureFilter magFilter, TextureWrap wrapS,
            TextureWrap wrapT) throws IOException {
        this(PNG.decode(streamIn, BufferCreatorDirect::byteBuffer), mipmaps,
                minFilter, magFilter, wrapS, wrapT);
        streamIn.close();
    }

    public TextureFile(Image image, int mipmaps, TextureFilter minFilter,
            TextureFilter magFilter, TextureWrap wrapS, TextureWrap wrapT) {
        super(image.getWidth(), image.getHeight(), image.getBuffer(), mipmaps,
                minFilter, magFilter, wrapS, wrapT);
    }
}
