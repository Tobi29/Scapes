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

package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3f;
import org.tobi29.scapes.entity.MobPositionHandler;
import org.tobi29.scapes.entity.MobileEntity;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.particle.ParticleEmitter3DBlock;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterTornado;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class EntityTornadoClient extends EntityClient implements MobileEntity {
    private final MobPositionHandler positionHandler;
    private double puff;
    private float baseSpin;

    public EntityTornadoClient(WorldClient world) {
        this(world, Vector3d.ZERO);
    }

    public EntityTornadoClient(WorldClient world, Vector3 pos) {
        super(world, pos);
        positionHandler =
                new MobPositionHandler(pos, world::send, this.pos::set,
                        newSpeed -> {
                        }, newPos -> {
                }, (ground, slidingWall, inWater, swimming) -> {
                });
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        if (positionHandler != null) {
            positionHandler.receiveMoveAbsolute(pos.doubleX(), pos.doubleY(),
                    pos.doubleZ());
        }
    }

    @Override
    public void update(double delta) {
        baseSpin += 40.0 * delta;
        baseSpin %= 360.0;
        puff -= delta;
        while (puff <= 0.0) {
            puff += 0.05;
            Random random = ThreadLocalRandom.current();
            float spin = random.nextFloat() * 360.0f;
            ParticleEmitterTornado emitter = world.scene().particles()
                    .emitter(ParticleEmitterTornado.class);
            emitter.add(instance -> {
                instance.pos.set(pos.now());
                instance.speed.set(Vector3f.ZERO);
                instance.time = 12.0f;
                instance.dir = random.nextFloat() * 360.0f;
                instance.spin = spin;
                instance.baseSpin = baseSpin * (float) FastMath.DEG_2_RAD;
                instance.width = 0.0f;
                instance.widthRandom = random.nextFloat() + 3.0f;
            });
            emitter.add(instance -> {
                instance.pos.set(pos.now());
                instance.speed.set(Vector3f.ZERO);
                instance.time = 1.0f;
                instance.dir = random.nextFloat() * 360.0f;
                instance.spin = spin;
                instance.baseSpin = baseSpin * (float) FastMath.DEG_2_RAD;
                instance.width = 0.0f;
                instance.widthRandom = random.nextFloat() * 20.0f + 20.0f;
            });
            if (random.nextInt(10) == 0) {
                emitter.add(instance -> {
                    instance.pos.set(pos.now());
                    instance.speed.set(Vector3f.ZERO);
                    instance.time = 12.0f;
                    instance.dir = random.nextFloat() * 360.0f;
                    instance.spin = spin;
                    instance.baseSpin = baseSpin * (float) FastMath.DEG_2_RAD;
                    instance.width = 0.0f;
                    instance.widthRandom = random.nextFloat() * 10.0f + 6.0f;
                });
            }
            TerrainClient terrain = world.terrain();
            int x = pos.intX() + random.nextInt(9) - 4;
            int y = pos.intY() + random.nextInt(9) - 4;
            int z = pos.intZ() + random.nextInt(7) - 3;
            BlockType type = terrain.type(x, y, z);
            if (type != world.air()) {
                ParticleEmitter3DBlock emitter2 = world.scene().particles()
                        .emitter(ParticleEmitter3DBlock.class);
                emitter2.add(instance -> {
                    Random random2 = ThreadLocalRandom.current();
                    double dir = random2.nextDouble() * FastMath.TWO_PI;
                    double dirSpeed = random2.nextDouble() * 12.0 + 20.0;
                    double dirSpeedX =
                            FastMath.cosTable(dir) * FastMath.cosTable(dir) *
                                    dirSpeed;
                    double dirSpeedY =
                            FastMath.sinTable(dir) * FastMath.cosTable(dir) *
                                    dirSpeed;
                    double dirSpeedZ = random2.nextDouble() * 6.0 + 24.0;
                    instance.pos.set(pos.now());
                    instance.speed.set(dirSpeedX, dirSpeedY, dirSpeedZ);
                    instance.time = 5.0f;
                    instance.rotation.set(0.0f, 0.0f, 0.0f);
                    instance.rotationSpeed.set(FastMath.normalizeSafe(
                            new Vector3f(random2.nextFloat() - 0.5f,
                                    random2.nextFloat() - 0.5f,
                                    random2.nextFloat() - 0.5f))
                            .multiply(480.0f));
                    instance.item = new ItemStack(type, terrain.data(x, y, z));
                });
            }
        }
    }

    @Override
    public MobPositionHandler positionHandler() {
        return positionHandler;
    }
}
