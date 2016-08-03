/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.block.models;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class SmoothLight {
    private static final byte[] SIDES =
            {0b000, 0b100, 0b010, 0b110, 0b001, 0b101, 0b011, 0b111, 0b010,
                    0b110, 0b011, 0b111, 0b000, 0b010, 0b001, 0b011, 0b000,
                    0b100, 0b001, 0b101, 0b100, 0b110, 0b101, 0b111, 0b000,
                    0b100, 0b010, 0b110, 0b001, 0b101, 0b011, 0b111};

    public static void calcLight(FloatTriple triple, Face side, int x, int y,
            int z, Terrain terrain) {
        int light = 0;
        int lights = 0;
        int sunLight = 0;
        int sunLights = 0;
        int ssaoLights = 0;
        int i, limit;
        if (side == Face.NONE) {
            i = 24;
            limit = 30;
        } else {
            i = side.getData() << 2;
            limit = i + 4;
        }
        while (i < limit) {
            byte offset = SIDES[i];
            int xx = x - (offset >> 2 & 1);
            int yy = y - (offset >> 1 & 1);
            int zz = z - (offset & 1);
            BlockType type = terrain.type(xx, yy, zz);
            if (!type.isSolid(terrain, xx, yy, zz)) {
                float tempLight = terrain.blockLight(xx, yy, zz);
                if (tempLight > 0) {
                    light += tempLight;
                    lights++;
                }
                tempLight = terrain.sunLight(xx, yy, zz);
                if (tempLight > 0) {
                    sunLight += tempLight;
                    sunLights++;
                }
                ssaoLights++;
            }
            i++;
        }
        if (lights == 0) {
            triple.a = 0.0f;
        } else {
            triple.a = light / lights / 15.0f;
        }
        if (sunLights == 0) {
            triple.b = 0.0f;
        } else {
            triple.b = sunLight / sunLights / 15.0f;
        }
        triple.c = FastMath.clamp(0.3f * ssaoLights - 0.2f, 0.0f, 1.0f);
    }

    public static void calcLight(FloatTriple triple, Face side, double x,
            double y, double z, Terrain terrain) {
        calcLight(triple, side, FastMath.round(x), FastMath.round(y),
                FastMath.round(z), terrain);
    }

    public static class FloatTriple {
        public float a;
        public float b;
        public float c;
    }
}
