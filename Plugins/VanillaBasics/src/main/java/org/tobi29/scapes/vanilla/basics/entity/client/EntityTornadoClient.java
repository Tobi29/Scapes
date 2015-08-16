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

package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.MobPositionHandler;
import org.tobi29.scapes.entity.MobileEntity;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleTornado;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleTornadoBlock;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class EntityTornadoClient extends EntityClient implements MobileEntity {
    private final MobPositionHandler positionHandler;
    private double baseSpin;

    public EntityTornadoClient(WorldClient world) {
        this(world, Vector3d.ZERO);
    }

    public EntityTornadoClient(WorldClient world, Vector3 pos) {
        super(world, pos);
        PlayConnection connection = world.connection();
        positionHandler =
                new MobPositionHandler(pos, connection::send, this.pos::set,
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
        double spin = ThreadLocalRandom.current().nextDouble() * 360.0;
        baseSpin += 40.0 * delta;
        baseSpin %= 360.0;
        Random random = ThreadLocalRandom.current();
        ParticleManager particleManager = world.particleManager();
        particleManager.add(new ParticleTornado(particleManager, pos.now(),
                Vector3d.ZERO, random.nextFloat() * 360, 12.0, spin, baseSpin,
                random.nextDouble() + 3));
        if (random.nextInt(10) == 0) {
            particleManager.add(new ParticleTornado(particleManager, pos.now(),
                    Vector3d.ZERO, random.nextFloat() * 360, 12.0, spin,
                    baseSpin, random.nextDouble() * 10 + 6));
        }
        if (random.nextInt(80) == 0) {
            int x = pos.intX() + random.nextInt(9) - 4;
            int y = pos.intY() + random.nextInt(9) - 4;
            int z = world.terrain().highestTerrainBlockZAt(x, y) - 1;
            particleManager
                    .add(new ParticleTornadoBlock(particleManager, pos.now(),
                            Vector3d.ZERO, random.nextFloat() * 360, 12.0, spin,
                            baseSpin, random.nextDouble() * 10 + 6,
                            world.terrain().type(x, y, z),
                            world.terrain().data(x, y, z)));
        }
        int x = pos.intX() + random.nextInt(9) - 4;
        int y = pos.intY() + random.nextInt(9) - 4;
        int z = pos.intZ() + random.nextInt(7) - 3;
        if (world.terrain().type(x, y, z) != world.air()) {
            particleManager
                    .add(new ParticleTornadoBlock(particleManager, pos.now(),
                            Vector3d.ZERO, random.nextFloat() * 360, 1.0, spin,
                            baseSpin, random.nextDouble() * 20 + 20,
                            world.terrain().type(x, y, z),
                            world.terrain().data(x, y, z)));
        }
        particleManager.add(new ParticleTornado(particleManager, pos.now(),
                Vector3d.ZERO, random.nextFloat() * 360, 1.0, spin, baseSpin,
                random.nextDouble() * 20 + 20));
    }

    @Override
    public MobPositionHandler positionHandler() {
        return positionHandler;
    }
}
