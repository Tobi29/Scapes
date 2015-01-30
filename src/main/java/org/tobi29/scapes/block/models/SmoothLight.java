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

package org.tobi29.scapes.block.models;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class SmoothLight {
    public static void calcLight(FloatTriple triple, Face side, int x, int y,
            int z, Terrain terrain) {
        float light = 0.0f;
        int lights = 0;
        float sunLight = 0.0f;
        int sunLights = 0;
        int ssaoLights = 0;
        BlockType type = terrain.getBlockType(x, y, z);
        if (side != Face.DOWN) {
            if (side != Face.WEST) {
                if (side != Face.NORTH) {
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight = terrain.getBlockLight(x, y, z);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x, y, z);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
                if (side != Face.SOUTH) {
                    type = terrain.getBlockType(x, y - 1, z);
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight = terrain.getBlockLight(x, y - 1, z);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x, y - 1, z);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
            }
            if (side != Face.EAST) {
                if (side != Face.NORTH) {
                    type = terrain.getBlockType(x - 1, y, z);
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight = terrain.getBlockLight(x - 1, y, z);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x - 1, y, z);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
                if (side != Face.SOUTH) {
                    type = terrain.getBlockType(x - 1, y - 1, z);
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight =
                                terrain.getBlockLight(x - 1, y - 1, z);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x - 1, y - 1, z);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
            }
        }
        if (side != Face.UP) {
            if (side != Face.WEST) {
                if (side != Face.NORTH) {
                    type = terrain.getBlockType(x, y, z - 1);
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight = terrain.getBlockLight(x, y, z - 1);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x, y, z - 1);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
                if (side != Face.SOUTH) {
                    type = terrain.getBlockType(x, y - 1, z - 1);
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight =
                                terrain.getBlockLight(x, y - 1, z - 1);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x, y - 1, z - 1);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
            }
            if (side != Face.EAST) {
                if (side != Face.NORTH) {
                    type = terrain.getBlockType(x - 1, y, z - 1);
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight =
                                terrain.getBlockLight(x - 1, y, z - 1);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x - 1, y, z - 1);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
                if (side != Face.SOUTH) {
                    type = terrain.getBlockType(x - 1, y - 1, z - 1);
                    if (!type.isSolid(terrain, x, y, z)) {
                        float tempLight =
                                terrain.getBlockLight(x - 1, y - 1, z - 1);
                        if (tempLight > 0) {
                            light += tempLight;
                            lights++;
                        }
                        tempLight = terrain.getSunLight(x - 1, y - 1, z - 1);
                        if (tempLight > 0) {
                            sunLight += tempLight;
                            sunLights++;
                        }
                        ssaoLights++;
                    }
                }
            }
        }
        if (lights == 0) {
            light = 0.0f;
        } else {
            light /= lights;
        }
        if (sunLights == 0) {
            sunLight = 0.0f;
        } else {
            sunLight /= sunLights;
        }
        triple.a = light / 15.0f;
        triple.b = sunLight / 15.0f;
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
