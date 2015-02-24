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

package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.*;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.packets.PacketMobDamage;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public abstract class MobLivingClient extends MobClient {
    protected final Frustum viewField, hitField;
    protected double lives, maxLives, armor = 1.0, footStep, invincibleTicks;

    protected MobLivingClient(WorldClient world, Vector3 pos, Vector3 speed,
            AABB aabb, double lives, double maxLives, Frustum viewField,
            Frustum hitField) {
        super(world, pos, speed, aabb);
        this.lives = lives;
        this.maxLives = maxLives;
        this.viewField = viewField;
        this.hitField = hitField;
    }

    public PointerPane getBlock(double distance) {
        Pool<PointerPane> pointerPanes = world.getTerrain()
                .getPointerPanes(pos.intX(), pos.intY(), pos.intZ(),
                        (int) FastMath.ceil(distance));
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * distance;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * distance;
        double lookZ =
                FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * distance;
        Vector3 viewOffset = getViewOffset();
        Vector3 f = pos.now().plus(viewOffset);
        Vector3 t = f.plus(new Vector3d(lookX, lookY, lookZ));
        double distanceSqr = distance * distance;
        PointerPane closest = null;
        for (PointerPane pane : pointerPanes) {
            Optional<Intersection> intersection =
                    Intersection.intersectPointerPane(f, t, pane);
            if (intersection.isPresent()) {
                double check =
                        FastMath.pointDistance(f, intersection.get().getPos());
                if (check < distanceSqr) {
                    closest = pane;
                    distanceSqr = check;
                }
            }
        }
        pointerPanes.reset();
        return closest;
    }

    public void onNotice(MobClient notice) {
    }

    public void onHeal(double heal) {
    }

    public void onDamage(double damage) {
    }

    public void onDeath() {
    }

    public abstract CreatureType getCreatureType();

    public double getLives() {
        return lives;
    }

    public double getMaxLives() {
        return maxLives;
    }

    public boolean isDead() {
        return lives <= 0;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        lives = tagStructure.getDouble("Lives");
        maxLives = tagStructure.getDouble("MaxLives");
        armor = tagStructure.getDouble("Armor");
    }

    @Override
    public void move(double delta) {
        super.move(delta);
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookZ = FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * 6;
        Vector3 viewOffset = getViewOffset();
        viewField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        world.getEntities().filter(entity -> entity instanceof MobClient)
                .forEach(entity -> {
                    MobClient mob = (MobClient) entity;
                    if (viewField.inView(mob.getAABB()) > 0) {
                        if (!world.checkBlocked(pos.intX(), pos.intY(),
                                pos.intZ(), mob.pos.intX(), mob.pos.intY(),
                                mob.pos.intZ())) {
                            onNotice(mob);
                        }
                    }
                });
        footStep -= delta;
        if (footStep <= 0.0) {
            footStep = 0.0;
            if (FastMath.max(FastMath.abs((Vector2) speed.now())) > 0.1) {
                int x = pos.intX(), y = pos.intY(), z =
                        FastMath.floor(pos.doubleZ() - 0.1);
                String footSteepSound = world.getTerrain().getBlockType(x, y, z)
                        .getFootStep(world.getTerrain().getBlockData(x, y, z));
                if (footSteepSound.isEmpty() && ground) {
                    z = FastMath.floor(pos.doubleZ() - 1.4);
                    footSteepSound = world.getTerrain().getBlockType(x, y, z)
                            .getFootStep(
                                    world.getTerrain().getBlockData(x, y, z));
                }
                if (!footSteepSound.isEmpty()) {
                    Random random = ThreadLocalRandom.current();
                    world.playSound(footSteepSound, this,
                            0.9f + random.nextFloat() * 0.2f, 1.0f, 1.0f);
                    footStep = 1.0 /
                            FastMath.clamp(FastMath.length(speed.now()), 1.0,
                                    4.0);
                }
            }
        }
        if (invincibleTicks >= 0.0) {
            invincibleTicks = FastMath.max(invincibleTicks - delta, 0.0);
        }
    }

    public abstract Vector3 getViewOffset();

    public double getInvincibleTicks() {
        return invincibleTicks;
    }

    public void processPacket(PacketMobDamage packet) {
        maxLives = packet.getMaxLives();
        double newLives = packet.getLives();
        double oldLives = lives;
        if (newLives < oldLives) {
            invincibleTicks = 0.8;
            lives = newLives;
            onDamage(oldLives - newLives);
        } else {
            lives = newLives;
            onHeal(newLives - oldLives);
        }
    }
}
