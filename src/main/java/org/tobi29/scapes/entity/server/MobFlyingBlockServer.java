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

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MobFlyingBlockServer extends MobServer {
    private final ItemStack item;
    private double time;

    public MobFlyingBlockServer(WorldServer world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, world.getAir(), 0);
    }

    public MobFlyingBlockServer(WorldServer world, Vector3 pos, Vector3 speed,
            BlockType type, int data) {
        super(world, pos, speed, new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5));
        item = new ItemStack(type, data);
        stepHeight = 0.0;
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        tag.setStructure("Block", item.save());
        tag.setDouble("Time", time);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        item.load(tagStructure.getStructure("Block"));
        time = tagStructure.getDouble("Time");
    }

    @Override
    public void update(double delta) {
        if (ground) {
            int x = pos.intX(), y = pos.intY(),
                    z = pos.intZ();
            if (!world.getTerrain().type(x, y, z)
                    .isReplaceable(world.getTerrain(), x, y, z)) {
                double xx = pos.intX() + 0.5, yy = pos.intY() + 0.5;
                if (pos.doubleX() == xx && pos.doubleY() == yy) {
                    Random random = ThreadLocalRandom.current();
                    xx += random.nextDouble() * delta - delta;
                    yy += random.nextDouble() * delta - delta;
                }
                push(pos.doubleX() - xx, pos.doubleY() - yy, 0.0);
                time += delta;
                if (time >= 5.0) {
                    pos.plusZ(1.0);
                    speed.setZ(1.0);
                }
            } else if (FastMath.abs(speed.doubleX()) < 0.1 &&
                    FastMath.abs(speed.doubleY()) < 0.1) {
                world.getTerrain().queue(handler -> handler
                        .typeData(x, y, z, (BlockType) item.getMaterial(),
                                item.getData()));
                world.deleteEntity(this);
            }
        }
    }
}
