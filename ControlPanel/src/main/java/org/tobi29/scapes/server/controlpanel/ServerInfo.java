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

package org.tobi29.scapes.server.controlpanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.CompressionUtil;
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ServerInfo {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServerInfo.class);
    private final String name;
    private final Image image;
    private final ByteBuffer buffer;

    public ServerInfo(String name, File icon) {
        this.name = name;
        Image image;
        if (icon.exists()) {
            try (InputStream streamIn = icon.read()) {
                image = PNG.decode(streamIn, BufferCreator::byteBuffer);
                int width = image.getWidth();
                if (width != image.getHeight()) {
                    throw new IOException("The icon has to be square sized.");
                } else if (width > 256) {
                    throw new IOException(
                            "The icon may not be larger than 256x256.");
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to load server icon: {}", e.toString());
                image = new Image();
            }
        } else {
            image = new Image();
        }
        this.image = image;
        ByteBuffer buffer;
        try {
            buffer = CompressionUtil.compress(image.getBuffer());
        } catch (IOException e) {
            throw new UnsupportedJVMException("Failed to compress server icon",
                    e);
        }
        byte[] array = name.getBytes(StandardCharsets.UTF_8);
        int size = 1 + array.length + buffer.remaining();
        this.buffer = BufferCreator.byteBuffer(4 + size);
        this.buffer.putInt(size);
        this.buffer.put((byte) array.length);
        this.buffer.put(array);
        this.buffer.put(buffer);
        this.buffer.rewind();
    }

    public ServerInfo(ByteBuffer buffer) {
        this.buffer = buffer;
        buffer.rewind();
        Image image;
        if (buffer.getInt() == buffer.remaining()) {
            byte[] array = new byte[buffer.get()];
            buffer.get(array);
            name = new String(array, StandardCharsets.UTF_8);
            try {
                ByteBuffer imageBuffer = CompressionUtil.decompress(buffer);
                int size = (int) FastMath.sqrt(imageBuffer.remaining() >> 2);
                image = new Image(size, size, imageBuffer);
            } catch (IOException e) {
                LOGGER.warn("Failed to decompress server icon: {}",
                        e.toString());
                image = new Image();
            }
        } else {
            name = "Invalid server info";
            image = new Image();
        }
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public Image getImage() {
        return image;
    }

    public ByteBuffer getBuffer() {
        return buffer.asReadOnlyBuffer();
    }
}
