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
import org.tobi29.scapes.entity.server.MobLivingEquippedServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobZombieServer extends MobLivingEquippedServer {
    private double soundWait, lookWait, walkWait, hitWait;

    public MobZombieServer(WorldServer world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, 0.0d, 0.0d);
    }

    public MobZombieServer(WorldServer world, Vector3 pos, Vector3 speed,
            double xRot, double zRot) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 20.0d,
                30.0d, new Frustum(90, 1, 0.1, 24),
                new Frustum(20, 0.5, 0.1, 0.2));
        rot.setX(xRot);
        rot.setZ(zRot);
        Random random = ThreadLocalRandom.current();
        listener((NoticeListener) mob -> {
            if (mob instanceof MobPlayerServer && !ai.hasMobTarget()) {
                ai.setMobTarget(mob, 10.0);
            }
        });
        listener((DamageListener) damage -> world
                .playSound("VanillaBasics:sound/entity/mob/skeleton/Hurt" +
                        (random.nextInt(3) + 1) + ".ogg", this));
    }

    @Override
    public boolean canMoveHere(TerrainServer terrain, int x, int y, int z) {
        if (terrain.light(x, y, z) < 7) {
            if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                    terrain.type(x, y, z)
                            .isTransparent(terrain, x, y, z) &&
                    !terrain.type(x, y, z + 1)
                            .isSolid(terrain, x, y, z + 1) &&
                    terrain.type(x, y, z + 1)
                            .isTransparent(terrain, x, y, z + 1) &&
                    terrain.type(x, y, z - 1)
                            .isSolid(terrain, x, y, z - 1) &&
                    !terrain.type(x, y, z - 1)
                            .isTransparent(terrain, x, y, z - 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTER;
    }

    @Override
    public Vector3 getViewOffset() {
        return new Vector3d(0.0, 0.0, 0.7);
    }

    @Override
    public void update(double delta) {
        if (swimming) {
            speed.plusZ(1.2);
            ground = false;
        }
        ai.update(delta);
        double walkSpeed = 0.0;
        hitWait -= delta;
        if (hitWait <= 0.0) {
            hitWait = 0.5;
            attack(30.0);
        }
        if (ai.hasTarget()) {
            walkSpeed = 60.0;
            rot.setZ(ai.getTargetYaw());
        } else {
            walkWait -= delta;
            if (walkWait <= 0.0) {
                walkWait = 0.2;
                findWalkPosition()
                        .ifPresent(dest -> ai.setPositionTarget(dest, 160.0));
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
            world.playSound("VanillaBasics:sound/entity/mob/zombie/Calm" +
                    (random.nextInt(2) + 1) + ".ogg", this);
        }
        if (world.getTerrain().light(pos.intX(), pos.intY(),
                FastMath.floor(pos.doubleZ() + 0.7)) > 10) {
            damage(0.5);
        }
    }

    @Override
    public ItemStack getLeftWeapon() {
        return new ItemStack(registry);
    }

    @Override
    public ItemStack getRightWeapon() {
        return new ItemStack(registry);
    }

    private Optional<Vector3> findWalkPosition() {
        Random random = ThreadLocalRandom.current();
        Vector3 vector3d = pos.now().plus(new Vector3d(random.nextInt(17) - 8,
                random.nextInt(17) - 8, random.nextInt(7) - 3));
        if (canMoveHere(world.getTerrain(), vector3d.intX(), vector3d.intY(),
                vector3d.intZ())) {
            return Optional.of(vector3d);
        }
        return Optional.empty();
    }
}
