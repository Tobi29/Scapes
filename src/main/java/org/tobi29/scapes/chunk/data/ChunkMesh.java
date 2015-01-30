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

package org.tobi29.scapes.chunk.data;

import org.tobi29.scapes.block.models.SmoothLight;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.RenderType;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.VertexType;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesh {
    private static final int BATCH_SIZE = 1200;
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
            double z, float xx, float yy, float zz, float texX, float texY,
            float r, float g, float b, float a, byte anim) {
        SmoothLight.calcLight(triple, side, x, y, z, terrain);
        r *= triple.c;
        g *= triple.c;
        b *= triple.c;
        addVertex(xx, yy, zz, Float.NaN, Float.NaN, Float.NaN, r, g, b, a, texX,
                texY, triple.a, triple.b, anim);
    }

    public void addVertex(TerrainClient terrain, Face side, double x, double y,
            double z, float xx, float yy, float zz, float nx, float ny,
            float nz, float texX, float texY, float r, float g, float b,
            float a, byte anim) {
        SmoothLight.calcLight(triple, side, x, y, z, terrain);
        r *= triple.c;
        g *= triple.c;
        b *= triple.c;
        addVertex(xx, yy, zz, nx, ny, nz, r, g, b, a, texX, texY, triple.a,
                triple.b, anim);
    }

    public void addVertex(float x, float y, float z, float nx, float ny,
            float nz, float r, float g, float b, float a, float tx, float ty,
            float bl, float sl, byte anim) {
        if (remaining <= 0) {
            changeArraySize(pos + BATCH_SIZE);
            remaining += BATCH_SIZE;
        }
        int i = pos * 3;
        arrays.vertexArray[i++] = x;
        arrays.vertexArray[i++] = y;
        arrays.vertexArray[i] = z;
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
        arrays.lightArray[i++] = bl;
        arrays.lightArray[i] = sl;
        arrays.animationArray[pos++] = anim;
        aabb.minX = FastMath.min(aabb.minX, x);
        aabb.minY = FastMath.min(aabb.minY, y);
        aabb.minZ = FastMath.min(aabb.minZ, z);
        aabb.maxX = FastMath.max(aabb.maxX, x);
        aabb.maxY = FastMath.max(aabb.maxY, y);
        aabb.maxZ = FastMath.max(aabb.maxZ, z);
        remaining--;
    }

    private void changeArraySize(int size) {
        float[] newVertexArray = new float[size * 3];
        float[] newColorArray = new float[size << 2];
        float[] newTextureArray = new float[size << 1];
        float[] newNormalArray = new float[size * 3];
        float[] newLightArray = new float[size << 1];
        byte[] newAnimationArray = new byte[size];
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

    public VAO finish() {
        computeNormals();
        int[] indexArray = new int[pos * 3 / 2];
        for (int i = 0, p = 0; i < indexArray.length; p += 4) {
            indexArray[i++] = p;
            indexArray[i++] = p + 1;
            indexArray[i++] = p + 2;
            indexArray[i++] = p;
            indexArray[i++] = p + 2;
            indexArray[i++] = p + 3;
        }
        List<VAO.VAOAttribute> vaoAttributes = new ArrayList<>(6);
        vaoAttributes
                .add(new VAO.VAOAttribute(0, 3, arrays.vertexArray, pos * 3,
                        false, VertexType.HALF_FLOAT));
        vaoAttributes
                .add(new VAO.VAOAttribute(1, 4, arrays.colorArray, pos << 2,
                        true, VertexType.UNSIGNED_BYTE));
        vaoAttributes
                .add(new VAO.VAOAttribute(2, 2, arrays.textureArray, pos << 1,
                        true, VertexType.UNSIGNED_SHORT));
        vaoAttributes
                .add(new VAO.VAOAttribute(3, 3, arrays.normalArray, pos * 3,
                        true, VertexType.BYTE));
        vaoAttributes
                .add(new VAO.VAOAttribute(4, 2, arrays.lightArray, pos << 1,
                        true, VertexType.UNSIGNED_BYTE));
        vaoAttributes.add(new VAO.VAOAttribute(5, 1, arrays.animationArray, pos,
                VertexType.UNSIGNED_BYTE));
        return new VAO(vaoAttributes, pos, indexArray, RenderType.TRIANGLES);
    }

    public int getSize() {
        return (int) (pos * 1.5);
    }

    public AABB getAABB() {
        return aabb;
    }

    public static class VertexArrays {
        public float[] vertexArray = new float[0];
        public float[] colorArray = new float[0];
        public float[] textureArray = new float[0];
        public float[] normalArray = new float[0];
        public float[] lightArray = new float[0];
        public byte[] animationArray = new byte[0];
    }
}
