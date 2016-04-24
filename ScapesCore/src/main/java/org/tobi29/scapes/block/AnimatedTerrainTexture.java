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
import org.tobi29.scapes.engine.utils.graphics.MipMapGenerator;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.nio.ByteBuffer;

public class AnimatedTerrainTexture extends TerrainTexture {
    private final ByteBuffer[][] frames;
    private boolean dirty = true;
    private double spin;
    private int i;

    public AnimatedTerrainTexture(ByteBuffer buffer, int width, int height,
            ShaderAnimation shaderAnimation, TerrainTextureRegistry registry) {
        super(buffer, width, shaderAnimation, registry);
        int frameSize = width * width << 2;
        frames = new ByteBuffer[height / width][5];
        for (int i = 0; i < frames.length; i++) {
            buffer.position(i * frameSize);
            frames[i] = MipMapGenerator
                    .generateMipMaps(buffer, registry.engine()::allocate, width,
                            width, 4, true);
        }
    }

    @Override
    protected void renderAnim(GL gl) {
        if (dirty) {
            atlas.texture().bind(gl);
            gl.replaceTextureMipMap(tileX, tileY, resolution, resolution,
                    frames[i]);
            dirty = false;
        }
    }

    @Override
    protected void updateAnim(double delta) {
        spin += delta * 20.0;
        int i = FastMath.floor(spin);
        if (i >= frames.length) {
            spin -= frames.length;
            i = 0;
        }
        this.i = i;
        dirty = true;
    }
}
