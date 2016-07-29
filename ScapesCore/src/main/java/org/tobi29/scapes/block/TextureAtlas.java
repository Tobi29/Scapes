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
package org.tobi29.scapes.block;

import java8.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.graphics.Texture;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TextureAtlas<T extends TextureAtlasEntry<?>> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TextureAtlas.class);
    protected final ScapesEngine engine;
    protected final Map<String, T> textures = new ConcurrentHashMap<>();
    protected final Map<String, Image> sources = new ConcurrentHashMap<>();
    protected final int minBits, minSize;
    protected Texture texture;

    protected TextureAtlas(ScapesEngine engine, int minBits) {
        this.engine = engine;
        this.minBits = 4;
        minSize = 1 << minBits;
    }

    protected String path(String[] paths) {
        StringBuilder pathBuilder = new StringBuilder(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            pathBuilder.append('\n').append(paths[i]);
        }
        return pathBuilder.toString();
    }

    protected Image load(String path) {
        return load(new String[]{path});
    }

    protected Image load(String[] paths) {
        ByteBuffer buffer;
        int width, height;
        try {
            Image source = sources.get(paths[0]);
            if (source == null) {
                source = engine.files().get(paths[0]).readReturn(
                        streamIn -> PNG.decode(streamIn, BufferCreator::bytes));
                sources.put(paths[0], source);
            }
            width = source.width();
            height = source.height();
            if (paths.length > 1) {
                buffer = BufferCreator.bytes(width * height << 2);
                buffer.put(source.buffer());
                buffer.rewind();
                source.buffer().rewind();
                for (int i = 1; i < paths.length; i++) {
                    Image layer = sources.get(paths[i]);
                    if (layer == null) {
                        layer = engine.files().get(paths[i]).readReturn(
                                streamIn -> PNG.decode(streamIn,
                                        BufferCreator::bytes));
                        sources.put(paths[i], layer);
                    }
                    if (layer.width() != source.width() ||
                            layer.height() != source.height()) {
                        LOGGER.warn("Invalid size for layered texture from: {}",
                                paths[i]);
                        continue;
                    }
                    int bufferR, bufferG, bufferB, bufferA, layerR, layerG,
                            layerB, layerA;
                    ByteBuffer layerBuffer = layer.buffer();
                    while (layerBuffer.hasRemaining()) {
                        layerR = layerBuffer.get() & 0xFF;
                        layerG = layerBuffer.get() & 0xFF;
                        layerB = layerBuffer.get() & 0xFF;
                        layerA = layerBuffer.get() & 0xFF;
                        if (layerA == 255) {
                            buffer.put((byte) layerR);
                            buffer.put((byte) layerG);
                            buffer.put((byte) layerB);
                            buffer.put((byte) layerA);
                        } else if (layerA != 0) {
                            buffer.mark();
                            bufferR = buffer.get() & 0xFF;
                            bufferG = buffer.get() & 0xFF;
                            bufferB = buffer.get() & 0xFF;
                            bufferA = buffer.get() & 0xFF;
                            buffer.reset();
                            double a = layerA / 255.0;
                            double oneMinusA = 1.0 - a;
                            buffer.put(
                                    (byte) (bufferR * oneMinusA + layerR * a));
                            buffer.put(
                                    (byte) (bufferG * oneMinusA + layerG * a));
                            buffer.put(
                                    (byte) (bufferB * oneMinusA + layerB * a));
                            buffer.put(
                                    (byte) FastMath.min(bufferA + layerA, 255));
                        } else {
                            buffer.position(buffer.position() + 4);
                        }
                    }
                    buffer.rewind();
                }
            } else {
                buffer = source.buffer();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load texture: {}", e.toString());
            buffer = BufferCreator.bytes(0x400);
            width = minSize;
            height = minSize;
        }
        return new Image(width, height, buffer);
    }

    public int init(ScapesEngine engine) {
        List<T> textureList = Streams.of(textures.values())
                .sorted((texture1, texture2) -> texture2.resolution() -
                        texture1.resolution()).collect(Collectors.toList());
        textureList.add(0, null);
        int size = 16;
        int[][] atlas = new int[size][size];
        int tiles = 0;
        int boundary = 0;
        int x = 0;
        int y = 0;
        for (int i = 1; i < textureList.size(); i++) {
            T texture = textureList.get(i);
            int textureTiles = texture.resolution() >> minBits;
            if (textureTiles != tiles) {
                x = 0;
                y = 0;
                tiles = textureTiles;
                boundary = size - tiles;
            }
            boolean flag = true;
            while (flag) {
                if (atlas[x][y] == 0 && x <= boundary && y <= boundary) {
                    for (int yy = 0; yy < tiles; yy++) {
                        int yyy = yy + y;
                        for (int xx = 0; xx < tiles; xx++) {
                            if (xx == 0 && yy == 0) {
                                atlas[x][y] = i;
                            } else {
                                atlas[xx + x][yyy] = -1;
                            }
                        }
                    }
                    flag = false;
                }
                x += textureTiles;
                if (x >= size) {
                    y += textureTiles;
                    x = 0;
                }
                if (y >= size) {
                    y = 0;
                    size <<= 1;
                    boundary = size - tiles;
                    int[][] newAtlas = new int[size][size];
                    for (int j = 0; j < atlas.length; j++) {
                        System.arraycopy(atlas[j], 0, newAtlas[j], 0,
                                atlas[j].length);
                    }
                    atlas = newAtlas;
                }
            }
        }
        int imageSize = size << minBits;
        ByteBuffer buffer = BufferCreator.bytes(imageSize * imageSize << 2);
        for (y = 0; y < size; y++) {
            int yy = y << minBits;
            for (x = 0; x < size; x++) {
                int i = atlas[x][y];
                if (i > 0) {
                    int xx = x << minBits;
                    T texture = textureList.get(i);
                    texture.x = (float) x / size;
                    texture.y = (float) y / size;
                    texture.tileX = xx;
                    texture.tileY = yy;
                    texture.size = (float) texture.resolution / imageSize;
                    int scansize = texture.resolution << 2;
                    int row = 0;
                    while (row < texture.resolution) {
                        buffer.position((yy + row) * imageSize + xx << 2);
                        row++;
                        texture.buffer.limit(scansize * row);
                        buffer.put(texture.buffer);
                    }
                    texture.buffer.rewind();
                    texture.buffer = null;
                }
            }
        }
        buffer.rewind();
        texture = engine.graphics()
                .createTexture(imageSize, imageSize, buffer, 4);
        sources.clear();
        return textures.size();
    }

    public ScapesEngine engine() {
        return engine;
    }

    public Texture texture() {
        return texture;
    }
}
