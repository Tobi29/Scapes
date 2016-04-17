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

import java.nio.ByteBuffer;

public class TextureAtlasEntry<T extends TextureAtlas<?>> {
    protected final int resolution;
    protected final T atlas;
    protected ByteBuffer buffer;
    protected int tileX, tileY;
    protected float x, y, size;

    public TextureAtlasEntry(ByteBuffer buffer, int resolution, T atlas) {
        this.buffer = buffer;
        this.resolution = resolution;
        this.atlas = atlas;
    }

    public float x() {
        return x + size * 0.005f;
    }

    public float realX() {
        return x;
    }

    public float y() {
        return y + size * 0.005f;
    }

    public float realY() {
        return y;
    }

    public float size() {
        return size * 0.99f;
    }

    public float realSize() {
        return size;
    }

    public int resolution() {
        return resolution;
    }

    public T registry() {
        return atlas;
    }
}
