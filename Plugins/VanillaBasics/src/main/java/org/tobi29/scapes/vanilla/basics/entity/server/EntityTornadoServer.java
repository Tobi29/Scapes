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
package org.tobi29.scapes.vanilla.basics.entity.server;

import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.MobPositionHandler;
import org.tobi29.scapes.entity.MobileEntity;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobServer;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class EntityTornadoServer extends EntityServer implements MobileEntity {
    private final MobPositionHandler positionHandler;
    private double time, dir;

    public EntityTornadoServer(WorldServer world) {
        this(world, Vector3d.ZERO);
    }

    public EntityTornadoServer(WorldServer world, Vector3 pos) {
        super(world, pos);
        Random random = ThreadLocalRandom.current();
        dir = random.nextDouble() * 360;
        time = random.nextInt(100) + 20;
        positionHandler =
                new MobPositionHandler(pos, world::send, this.pos::set,
                        newSpeed -> {
                        }, newPos -> {
                }, (ground, slidingWall, inWater, swimming) -> {
                });
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        tag.setDouble("Dir", dir);
        tag.setDouble("Time", time);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        if (positionHandler != null) {
            positionHandler.receiveMoveAbsolute(pos.doubleX(), pos.doubleY(),
                    pos.doubleZ());
        }
        dir = tagStructure.getDouble("Dir");
        time = tagStructure.getDouble("Time");
    }

    @Override
    public void update(double delta) {
        Random random = ThreadLocalRandom.current();
        dir += (random.nextDouble() * 80.0 - 40.0) * delta;
        double d = dir * FastMath.DEG_2_RAD;
        double speed = 2.0 * delta;
        pos.plusX(FastMath.cosTable(d) * speed);
        pos.plusY(FastMath.sinTable(d) * speed);
        pos.setZ(world.getTerrain()
                .highestTerrainBlockZAt(pos.intX(), pos.intY()) + 0.5);
        positionHandler
                .submitUpdate(uuid, pos.now(), Vector3d.ZERO, Vector3d.ZERO,
                        false, false, false, false);
        Vector3 currentPos = pos.now();
        world.entities(currentPos, 16.0,
                stream -> stream.filter(entity -> entity instanceof MobServer)
                        .forEach(entity -> {
                            Vector3 push = entity.pos().minus(currentPos);
                            double s = FastMath.max(0.0,
                                    320.0 - FastMath.length(push) * 8.0) *
                                    delta;
                            Vector3 force =
                                    FastMath.normalizeSafe(push).multiply(-s);
                            ((MobServer) entity)
                                    .push(force.doubleX(), force.doubleY(), s);
                        }));
        time -= delta;
        if (time <= 0.0) {
            world.removeEntity(this);
        }
    }

    @Override
    public MobPositionHandler positionHandler() {
        return positionHandler;
    }
}
