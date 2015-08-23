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

package org.tobi29.scapes.entity.server;

import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.*;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.ai.AI;
import org.tobi29.scapes.entity.ai.SimpleAI;
import org.tobi29.scapes.packets.PacketMobDamage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MobLivingServer extends MobServer {
    protected final Map<String, NoticeListener> noticeListeners =
            new ConcurrentHashMap<>();
    protected final Map<String, JumpListener> jumpListeners =
            new ConcurrentHashMap<>();
    protected final Map<String, HealListener> healListeners =
            new ConcurrentHashMap<>();
    protected final Map<String, DamageListener> damageListeners =
            new ConcurrentHashMap<>();
    protected final Map<String, DeathListener> deathListeners =
            new ConcurrentHashMap<>();
    protected final Frustum viewField, hitField;
    protected final AI ai;
    protected double lastDamage, health, maxHealth, invincibleTicks;

    protected MobLivingServer(WorldServer world, Vector3 pos, Vector3 speed,
            AABB aabb, double health, double maxHealth, Frustum viewField,
            Frustum hitField) {
        super(world, pos, speed, aabb);
        this.health = health;
        this.maxHealth = maxHealth;
        this.viewField = viewField;
        this.hitField = hitField;
        ai = createAI();
    }

    protected AI createAI() {
        return new SimpleAI(this);
    }

    public synchronized List<MobServer> attack(double damage) {
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookZ = FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * 6;
        Vector3 viewOffset = viewOffset();
        hitField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        List<MobServer> mobs = world.damageEntities(
                Collections.singletonList((MobServer) this), hitField, damage);
        mobs.stream().filter(mob -> mob instanceof MobLivingServer)
                .map(mob -> (MobLivingServer) mob).forEach(mob -> {
            mob.onNotice(this);
            double rad = rot.doubleZ() * FastMath.DEG_2_RAD;
            mob.push(FastMath.cosTable(rad) * 10.0,
                    FastMath.sinTable(rad) * 10.0, 2.0);
        });
        return mobs;
    }

    public PointerPane block(double distance) {
        Pool<PointerPane> pointerPanes = world.getTerrain()
                .pointerPanes(pos.intX(), pos.intY(), pos.intZ(),
                        (int) FastMath.ceil(distance));
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * distance;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * distance;
        double lookZ =
                FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * distance;
        Vector3 viewOffset = viewOffset();
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

    public abstract boolean canMoveHere(TerrainServer terrain, int x, int y,
            int z);

    public abstract CreatureType creatureType();

    public void damage(double damage) {
        damage(damage, false);
    }

    public void damage(double damage, boolean ignoreInvincible) {
        double d = damage;
        if (invincibleTicks > 0.0 && !ignoreInvincible) {
            if (damage > lastDamage) {
                d = damage - lastDamage;
                lastDamage = damage;
            } else {
                d = 0;
            }
        } else {
            lastDamage = damage;
            invincibleTicks = 0.8;
        }
        health -= d;
        if (health > maxHealth) {
            health = maxHealth;
        }
        if (health < 0) {
            health = 0;
        }
        if (d != 0) {
            onDamage(damage);
            world.send(new PacketMobDamage(this));
        }
    }

    public double health() {
        return health;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public void heal(double heal) {
        health += heal;
        if (health > maxHealth) {
            health = maxHealth;
        }
        if (health < 0) {
            health = 0;
        }
        onHeal(heal);
        world.send(new PacketMobDamage(this));
    }

    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        tag.setDouble("Health", health);
        tag.setDouble("MaxHealth", maxHealth);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        health = tagStructure.getDouble("Health");
        maxHealth = tagStructure.getDouble("MaxHealth");
    }

    @Override
    public void move(double delta) {
        super.move(delta);
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookZ = FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * 6;
        Vector3 viewOffset = viewOffset();
        viewField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        world.entities().filter(entity -> entity instanceof MobServer)
                .forEach(entity -> {
                    MobServer mob = (MobServer) entity;
                    if (viewField.inView(mob.aabb()) > 0) {
                        if (!world.checkBlocked(pos.intX(), pos.intY(),
                                pos.intZ(), mob.pos.intX(), mob.pos.intY(),
                                mob.pos.intZ())) {
                            onNotice(mob);
                        }
                    }
                });
        if (invincibleTicks >= 0.0) {
            invincibleTicks = FastMath.max(invincibleTicks - delta, 0.0);
        }
    }

    public abstract Vector3 viewOffset();

    @Override
    public void listener(String id, EntityServer.Listener listener) {
        super.listener(id, listener);
        if (listener instanceof NoticeListener) {
            noticeListeners.put(id, (NoticeListener) listener);
        }
        if (listener instanceof JumpListener) {
            jumpListeners.put(id, (JumpListener) listener);
        }
        if (listener instanceof HealListener) {
            healListeners.put(id, (HealListener) listener);
        }
        if (listener instanceof DamageListener) {
            damageListeners.put(id, (DamageListener) listener);
        }
        if (listener instanceof DeathListener) {
            deathListeners.put(id, (DeathListener) listener);
        }
    }

    public void onNotice(MobServer mob) {
        noticeListeners.values().forEach(listener -> listener.onNotice(mob));
    }

    public void onJump() {
        jumpListeners.values().forEach(JumpListener::onJump);
    }

    public void onHeal(double amount) {
        healListeners.values().forEach(listener -> listener.onHeal(amount));
    }

    public void onDamage(double amount) {
        damageListeners.values().forEach(listener -> listener.onDamage(amount));
    }

    public void onDeath() {
        deathListeners.values().forEach(DeathListener::onDeath);
    }

    @FunctionalInterface
    public interface NoticeListener extends EntityServer.Listener {
        void onNotice(MobServer mob);
    }

    @FunctionalInterface
    public interface JumpListener extends EntityServer.Listener {
        void onJump();
    }

    @FunctionalInterface
    public interface HealListener extends EntityServer.Listener {
        void onHeal(double amount);
    }

    @FunctionalInterface
    public interface DamageListener extends EntityServer.Listener {
        void onDamage(double amount);
    }

    @FunctionalInterface
    public interface DeathListener extends EntityServer.Listener {
        void onDeath();
    }
}
