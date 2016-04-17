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
package org.tobi29.scapes.entity.model;

import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.shader.Shader;

public class Box {
    private static final int[] INDICES, INDICES_NO_CULL;

    static {
        INDICES = new int[]{0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10,
                11, 12, 13, 14, 12, 14, 15, 16, 17, 18, 16, 18, 19, 20, 21, 22,
                20, 22, 23};
        INDICES_NO_CULL = new int[INDICES.length << 1];
        System.arraycopy(INDICES, 0, INDICES_NO_CULL, 0, INDICES.length);
        for (int i = 0; i < INDICES.length; i++) {
            INDICES_NO_CULL[INDICES_NO_CULL.length - i - 1] = INDICES[i];
        }
    }

    public final float minX, minY, minZ, maxX, maxY, maxZ;
    private final VAO vao;

    public Box(float tbs, float minX, float minY, float minZ, float maxX,
            float maxY, float maxZ, float tX, float tY) {
        this(tbs, minX, minY, minZ, maxX, maxY, maxZ, tX, tY, true);
    }

    public Box(float tbs, float minX, float minY, float minZ, float maxX,
            float maxY, float maxZ, float tX, float tY, boolean culling) {
        tX *= tbs;
        tY *= tbs;
        float lX = (maxX - minX) * tbs;
        float lY = (maxY - minY) * tbs;
        float lZ = (maxZ - minZ) * tbs;
        minX /= 16;
        minY /= 16;
        minZ /= 16;
        maxX /= 16;
        maxY /= 16;
        maxZ /= 16;
        int[] indices;
        if (culling) {
            indices = INDICES;
        } else {
            indices = INDICES_NO_CULL;
        }
        vao = VAOUtility.createVTNI(
                new float[]{minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY,
                        minZ, minX, maxY, minZ, minX, minY, maxZ, minX, minY,
                        minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY,
                        maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY,
                        minZ, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY,
                        minZ, maxX, maxY, maxZ, minX, minY, maxZ, maxX, minY,
                        maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY,
                        minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY,
                        minZ},
                new float[]{lY + tX, tY, lY + lX + tX, tY, lY + lX + tX,
                        lZ + tY, lY + tX, lZ + tY, lY * 2 + lX + tX, tY,
                        lY * 2 + lX + tX, lZ + tY, lY * 2 + lX * 2 + tX,
                        lZ + tY, lY * 2 + lX * 2 + tX, tY, tX, tY, lY + tX, tY,
                        lY + tX, lZ + tY, tX, lZ + tY, lY * 2 + lX + tX, tY,
                        lY * 2 + lX + tX, lZ + tY, lY + lX + tX, lZ + tY,
                        lY + lX + tX, tY, lY + tX, lZ + tY, lY + lX + tX,
                        lZ + tY, lY + lX + tX, lZ + lY + tY, lY + tX,
                        lZ + lY + tY, lY * 2 + lX + tX, lZ + tY,
                        lY * 2 + lX + tX, lZ + lY + tY, lY * 2 + lX * 2 + tX,
                        lZ + lY + tY, lY * 2 + lX * 2 + tX, lZ + tY},
                new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
                        -1.0f}, indices, RenderType.TRIANGLES);
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public void render(float r, float g, float b, float a, GL gl,
            Shader shader) {
        gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, r, g, b, a);
        vao.render(gl, shader);
    }
}
