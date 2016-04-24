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

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.client.EntityBlockBreakClient;

public class EntityModelBlockBreak implements EntityModel {
    private final MutableVector3 pos;
    private final EntityBlockBreakClient entity;
    private final Pool<PointerPane> pointerPanes = new Pool<>(PointerPane::new);
    private final VAO vao;

    public EntityModelBlockBreak(EntityModelBlockBreakShared shared,
            EntityBlockBreakClient entity) {
        this.entity = entity;
        pos = new MutableVector3d(entity.pos());
        vao = shared.vao;
    }

    @Override
    public Vector3 pos() {
        return pos.now();
    }

    @Override
    public void shapeAABB(AABB aabb) {
        aabb.minX = pos.doubleX() - 0.5;
        aabb.minY = pos.doubleY() - 0.5;
        aabb.minZ = pos.doubleZ() - 0.5;
        aabb.maxX = pos.doubleX() + 0.5;
        aabb.maxY = pos.doubleY() + 0.5;
        aabb.maxZ = pos.doubleZ() + 0.5;
    }

    @Override
    public void renderUpdate(double delta) {
        double factor = FastMath.min(1.0, delta * 5.0);
        pos.plus(entity.pos().minus(pos.now()).multiply(factor));
        pointerPanes.reset();
        TerrainClient terrain = entity.world().terrain();
        terrain.type(pos.intX(), pos.intY(), pos.intZ()).addPointerCollision(
                terrain.data(pos.intX(), pos.intY(), pos.intZ()), pointerPanes,
                pos.intX(), pos.intY(), pos.intZ());
    }

    @Override
    public void render(GL gl, WorldClient world, Cam cam, Shader shader) {
        float posRenderX = (float) (pos.doubleX() - cam.position.doubleX());
        float posRenderY = (float) (pos.doubleY() - cam.position.doubleY());
        float posRenderZ = (float) (pos.doubleZ() - cam.position.doubleZ());
        int i = FastMath.floor(entity.progress() * 10) + 1;
        if (i < 1 || i > 10) {
            return;
        }
        gl.setAttribute2f(4,
                world.terrain().blockLight(pos.intX(), pos.intY(), pos.intZ()) /
                        15.0f,
                world.terrain().sunLight(pos.intX(), pos.intY(), pos.intZ()) /
                        15.0f);
        gl.textures().bind("Scapes:image/entity/Break" + i, gl);
        for (PointerPane pane : pointerPanes) {
            MatrixStack matrixStack = gl.matrixStack();
            Matrix matrix = matrixStack.push();
            matrix.translate((float) (posRenderX - 0.5 +
                            (pane.aabb.minX + pane.aabb.maxX) / 2),
                    (float) (posRenderY - 0.5 +
                            (pane.aabb.minY + pane.aabb.maxY) / 2),
                    (float) (posRenderZ - 0.5 +
                            (pane.aabb.minZ + pane.aabb.maxZ) / 2));
            matrix.scale((float) (pane.aabb.maxX - pane.aabb.minX) + 0.01f,
                    (float) (pane.aabb.maxY - pane.aabb.minY) + 0.01f,
                    (float) (pane.aabb.maxZ - pane.aabb.minZ) + 0.01f);
            switch (pane.face) {
                case DOWN:
                    matrix.rotate(180, 1, 0, 0);
                    break;
                case NORTH:
                    matrix.rotate(90, 1, 0, 0);
                    break;
                case EAST:
                    matrix.rotate(90, 0, 1, 0);
                    break;
                case SOUTH:
                    matrix.rotate(270, 1, 0, 0);
                    break;
                case WEST:
                    matrix.rotate(270, 0, 1, 0);
                    break;
            }
            gl.setAttribute4f(OpenGL.COLOR_ATTRIBUTE, 0.3f, 0.3f, 0.3f, 0.4f);
            vao.render(gl, shader);
            matrixStack.pop();
        }
    }
}
