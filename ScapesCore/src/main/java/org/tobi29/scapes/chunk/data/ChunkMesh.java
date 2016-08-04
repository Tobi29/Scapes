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
package org.tobi29.scapes.chunk.data;

import org.tobi29.scapes.block.models.SmoothLight;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.graphics.Model;
import org.tobi29.scapes.engine.graphics.ModelAttribute;
import org.tobi29.scapes.engine.graphics.RenderType;
import org.tobi29.scapes.engine.graphics.VertexType;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesh {
    private static final float[] EMPTY_FLOAT = {};
    private static final int[] EMPTY_INT = {};
    private static final int START_SIZE = 6 * 6000;
    private final SmoothLight.FloatTriple triple =
            new SmoothLight.FloatTriple();
    private final AABB aabb =
            new AABB(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN);
    private final VertexArrays arrays;
    private int pos, remaining;

    public ChunkMesh(VertexArrays arrays) {
        this.arrays = arrays;
        remaining = arrays.vertexArray.length / 3;
    }

    public void addVertex(TerrainClient terrain, Face side, double x, double y,
            double z, float xx, float yy, float zz, float tx, float ty, float r,
            float g, float b, float a, boolean lod, byte anim) {
        addVertex(terrain, side, x, y, z, xx, yy, zz, Float.NaN, Float.NaN,
                Float.NaN, tx, ty, r, g, b, a, lod, anim);
    }

    public void addVertex(TerrainClient terrain, Face side, double x, double y,
            double z, float xx, float yy, float zz, float nx, float ny,
            float nz, float tx, float ty, float r, float g, float b, float a,
            boolean lod, byte anim) {
        float light, sunLight;
        if (lod) {
            SmoothLight.calcLight(triple, side, x, y, z, terrain);
            light = triple.a;
            sunLight = triple.b;
            r *= triple.c;
            g *= triple.c;
            b *= triple.c;
        } else {
            int xxx = FastMath.floor(x + side.getX());
            int yyy = FastMath.floor(y + side.getY());
            int zzz = FastMath.floor(z + side.getZ());
            light = terrain.blockLight(xxx, yyy, zzz) / 15.0f;
            sunLight = terrain.sunLight(xxx, yyy, zzz) / 15.0f;
        }
        if (remaining <= 0) {
            int growth;
            if (pos == 0) {
                growth = START_SIZE;
            } else {
                growth = pos;
            }
            changeArraySize(pos + growth);
            remaining += growth;
        }
        int i = pos * 3;
        arrays.vertexArray[i++] = xx;
        arrays.vertexArray[i++] = yy;
        arrays.vertexArray[i] = zz;
        i = pos * 3;
        arrays.normalArray[i++] = nx;
        arrays.normalArray[i++] = ny;
        arrays.normalArray[i] = nz;
        i = pos << 2;
        arrays.colorArray[i++] = r;
        arrays.colorArray[i++] = g;
        arrays.colorArray[i++] = b;
        arrays.colorArray[i] = a;
        i = pos << 1;
        arrays.textureArray[i++] = tx;
        arrays.textureArray[i] = ty;
        i = pos << 1;
        arrays.lightArray[i++] = light;
        arrays.lightArray[i] = sunLight;
        arrays.animationArray[pos++] = anim;
        aabb.minX = FastMath.min(aabb.minX, xx);
        aabb.minY = FastMath.min(aabb.minY, yy);
        aabb.minZ = FastMath.min(aabb.minZ, zz);
        aabb.maxX = FastMath.max(aabb.maxX, xx);
        aabb.maxY = FastMath.max(aabb.maxY, yy);
        aabb.maxZ = FastMath.max(aabb.maxZ, zz);
        remaining--;
    }

    private void changeArraySize(int size) {
        float[] newVertexArray = new float[size * 3];
        float[] newColorArray = new float[size << 2];
        float[] newTextureArray = new float[size << 1];
        float[] newNormalArray = new float[size * 3];
        float[] newLightArray = new float[size << 1];
        int[] newAnimationArray = new int[size];
        System.arraycopy(arrays.vertexArray, 0, newVertexArray, 0,
                FastMath.min(arrays.vertexArray.length, newVertexArray.length));
        System.arraycopy(arrays.colorArray, 0, newColorArray, 0,
                FastMath.min(arrays.colorArray.length, newColorArray.length));
        System.arraycopy(arrays.textureArray, 0, newTextureArray, 0,
                FastMath.min(arrays.textureArray.length,
                        newTextureArray.length));
        System.arraycopy(arrays.normalArray, 0, newNormalArray, 0,
                FastMath.min(arrays.normalArray.length, newNormalArray.length));
        System.arraycopy(arrays.lightArray, 0, newLightArray, 0,
                FastMath.min(arrays.lightArray.length, newLightArray.length));
        System.arraycopy(arrays.animationArray, 0, newAnimationArray, 0,
                FastMath.min(arrays.animationArray.length,
                        newAnimationArray.length));
        arrays.vertexArray = newVertexArray;
        arrays.colorArray = newColorArray;
        arrays.textureArray = newTextureArray;
        arrays.normalArray = newNormalArray;
        arrays.lightArray = newLightArray;
        arrays.animationArray = newAnimationArray;
    }

    private void computeNormals() {
        int ii1 = 0, ii2 = 0;
        for (int i = 0; i < pos; i += 4) {
            if (Float.isNaN(arrays.normalArray[ii2])) {
                float x1 = arrays.vertexArray[ii1++];
                float y1 = arrays.vertexArray[ii1++];
                float z1 = arrays.vertexArray[ii1++];
                float x2 = arrays.vertexArray[ii1++];
                float y2 = arrays.vertexArray[ii1++];
                float z2 = arrays.vertexArray[ii1++];
                float x3 = arrays.vertexArray[ii1++];
                float y3 = arrays.vertexArray[ii1++];
                float z3 = arrays.vertexArray[ii1++];
                float x4 = arrays.vertexArray[ii1++];
                float y4 = arrays.vertexArray[ii1++];
                float z4 = arrays.vertexArray[ii1++];
                float ux = x2 - x1;
                float uy = y2 - y1;
                float uz = z2 - z1;
                float vx = x3 - x1;
                float vy = y3 - y1;
                float vz = z3 - z1;
                float nx = uy * vz - uz * vy;
                float ny = uz * vx - ux * vz;
                float nz = ux * vy - uy * vx;
                ux = x4 - x1;
                uy = y4 - y1;
                uz = z4 - z1;
                nx += vy * uz - vz * uy;
                ny += vz * ux - vx * uz;
                nz += vx * uy - vy * ux;
                nx *= 0.5f;
                ny *= 0.5f;
                nz *= 0.5f;
                float length =
                        (float) FastMath.sqrt(nx * nx + ny * ny + nz * nz);
                nx /= length;
                ny /= length;
                nz /= length;
                arrays.normalArray[ii2++] = nx;
                arrays.normalArray[ii2++] = ny;
                arrays.normalArray[ii2++] = nz;
                arrays.normalArray[ii2++] = nx;
                arrays.normalArray[ii2++] = ny;
                arrays.normalArray[ii2++] = nz;
                arrays.normalArray[ii2++] = nx;
                arrays.normalArray[ii2++] = ny;
                arrays.normalArray[ii2++] = nz;
                arrays.normalArray[ii2++] = nx;
                arrays.normalArray[ii2++] = ny;
                arrays.normalArray[ii2++] = nz;
            } else {
                ii1 += 12;
                ii2 += 12;
            }
        }
    }

    public Model finish(ScapesEngine engine) {
        computeNormals();
        int[] indexArray = new int[pos * 3 / 2];
        int i = 0, p = 0;
        while (i < indexArray.length) {
            indexArray[i++] = p;
            indexArray[i++] = p + 1;
            indexArray[i++] = p + 2;
            indexArray[i++] = p;
            indexArray[i++] = p + 2;
            indexArray[i++] = p + 3;
            p += 4;
        }
        List<ModelAttribute> attributes = new ArrayList<>(6);
        attributes.add(new ModelAttribute(0, 3, arrays.vertexArray, pos * 3,
                false, 0, VertexType.HALF_FLOAT));
        attributes
                .add(new ModelAttribute(1, 4, arrays.colorArray, pos << 2, true,
                        0, VertexType.UNSIGNED_BYTE));
        attributes.add(new ModelAttribute(2, 2, arrays.textureArray, pos << 1,
                true, 0, VertexType.UNSIGNED_SHORT));
        attributes
                .add(new ModelAttribute(3, 3, arrays.normalArray, pos * 3, true,
                        0, VertexType.BYTE));
        attributes
                .add(new ModelAttribute(4, 2, arrays.lightArray, pos << 1, true,
                        0, VertexType.UNSIGNED_BYTE));
        attributes.add(new ModelAttribute(5, 1, arrays.animationArray, pos, 0,
                VertexType.UNSIGNED_BYTE));
        return engine.graphics().createModelStatic(attributes, pos, indexArray,
                RenderType.TRIANGLES);
    }

    public int size() {
        return (int) (pos * 1.5);
    }

    public AABB aabb() {
        return aabb;
    }

    public static class VertexArrays {
        public float[] vertexArray = EMPTY_FLOAT;
        public float[] colorArray = EMPTY_FLOAT;
        public float[] textureArray = EMPTY_FLOAT;
        public float[] normalArray = EMPTY_FLOAT;
        public float[] lightArray = EMPTY_FLOAT;
        public int[] animationArray = EMPTY_INT;
    }
}
