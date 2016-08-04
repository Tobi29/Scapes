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
package org.tobi29.scapes.vanilla.basics.util;

import org.tobi29.scapes.vanilla.basics.material.block.BlockExplosive;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobFlyingBlockServer;
import org.tobi29.scapes.entity.server.MobLivingServer;
import org.tobi29.scapes.entity.server.MobServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ExplosionUtil {
    private ExplosionUtil() {
    }

    public static void explosionEntities(WorldServer world, double x, double y,
            double z, double radius, double push, double damage) {
        assert world.checkThread();
        world.entities(new Vector3d(x, y, z), radius)
                .filter(entity -> entity instanceof MobServer)
                .forEach(entity -> {
                    Vector3 relative =
                            entity.pos().minus(new Vector3d(x, y, z));
                    double s = radius - FastMath.length(relative);
                    if (s > 0) {
                        double p = s * push;
                        Vector3 force =
                                FastMath.normalizeSafe(relative).multiply(p);
                        ((MobServer) entity)
                                .push(force.doubleX(), force.doubleY(),
                                        force.doubleZ());
                        if (entity instanceof MobLivingServer) {
                            ((MobLivingServer) entity).damage(s * damage);
                        }
                    }
                });
    }

    public static void explosionBlockPush(TerrainServer.TerrainMutable handle,
            double x, double y, double z, double size, double dropChance,
            double blockChance, double push, double damage) {
        WorldServer world = handle.world();
        BlockType air = world.air();
        List<EntityServer> entities = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        double step = 360.0 / FastMath.TWO_PI / size;
        for (double pitch = 90.0; pitch >= -90.0; pitch -= step) {
            double cosYaw = FastMath.cosTable(pitch * FastMath.DEG_2_RAD);
            double stepYawForPitch = FastMath.abs(step / cosYaw);
            double deltaZ = FastMath.sinTable(pitch * FastMath.DEG_2_RAD);
            for (double yaw = 0.0; yaw < 360.0; yaw += stepYawForPitch) {
                double deltaX =
                        FastMath.cosTable(yaw * FastMath.DEG_2_RAD) * cosYaw;
                double deltaY =
                        FastMath.sinTable(yaw * FastMath.DEG_2_RAD) * cosYaw;
                for (double distance = 0; distance < size; distance++) {
                    int xxx = FastMath.floor(x + deltaX * distance);
                    int yyy = FastMath.floor(y + deltaY * distance);
                    int zzz = FastMath.floor(z + deltaZ * distance);
                    BlockType type = handle.type(xxx, yyy, zzz);
                    if (type != air) {
                        if (type instanceof BlockExplosive) {
                            ((BlockExplosive) type)
                                    .igniteByExplosion(handle, xxx, yyy, zzz);
                        } else {
                            if (random.nextDouble() < dropChance) {
                                world.dropItems(type.drops(
                                        new ItemStack(world.registry()),
                                        handle.data(xxx, yyy, zzz)), xxx, yyy,
                                        zzz);
                            } else if (type.isSolid(handle, xxx, yyy, zzz) &&
                                    !type.isTransparent(handle, xxx, yyy,
                                            zzz) &&
                                    random.nextDouble() < blockChance) {
                                int data = handle.data(xxx, yyy, zzz);
                                entities.add(new MobFlyingBlockServer(world,
                                        new Vector3d(xxx + 0.5, yyy + 0.5,
                                                zzz + 0.5), new Vector3d(
                                        random.nextDouble() * 0.1 - 0.05,
                                        random.nextDouble() * 0.1 - 0.05,
                                        random.nextDouble() * 1 + 2), type,
                                        data));
                            }
                        }
                        handle.typeData(xxx, yyy, zzz, air, 0);
                    }
                }
            }
        }
        world.taskExecutor().addTask(() -> {
            Streams.forEach(entities, entity -> {
                entity.onSpawn();
                world.addEntity(entity);
            });
            explosionEntities(world, x, y, z, size, push, damage);
        }, "Explosion-Entities");
    }
}
