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

package org.tobi29.scapes.engine.backends.lwjgl3.glfw.dialogs.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.tobi29.scapes.engine.gui.GlyphRenderer;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.MutableSingle;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.nio.ByteBuffer;

public class SWTGlyphRenderer implements GlyphRenderer {
    private static final PaletteData PALETTE_DATA =
            new PaletteData(0xFF0000, 0xFF00, 0xFF);
    private static final byte[] WHITE = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private final String fontName;
    private final int tiles, pageTiles, pageTileBits, pageTileMask, size,
            glyphSize, imageSize, renderX, renderY;
    private final float tileSize;
    private final int[] color;
    private Image image;
    private Font font;

    public SWTGlyphRenderer(String fontName, int size) {
        this.fontName = fontName;
        this.size = size;
        int tileBits = 3;
        tiles = 1 << tileBits;
        pageTileBits = tileBits << 1;
        pageTileMask = (1 << pageTileBits) - 1;
        pageTiles = 1 << pageTileBits;
        tileSize = 1.0f / tiles;
        glyphSize = size << 1;
        imageSize = glyphSize << tileBits;
        renderX = size / 2;
        renderY = FastMath.round(size * 0.375);
        color = new int[imageSize * imageSize];
    }

    @SuppressWarnings("AccessToStaticFieldLockedOnInstance")
    @Override
    public synchronized GlyphPage page(int id) {
        float[] width = new float[pageTiles];
        MutableSingle<ByteBuffer> output = new MutableSingle<>(null);
        Display display = Display.getDefault();
        display.syncExec(() -> {
            if (image == null) {
                ImageData imageData =
                        new ImageData(imageSize, imageSize, 24, PALETTE_DATA);
                image = new Image(display, imageData);
            }
            if (font == null) {
                font = new Font(display, fontName, size, SWT.NONE);
                GC gc = new GC(image);
                gc.setFont(font);
                int height = FastMath.round(gc.stringExtent("").y * 0.85);
                if (height != size) {
                    double scale = (double) size / height;
                    font.dispose();
                    font = new Font(display, fontName,
                            FastMath.round(size * scale), SWT.NONE);
                }
                gc.dispose();
            }
            GC gc = new GC(image);
            gc.setFont(font);
            gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
            gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.fillRectangle(0, 0, imageSize, imageSize);
            int i = 0;
            int offset = id << pageTileBits;
            for (int y = 0; y < tiles; y++) {
                int yy = y * glyphSize + renderY;
                for (int x = 0; x < tiles; x++) {
                    int xx = x * glyphSize + renderX;
                    char c = (char) (i + offset);
                    String str = new String(new char[]{c});
                    gc.drawString(str, xx, yy, true);
                    width[i++] = (float) gc.stringExtent(str).x / size;
                }
            }
            gc.dispose();
            image.getImageData()
                    .getPixels(0, 0, imageSize * imageSize, color, 0);
            ByteBuffer buffer = BufferCreator.bytes(imageSize * imageSize << 2);
            i = 0;
            while (buffer.hasRemaining()) {
                buffer.put(WHITE);
                buffer.put((byte) (color[i] & 0xFF));
                i++;
            }
            buffer.rewind();
            output.a = buffer;
        });
        return new GlyphPage(output.a, width, imageSize, tiles, tileSize);
    }

    @Override
    public int pageID(char character) {
        return character >> pageTileBits;
    }

    @Override
    public int pageCode(char character) {
        return character & pageTileMask;
    }

    @Override
    public void dispose() {
        if (image != null) {
            image.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
