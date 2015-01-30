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
import java.util.Properties;

public class TextureAsset extends Texture {
    public TextureAsset(InputStream streamIn, Properties properties)
            throws IOException {
        this(PNG.decode(streamIn, BufferCreatorDirect::byteBuffer), properties);
        streamIn.close();
    }

    public TextureAsset(Image image, Properties properties) {
        super(image.getWidth(), image.getHeight(), image.getBuffer(),
                Integer.valueOf(properties.getProperty("Mipmaps", "4")),
                TextureFilter
                        .get(properties.getProperty("MinFilter", "Nearest")),
                TextureFilter
                        .get(properties.getProperty("MagFilter", "Nearest")),
                TextureWrap.get(properties.getProperty("WrapS", "Repeat")),
                TextureWrap.get(properties.getProperty("WrapT", "Repeat")));
    }
}
