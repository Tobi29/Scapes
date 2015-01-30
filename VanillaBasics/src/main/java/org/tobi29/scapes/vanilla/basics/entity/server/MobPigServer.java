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

package org.tobi29.scapes.vanilla.basics.entity.server;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.server.MobLivingServer;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobPigServer extends MobLivingServer {
    private double soundWait, lookWait, walkWait;

    public MobPigServer(WorldServer world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, 0.0d, 0.0d);
    }

    public MobPigServer(WorldServer world, Vector3 pos, Vector3 speed,
            double xRot, double zRot) {
        super(world, pos, speed,
                new AABB(-0.45, -0.45, -0.6875, 0.45, 0.45, 0.375), 20, 30,
                new Frustum(90, 1, 0.1, 24), new Frustum(20, 0.5, 0.1, 0.2));
        rot.setX(xRot);
        rot.setZ(zRot);
        Random random = ThreadLocalRandom.current();
        VanillaBasics plugin =
                (VanillaBasics) world.getPlugins().getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        listener((DamageListener) damage -> world
                .playSound("VanillaBasics:sound/entity/mob/pig/Hurt" +
                        (random.nextInt(2) + 1) + ".ogg", this));
        listener((DeathListener) () -> world.dropItem(
                new ItemStack(materials.meat, (short) 0,
                        random.nextInt(7) + 10), this.pos.now()));
    }

    @Override
    public boolean canMoveHere(TerrainServer terrain, int x, int y, int z) {
        if (terrain.getLight(x, y, z) >= 7) {
            if (!terrain.getBlockType(x, y, z).isSolid(terrain, x, y, z) &&
                    terrain.getBlockType(x, y, z)
                            .isTransparent(terrain, x, y, z) &&
                    terrain.getBlockType(x, y, z - 1)
                            .isSolid(terrain, x, y, z - 1) &&
                    !terrain.getBlockType(x, y, z - 1)
                            .isTransparent(terrain, x, y, z - 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.CREATURE;
    }

    @Override
    public Vector3 getViewOffset() {
        return new Vector3d(0.0, 0.0, 0.2);
    }

    @Override
    public void update(double delta) {
        // Movement
        if (swimming) {
            speed.plusZ(1.2);
            ground = false;
        }
        ai.update(delta);
        double walkSpeed = 0.0;
        if (ai.hasTarget()) {
            walkSpeed = 40.0;
            rot.setZ(ai.getTargetYaw());
        } else {
            walkWait -= delta;
            if (walkWait <= 0.0) {
                walkWait = 0.2;
                Vector3 dest = findWalkPosition();
                if (dest != null) {
                    ai.setPositionTarget(dest, 160.0);
                }
            }
        }
        lookWait -= delta;
        if (lookWait <= 0.0) {
            Random random = ThreadLocalRandom.current();
            lookWait = random.nextDouble() * 8.0 + 1.0;
            rot.setX(random.nextDouble() * 40.0 - 20.0);
        }
        if (!ground && !slidingWall && !inWater) {
            walkSpeed *= 0.0006;
        } else if (!ground && !inWater) {
            walkSpeed *= 0.05;
        } else if (inWater) {
            walkSpeed *= 0.2;
        }
        walkSpeed *= delta;
        speed.plusX(FastMath.cosTable(rot.doubleZ() * FastMath.DEG_2_RAD) *
                walkSpeed);
        speed.plusY(FastMath.sinTable(rot.doubleZ() * FastMath.DEG_2_RAD) *
                walkSpeed);
        soundWait -= delta;
        if (soundWait <= 0.0) {
            Random random = ThreadLocalRandom.current();
            soundWait = random.nextDouble() * 12.0 + 3.0;
            world.playSound("VanillaBasics:sound/entity/mob/pig/Calm" +
                    (random.nextInt(2) + 1) + ".ogg", this);
        }
    }

    private Vector3 findWalkPosition() {
        Random random = ThreadLocalRandom.current();
        Vector3 vector3d = pos.now().plus(new Vector3d(random.nextInt(17) - 8,
                random.nextInt(17) - 8, random.nextInt(7) - 3));
        if (canMoveHere(world.getTerrain(), vector3d.intX(), vector3d.intY(),
                vector3d.intZ())) {
            return vector3d;
        }
        return null;
    }
}
