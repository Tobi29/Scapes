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

package org.tobi29.scapes.chunk.terrain.infinite;

import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Arrays;

public class TerrainInfiniteRendererChunk {
    private static final VAO FRAME;
    private final TerrainInfiniteChunkClient chunk;
    private final TerrainInfiniteRenderer renderer;
    private final VAO[] vao;
    private final VAO[] vaoAlpha;
    private final AABB[] aabb;
    private final AABB[] aabbAlpha;
    private final boolean[] geometryDirty;
    private final boolean[] geometryInit;
    private final boolean[] solid;
    private final boolean[] visible;
    private final boolean[] prepareVisible;
    private final boolean[] culled;
    private final byte[] lod;

    static {
        float min = 0.001f;
        float max = 0.999f;
        FRAME = VAOUtility.createVI(
                new float[]{min, min, min, max, min, min, max, max, min, min,
                        max, min, min, min, max, max, min, max, max, max, max,
                        min, max, max},
                new int[]{0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4,
                        1, 5, 2, 6, 3, 7}, RenderType.LINES);
    }

    public TerrainInfiniteRendererChunk(TerrainInfiniteChunkClient chunk,
            TerrainInfiniteRenderer renderer) {
        this.chunk = chunk;
        this.renderer = renderer;
        int zSections = chunk.getZSize() >> 4;
        geometryDirty = new boolean[zSections];
        geometryInit = new boolean[zSections + 1];
        vao = new VAO[zSections];
        vaoAlpha = new VAO[zSections];
        aabb = new AABB[zSections];
        aabbAlpha = new AABB[zSections];
        lod = new byte[zSections];
        solid = new boolean[zSections];
        visible = new boolean[zSections];
        prepareVisible = new boolean[zSections];
        culled = new boolean[zSections];
        Arrays.fill(solid, true);
        Arrays.fill(geometryDirty, true);
        renderer.addToLoadQueue(this);
    }

    public TerrainInfiniteChunkClient getChunk() {
        return chunk;
    }

    public int getZSections() {
        return vao.length;
    }

    public boolean getLod(int i) {
        return (lod[i] & 1) == 1;
    }

    public boolean isGeometryDirty(int i) {
        return geometryDirty[i];
    }

    public void render(GraphicsSystem graphics, Shader shader,
            boolean ensureStored, Cam cam) {
        double relativeX = (chunk.getX() << 4) - cam.position.doubleX();
        double relativeY = (chunk.getY() << 4) - cam.position.doubleY();
        MatrixStack matrixStack = graphics.getMatrixStack();
        for (int i = 0; i < vao.length; i++) {
            double relativeZ = (i << 4) - cam.position.doubleZ();
            boolean oldLod = (lod[i] & 1) == 1;
            boolean newLod = FastMath.sqr(relativeX + 8) +
                    FastMath.sqr(relativeY + 8) +
                    FastMath.sqr(relativeZ + 8) < 9216;
            if (newLod != oldLod) {
                if (newLod) {
                    lod[i] |= 1;
                } else {
                    lod[i] &= ~1;
                }
                if ((lod[i] & 2) == 2) {
                    geometryDirty[i] = true;
                    renderer.addToLoadQueue(this);
                }
            }
            VAO vao = this.vao[i];
            AABB aabb = this.aabb[i];
            if (vao != null && aabb != null) {
                if (cam.frustum.inView(aabb) != 0) {
                    Matrix matrix = matrixStack.push();
                    matrix.translate((float) relativeX, (float) relativeY,
                            (float) relativeZ);
                    vao.render(graphics, shader);
                    matrixStack.pop();
                } else if (ensureStored) {
                    vao.ensureStored(graphics);
                }
            }
        }
    }

    public void renderAlpha(GraphicsSystem graphics, Shader shader,
            boolean ensureStored, Cam cam) {
        double relativeX = (chunk.getX() << 4) - cam.position.doubleX();
        double relativeY = (chunk.getY() << 4) - cam.position.doubleY();
        MatrixStack matrixStack = graphics.getMatrixStack();
        for (int i = 0; i < vao.length; i++) {
            VAO vao = vaoAlpha[i];
            AABB aabb = aabbAlpha[i];
            if (vao != null && aabb != null) {
                if (cam.frustum.inView(aabb) != 0) {
                    Matrix matrix = matrixStack.push();
                    matrix.translate((float) relativeX, (float) relativeY,
                            (float) ((i << 4) - cam.position.doubleZ()));
                    vao.render(graphics, shader);
                    matrixStack.pop();
                } else if (ensureStored) {
                    vao.ensureStored(graphics);
                }
            }
        }
    }

    public boolean isLoaded() {
        return geometryInit[0] && chunk.isLoaded();
    }

    public void renderFrame(GraphicsSystem graphics, Shader shader, Cam cam) {
        MatrixStack matrixStack = graphics.getMatrixStack();
        OpenGL openGL = graphics.getOpenGL();
        for (int i = 0; i < aabb.length; i++) {
            AABB aabb = this.aabb[i];
            if (aabb != null) {
                Matrix matrix = matrixStack.push();
                openGL.setAttribute2f(4, 1.0f, 1.0f);
                if (!chunk.isLoaded()) {
                    openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 0.0f,
                            0.0f, 1.0f);
                } else if (geometryDirty[i]) {
                    openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f,
                            0.0f, 1.0f);
                } else {
                    openGL.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 1.0f,
                            0.0f, 1.0f);
                }
                matrix.translate((float) (aabb.minX - cam.position.doubleX()),
                        (float) (aabb.minY - cam.position.doubleY()),
                        (float) (aabb.minZ - cam.position.doubleZ()));
                matrix.scale((float) (aabb.maxX - aabb.minX),
                        (float) (aabb.maxY - aabb.minY),
                        (float) (aabb.maxZ - aabb.minZ));
                FRAME.render(graphics, shader);
                matrixStack.pop();
            }
        }
    }

    public synchronized void replaceMesh(int i, VAO render, VAO renderAlpha,
            AABB aabb, AABB aabbAlpha) {
        if (visible[i]) {
            vao[i] = render;
            vaoAlpha[i] = renderAlpha;
            if (aabb != null) {
                aabb.add(chunk.getX() << 4, chunk.getY() << 4, i << 4);
            }
            if (aabbAlpha != null) {
                aabbAlpha.add(chunk.getX() << 4, chunk.getY() << 4, i << 4);
            }
            this.aabb[i] = aabb;
            this.aabbAlpha[i] = aabbAlpha;
        } else {
            vao[i] = null;
            vaoAlpha[i] = null;
            this.aabb[i] = null;
            this.aabbAlpha[i] = null;
        }
        if (!geometryInit[0]) {
            geometryInit[i + 1] = true;
            boolean flag = true;
            for (int j = 1; j < geometryInit.length; j++) {
                if (!geometryInit[j]) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                geometryInit[0] = true;
            }
        }
    }

    public void setGeometryDirty() {
        for (int i = 0; i < vao.length; i++) {
            geometryDirty[i] = true;
        }
        renderer.addToLoadQueue(this);
    }

    public void setGeometryDirty(int i) {
        geometryDirty[i] = true;
        renderer.addToUpdateQueue(this);
    }

    public void setNeedsLod(int i, boolean value) {
        if (value) {
            lod[i] |= 2;
        } else {
            lod[i] &= ~2;
        }
    }

    public void setSolid(int i, boolean value) {
        if (i >= 0 && i < solid.length) {
            solid[i] = value;
        }
    }

    protected void resetPrepareVisible() {
        Arrays.fill(prepareVisible, false);
    }

    protected void setPrepareVisible(int i) {
        if (i >= 0 && i < prepareVisible.length) {
            prepareVisible[i] = true;
        }
    }

    public void updateVisible() {
        for (int i = 0; i < visible.length; i++) {
            boolean oldVisible = visible[i];
            if (prepareVisible[i] && !oldVisible) {
                visible[i] = true;
                setGeometryDirty(i);
            } else if (!prepareVisible[i] && oldVisible) {
                visible[i] = false;
                vao[i] = null;
                vaoAlpha[i] = null;
            }
        }
    }

    public void setCulled(boolean value) {
        Arrays.fill(culled, value);
    }

    public boolean setCulled(int i, boolean value) {
        if (i >= 0 && i < culled.length) {
            boolean old = culled[i];
            culled[i] = value;
            return old != value;
        }
        return false;
    }

    public void unsetGeometryDirty(int i) {
        geometryDirty[i] = false;
    }

    public boolean isSolid(int i) {
        return i < 0 || i >= solid.length || solid[i];
    }

    public boolean isVisible(int i) {
        return !(i < 0 || i >= visible.length) && visible[i];
    }

    public boolean isCulled(int i) {
        return !(i < 0 || i >= culled.length) && culled[i];
    }

    public void reset() {
        Arrays.fill(solid, true);
        Arrays.fill(geometryDirty, true);
        Arrays.fill(geometryInit, false);
        Arrays.fill(culled, false);
        Arrays.fill(vao, null);
        Arrays.fill(vaoAlpha, null);
        renderer.addToLoadQueue(this);
    }
}
