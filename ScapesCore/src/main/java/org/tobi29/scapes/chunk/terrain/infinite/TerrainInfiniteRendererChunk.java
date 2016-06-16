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

import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.opengl.vao.VAO;
import org.tobi29.scapes.engine.opengl.vao.VAOStatic;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerrainInfiniteRendererChunk {
    private final TerrainInfiniteChunkClient chunk;
    private final TerrainInfiniteRenderer renderer;
    private final VAOStatic[] vao, vaoAlpha;
    private final AABB[] aabb, aabbAlpha;
    private final AtomicBoolean[] geometryDirty;
    private final boolean[] geometryInit, solid, visible, prepareVisible,
            culled, lod;

    public TerrainInfiniteRendererChunk(TerrainInfiniteChunkClient chunk,
            TerrainInfiniteRenderer renderer) {
        this.chunk = chunk;
        this.renderer = renderer;
        int zSections = chunk.zSize() >> 4;
        geometryDirty = new AtomicBoolean[zSections];
        ArrayUtil.fill(geometryDirty, () -> new AtomicBoolean(true));
        geometryInit = new boolean[zSections + 1];
        vao = new VAOStatic[zSections];
        vaoAlpha = new VAOStatic[zSections];
        aabb = new AABB[zSections];
        aabbAlpha = new AABB[zSections];
        lod = new boolean[zSections];
        solid = new boolean[zSections];
        visible = new boolean[zSections];
        prepareVisible = new boolean[zSections];
        culled = new boolean[zSections];
        Arrays.fill(solid, true);
        renderer.addToQueue(this);
    }

    public TerrainInfiniteChunkClient chunk() {
        return chunk;
    }

    public int zSections() {
        return vao.length;
    }

    public void render(GL gl, Shader shader, Cam cam) {
        double relativeX = chunk.blockX() - cam.position.doubleX();
        double relativeY = chunk.blockY() - cam.position.doubleY();
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < vao.length; i++) {
            double relativeZ = (i << 4) - cam.position.doubleZ();
            boolean newLod = FastMath.sqr(relativeX + 8) +
                    FastMath.sqr(relativeY + 8) +
                    FastMath.sqr(relativeZ + 8) < 9216;
            if (lod[i] != newLod) {
                lod[i] = newLod;
                setGeometryDirty(i);
            }
            VAOStatic vao = this.vao[i];
            AABB aabb = this.aabb[i];
            if (vao != null && aabb != null) {
                if (cam.frustum.inView(aabb) != 0) {
                    Matrix matrix = matrixStack.push();
                    matrix.translate((float) relativeX, (float) relativeY,
                            (float) relativeZ);
                    if (!vao.render(gl, shader)) {
                        setGeometryDirty(i);
                    }
                    matrixStack.pop();
                } else {
                    vao.ensureStored(gl);
                }
            }
        }
    }

    public void renderAlpha(GL gl, Shader shader, Cam cam) {
        double relativeX = chunk.blockX() - cam.position.doubleX();
        double relativeY = chunk.blockY() - cam.position.doubleY();
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < vao.length; i++) {
            VAOStatic vao = vaoAlpha[i];
            AABB aabb = aabbAlpha[i];
            if (vao != null && aabb != null) {
                if (cam.frustum.inView(aabb) != 0) {
                    Matrix matrix = matrixStack.push();
                    matrix.translate((float) relativeX, (float) relativeY,
                            (float) ((i << 4) - cam.position.doubleZ()));
                    if (!vao.render(gl, shader)) {
                        setGeometryDirty(i);
                    }
                    matrixStack.pop();
                } else {
                    vao.ensureStored(gl);
                }
            }
        }
    }

    public boolean isLoaded() {
        return geometryInit[0] && chunk.isLoaded();
    }

    public void renderFrame(GL gl, VAO frame, Shader shader, Cam cam) {
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < aabb.length; i++) {
            AABB aabb = this.aabb[i];
            if (aabb != null) {
                Matrix matrix = matrixStack.push();
                gl.setAttribute2f(4, 1.0f, 1.0f);
                if (!chunk.isLoaded()) {
                    gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 0.0f, 0.0f,
                            1.0f);
                } else if (geometryDirty[i].get()) {
                    gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 0.0f,
                            1.0f);
                } else {
                    gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.0f, 1.0f, 0.0f,
                            1.0f);
                }
                matrix.translate((float) (aabb.minX - cam.position.doubleX()),
                        (float) (aabb.minY - cam.position.doubleY()),
                        (float) (aabb.minZ - cam.position.doubleZ()));
                matrix.scale((float) (aabb.maxX - aabb.minX),
                        (float) (aabb.maxY - aabb.minY),
                        (float) (aabb.maxZ - aabb.minZ));
                frame.render(gl, shader);
                matrixStack.pop();
            }
        }
    }

    public synchronized void replaceMesh(int i, VAOStatic render,
            VAOStatic renderAlpha, AABB aabb, AABB aabbAlpha) {
        if (visible[i]) {
            if (render != null) {
                render.setWeak(true);
            }
            if (renderAlpha != null) {
                renderAlpha.setWeak(true);
            }
            vao[i] = render;
            vaoAlpha[i] = renderAlpha;
            if (aabb != null) {
                aabb.add(chunk.blockX(), chunk.blockY(), i << 4);
            }
            if (aabbAlpha != null) {
                aabbAlpha.add(chunk.blockX(), chunk.blockY(), i << 4);
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
            geometryDirty[i].set(true);
        }
        renderer.addToQueue(this);
    }

    public void setGeometryDirty(int i) {
        if (!geometryDirty[i].getAndSet(true)) {
            renderer.addToQueue(this, i);
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

    public boolean unsetGeometryDirty(int i) {
        return geometryDirty[i].getAndSet(false);
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
        Arrays.fill(vao, null);
        Arrays.fill(vaoAlpha, null);
        Arrays.fill(solid, true);
        Streams.forEach(geometryDirty, value -> value.set(true));
        Arrays.fill(geometryInit, false);
        Arrays.fill(culled, false);
        renderer.addToQueue(this);
    }
}
