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
package org.tobi29.scapes.entity.server;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;

public class MobItemServer extends MobServer {
    private final ItemStack item;
    private double pickupwait = 1.0, despawntime, stackwait;

    public MobItemServer(WorldServer world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO,
                new ItemStack(world.registry()), Double.NaN);
    }

    public MobItemServer(WorldServer world, Vector3 pos, Vector3 speed,
            ItemStack item, double despawntime) {
        super(world, pos, speed, new AABB(-0.2, -0.2, -0.2, 0.2, 0.2, 0.2));
        this.item = new ItemStack(item);
        this.despawntime = despawntime;
        stepHeight = 0.0;
    }

    @Override
    public TagStructure write() {
        TagStructure tagStructure = super.write();
        item.save();
        tagStructure.setStructure("Inventory", item.save());
        tagStructure.setDouble("Pickupwait", pickupwait);
        tagStructure.setDouble("Despawntime", despawntime);
        return tagStructure;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        item.load(tagStructure.getStructure("Inventory"));
        pickupwait = tagStructure.getDouble("Pickupwait");
        despawntime = tagStructure.getDouble("Despawntime");
    }

    public ItemStack item() {
        return item;
    }

    @Override
    public void update(double delta) {
        if (pickupwait <= 0) {
            AABB aabb = aabb().grow(0.8, 0.8, 0.4);
            Streams.forEach(world.players(),
                    entity -> aabb.overlay(entity.aabb()), entity -> {
                        world.playSound("Scapes:sound/entity/mob/Item.ogg",
                                this);
                        entity.inventories().modify("Container",
                                inventory -> item.setAmount(
                                        item.amount() - inventory.add(item)));
                    });
            stackwait -= delta;
            if (stackwait <= 0) {
                aabb.grow(0.0, 0.0, 0.4);
                world.entities(aabb, stream -> stream
                        .filter(entity -> entity instanceof MobItemServer &&
                                entity != this)
                        .map(entity -> (MobItemServer) entity).forEach(
                                entity -> item.setAmount(item.amount() -
                                        entity.item.stack(item))));
                stackwait = 1.0;
            }
        } else {
            pickupwait -= delta;
        }
        if (item.amount() <= 0 || item.material() == registry.air()) {
            world.removeEntity(this);
        }
        if (!Double.isNaN(despawntime)) {
            despawntime -= delta;
            if (despawntime <= 0) {
                world.removeEntity(this);
            }
        }
    }
}
