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

package org.tobi29.scapes.vanilla.basics.entity.particle;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.client.states.scenes.SceneScapesVoxelWorld;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.matrix.Matrix4f;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.particle.ParticleEmitterInstanced;
import org.tobi29.scapes.entity.particle.ParticleInstance;
import org.tobi29.scapes.entity.particle.ParticlePhysics;
import org.tobi29.scapes.entity.particle.ParticleSystem;

import java.util.ArrayList;
import java.util.List;

public class ParticleEmitterSnow
        extends ParticleEmitterInstanced<ParticleInstanceSnow> {
    private static final float[] EMPTY_FLOAT = {};
    private static final float SIZE = 0.075f;
    private final Shader shader;
    private final Matrix4f matrix = new Matrix4f();

    public ParticleEmitterSnow(ParticleSystem system, Texture texture) {
        super(system, texture, createAttributes(), 6, createAttributesStream(),
                RenderType.TRIANGLES, new ParticleInstanceSnow[4096],
                ParticleInstanceSnow::new);
        GraphicsSystem graphics = system.world().game().engine().graphics();
        shader = graphics.createShader("VanillaBasics:shader/ParticleSnow");
    }

    private static List<ModelAttribute> createAttributes() {
        List<ModelAttribute> attributes = new ArrayList<>();
        attributes.add(new ModelAttribute(GL.VERTEX_ATTRIBUTE, 3,
                new float[]{-SIZE, 0.0f, -SIZE, SIZE, 0.0f, -SIZE, SIZE, 0.0f,
                        SIZE, -SIZE, 0.0f, SIZE, -SIZE, 0.0f, -SIZE, SIZE, 0.0f,
                        SIZE}, false, 0, VertexType.HALF_FLOAT));
        attributes.add(new ModelAttribute(GL.COLOR_ATTRIBUTE, 4,
                new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}, true, 0,
                VertexType.UNSIGNED_BYTE));
        attributes.add(new ModelAttribute(GL.TEXTURE_ATTRIBUTE, 2,
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f, 1.0f}, false, 0, VertexType.FLOAT));
        return attributes;
    }

    private static List<ModelAttribute> createAttributesStream() {
        List<ModelAttribute> attributes = new ArrayList<>();
        attributes.add(new ModelAttribute(4, 2, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        attributes.add(new ModelAttribute(5, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        attributes.add(new ModelAttribute(6, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        attributes.add(new ModelAttribute(7, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        attributes.add(new ModelAttribute(8, 4, EMPTY_FLOAT, false, 1,
                VertexType.FLOAT));
        return attributes;
    }

    @Override
    public void update(double delta) {
        if (!hasAlive) {
            return;
        }
        AABB aabb = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        float gravitation = (float) system.world().gravity();
        TerrainClient terrain = system.world().terrain();
        boolean hasAlive = false;
        for (ParticleInstanceSnow instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            hasAlive = true;
            instance.time -= delta;
            if (instance.time <= 0.0) {
                instance.state = ParticleInstance.State.DEAD;
                continue;
            }
            aabb.minX = instance.pos.doubleX() - SIZE;
            aabb.minY = instance.pos.doubleY() - SIZE;
            aabb.minZ = instance.pos.doubleZ() - SIZE;
            aabb.maxX = instance.pos.doubleX() + SIZE;
            aabb.maxY = instance.pos.doubleY() + SIZE;
            aabb.maxZ = instance.pos.doubleZ() + SIZE;
            ParticlePhysics
                    .update(delta, instance, terrain, aabb, gravitation, 1.0f,
                            0.9f, 0.4f, 8.0f);
        }
        this.hasAlive = hasAlive;
    }

    @Override
    protected Shader prepareShader(GL gl, Cam cam) {
        WorldClient world = system.world();
        SceneScapesVoxelWorld scene = world.scene();
        MobPlayerClientMain player = world.player();
        EnvironmentClient environment = world.environment();
        float sunLightReduction = environment
                .sunLightReduction(cam.position.doubleX(),
                        cam.position.doubleY()) / 15.0f;
        float playerLight = FastMath.max(
                player.leftWeapon().material().playerLight(player.leftWeapon()),
                player.rightWeapon().material()
                        .playerLight(player.rightWeapon()));
        shader.setUniform3f(4, scene.fogR(), scene.fogG(), scene.fogB());
        shader.setUniform1f(5, scene.fogDistance() * scene.renderDistance());
        shader.setUniform1i(6, 1);
        shader.setUniform1f(7, sunLightReduction);
        shader.setUniform1f(8, playerLight);
        return shader;
    }

    @Override
    protected int prepareBuffer(Cam cam) {
        WorldClient world = system.world();
        TerrainClient terrain = world.terrain();
        int count = 0;
        for (ParticleInstanceSnow instance : instances) {
            if (instance.state != ParticleInstance.State.ALIVE) {
                continue;
            }
            int x = instance.pos.intX(), y = instance.pos.intY(), z =
                    instance.pos.intZ();
            BlockType type = terrain.type(x, y, z);
            if (!type.isSolid(terrain, x, y, z) ||
                    type.isTransparent(terrain, x, y, z)) {
                float posRenderX = (float) (instance.pos.doubleX() -
                        cam.position.doubleX());
                float posRenderY = (float) (instance.pos.doubleY() -
                        cam.position.doubleY());
                float posRenderZ = (float) (instance.pos.doubleZ() -
                        cam.position.doubleZ());
                float yaw = FastMath.atan2Fast(-posRenderY, -posRenderX);
                float pitch = FastMath.atan2Fast(posRenderZ,
                        (float) FastMath.length(posRenderX, posRenderY));
                matrix.identity();
                matrix.translate(posRenderX, posRenderY, posRenderZ);
                matrix.rotateRad(yaw + (float) FastMath.HALF_PI, 0, 0, 1);
                matrix.rotateRad(pitch, 1, 0, 0);
                matrix.rotateRad(yaw + instance.dir, 0, 1, 0);
                buffer.putFloat(terrain.blockLight(x, y, z) / 15.0f);
                buffer.putFloat(terrain.sunLight(x, y, z) / 15.0f);
                matrix.putInto(buffer);
                count++;
            }
        }
        return count;
    }
}
