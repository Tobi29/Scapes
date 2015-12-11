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

import org.tobi29.scapes.engine.opengl.GL;

import java.nio.ByteBuffer;

public class TerrainTexture {
    protected final int tiles, resolution;
    protected final ShaderAnimation shaderAnimation;
    protected final TerrainTextureRegistry registry;
    protected ByteBuffer buffer;
    protected int tileX, tileY;
    protected float x, y, size;

    public TerrainTexture(ByteBuffer buffer, int resolution,
            ShaderAnimation shaderAnimation, TerrainTextureRegistry registry) {
        this.shaderAnimation = shaderAnimation;
        this.registry = registry;
        this.buffer = buffer;
        this.resolution = resolution;
        tiles = resolution >> 4;
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

    public TerrainTextureRegistry registry() {
        return registry;
    }

    public ShaderAnimation shaderAnimation() {
        return shaderAnimation;
    }

    protected void renderAnim(GL gl) {
    }

    protected void updateAnim(double delta) {
    }
}