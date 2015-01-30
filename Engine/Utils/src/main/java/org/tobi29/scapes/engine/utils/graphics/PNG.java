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

package org.tobi29.scapes.engine.utils.graphics;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import de.matthiasmann.twl.utils.PNGDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class PNG {
    private PNG() {
    }

    public static Image decode(InputStream streamIn, BufferSupplier supplier)
            throws IOException {
        PNGDecoder decoder = new PNGDecoder(streamIn);
        int width = decoder.getWidth();
        int height = decoder.getHeight();
        ByteBuffer buffer = supplier.bytes(width * height << 2);
        decoder.decode(buffer, width << 2, PNGDecoder.Format.RGBA);
        buffer.rewind();
        return new Image(width, height, buffer);
    }

    public static void encode(Image image, OutputStream streamOut, int level,
            boolean alpha) throws IOException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            ByteBuffer buffer = image.getBuffer();
            ImageInfo info = new ImageInfo(width, height, 8, alpha);
            PngWriter writer = new PngWriter(streamOut, info);
            writer.setCompLevel(level);
            ImageLineByte line = new ImageLineByte(info);
            byte[] scanline = line.getScanline();
            if (alpha) {
                for (int y = height - 1; y >= 0; y--) {
                    buffer.position(y * height << 2);
                    int x = 0;
                    while (x < scanline.length) {
                        scanline[x++] = buffer.get();
                        scanline[x++] = buffer.get();
                        scanline[x++] = buffer.get();
                        scanline[x++] = buffer.get();
                    }
                    writer.writeRow(line);
                }
            } else {
                for (int y = height - 1; y >= 0; y--) {
                    buffer.position(y * width << 2);
                    int x = 0;
                    while (x < scanline.length) {
                        scanline[x++] = buffer.get();
                        scanline[x++] = buffer.get();
                        scanline[x++] = buffer.get();
                        buffer.get();
                    }
                    writer.writeRow(line);
                }
            }
            writer.end();
        } catch (PngjException e) {
            throw new IOException(e);
        }
    }

    @FunctionalInterface
    public interface BufferSupplier {
        ByteBuffer bytes(int size);
    }
}
