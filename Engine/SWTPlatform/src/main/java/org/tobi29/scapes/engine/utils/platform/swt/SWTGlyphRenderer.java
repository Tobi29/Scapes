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

package org.tobi29.scapes.engine.utils.platform.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.MutableSingle;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.ui.font.GlyphRenderer;

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
    private Font font;
    private Image image;

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
    public synchronized GlyphPage getPage(int id) {
        float[] width = new float[pageTiles];
        MutableSingle<ImageData> output = new MutableSingle<>(null);
        Display display = Display.getDefault();
        display.syncExec(() -> {
            if (font == null) {
                double fontSize;
                if ("gtk".equals(SWT.getPlatform())) {
                    fontSize = size * 0.7; // Note to self: SWT is NOT platform independent by any means
                } else {
                    fontSize = size;
                }
                font = new Font(display, fontName, FastMath.round(fontSize),
                        SWT.NONE);
            }
            if (image == null) {
                ImageData imageData =
                        new ImageData(imageSize, imageSize, 24, PALETTE_DATA);
                image = new Image(display, imageData);
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
            output.a = image.getImageData();
        });
        output.a.getPixels(0, 0, imageSize * imageSize, color, 0);
        ByteBuffer buffer =
                BufferCreatorDirect.byteBuffer(imageSize * imageSize << 2);
        int i = 0;
        while (buffer.hasRemaining()) {
            buffer.put(WHITE);
            buffer.put((byte) (color[i] & 0xFF));
            i++;
        }
        buffer.rewind();
        return new GlyphPage(buffer, width, imageSize, tiles, tileSize);
    }

    @Override
    public int getPageID(char character) {
        return character >> pageTileBits;
    }

    @Override
    public int getPageLetter(char character) {
        return character & pageTileMask;
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.dispose();
        }
        if (image != null) {
            image.dispose();
        }
    }
}
