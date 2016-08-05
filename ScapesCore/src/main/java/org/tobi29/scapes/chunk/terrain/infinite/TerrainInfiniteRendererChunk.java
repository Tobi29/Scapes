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
package org.tobi29.scapes.chunk.terrain.infinite;

import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerrainInfiniteRendererChunk {
    private final TerrainInfiniteChunkClient chunk;
    private final TerrainInfiniteRenderer renderer;
    private final TerrainInfiniteChunkModel[] vao;
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
        vao = new TerrainInfiniteChunkModel[zSections];
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

    public void render(GL gl, Shader shader1, Shader shader2, Cam cam) {
        double relativeX = chunk.blockX() - cam.position.doubleX();
        double relativeY = chunk.blockY() - cam.position.doubleY();
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < vao.length; i++) {
            double relativeZ = (i << 4) - cam.position.doubleZ();
            double distance =
                    FastMath.sqr(relativeX + 8) + FastMath.sqr(relativeY + 8) +
                            FastMath.sqr(relativeZ + 8);
            boolean newLod = distance < 9216;
            TerrainInfiniteChunkModel vao = this.vao[i];
            if (vao != null && vao.model.isPresent()) {
                Pair<Model, AABB> m = vao.model.get();
                if (vao.lod != newLod) {
                    setGeometryDirty(i);
                }
                if (cam.frustum.inView(m.b) != 0) {
                    boolean animated = distance < 2304;
                    Matrix matrix = matrixStack.push();
                    matrix.translate((float) relativeX, (float) relativeY,
                            (float) relativeZ);
                    if (!m.a.render(gl, animated ? shader1 : shader2)) {
                        setGeometryDirty(i);
                    }
                    matrixStack.pop();
                } else {
                    m.a.ensureStored(gl);
                }
            }
        }
    }

    public void renderAlpha(GL gl, Shader shader1, Shader shader2, Cam cam) {
        double relativeX = chunk.blockX() - cam.position.doubleX();
        double relativeY = chunk.blockY() - cam.position.doubleY();
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < vao.length; i++) {
            TerrainInfiniteChunkModel vao = this.vao[i];
            if (vao != null && vao.modelAlpha.isPresent()) {
                Pair<Model, AABB> m = vao.modelAlpha.get();
                if (cam.frustum.inView(m.b) != 0) {
                    double relativeZ = (i << 4) - cam.position.doubleZ();
                    double distance = FastMath.sqr(relativeX + 8) +
                            FastMath.sqr(relativeY + 8) +
                            FastMath.sqr(relativeZ + 8);
                    boolean animated = distance < 2304;
                    Matrix matrix = matrixStack.push();
                    matrix.translate((float) relativeX, (float) relativeY,
                            (float) relativeZ);
                    if (!m.a.render(gl, animated ? shader1 : shader2)) {
                        setGeometryDirty(i);
                    }
                    matrixStack.pop();
                } else {
                    m.a.ensureStored(gl);
                }
            }
        }
    }

    public boolean isLoaded() {
        return geometryInit[0] && chunk.isLoaded();
    }

    public void renderFrame(GL gl, Model frame, Shader shader, Cam cam) {
        MatrixStack matrixStack = gl.matrixStack();
        for (int i = 0; i < vao.length; i++) {
            TerrainInfiniteChunkModel vao = this.vao[i];
            if (vao != null && vao.model.isPresent()) {
                AABB aabb = vao.model.get().b;
                Matrix matrix = matrixStack.push();
                gl.setAttribute2f(4, 1.0f, 1.0f);
                if (!chunk.isLoaded()) {
                    gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 0.0f, 0.0f,
                            1.0f);
                } else if (geometryDirty[i].get()) {
                    gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 1.0f, 1.0f, 0.0f,
                            1.0f);
                } else {
                    gl.setAttribute4f(GL.COLOR_ATTRIBUTE, 0.0f, 1.0f, 0.0f,
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

    public synchronized void replaceMesh(int i,
            TerrainInfiniteChunkModel model) {
        if (visible[i] && model != null) {
            model.model.ifPresent(m -> m.a.setWeak(true));
            model.modelAlpha.ifPresent(m -> m.a.setWeak(true));
            model.model.ifPresent(
                    m -> m.b.add(chunk.blockX(), chunk.blockY(), i << 4));
            model.modelAlpha.ifPresent(
                    m -> m.b.add(chunk.blockX(), chunk.blockY(), i << 4));
            vao[i] = model;
        } else {
            vao[i] = null;
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
        Arrays.fill(solid, true);
        Streams.forEach(geometryDirty, value -> value.set(true));
        Arrays.fill(geometryInit, false);
        Arrays.fill(culled, false);
        renderer.addToQueue(this);
    }
}
