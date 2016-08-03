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

package org.tobi29.scapes.entity.client;

import java8.util.Optional;
import org.tobi29.scapes.block.BlockExplosive;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.entity.model.MobModelBlock;

public class MobBombClient extends MobClient {
    private final ItemStack item;
    private double time;
    private boolean exploded;

    public MobBombClient(WorldClient world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, world.air(), 0, 0.0);
    }

    public MobBombClient(WorldClient world, Vector3 pos, Vector3 speed,
            BlockType type, int data, double time) {
        super(world, pos, speed, new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5));
        item = new ItemStack(type, data);
        this.time = time;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        item.load(tagStructure.getStructure("Block"));
        time = tagStructure.getDouble("Time");
    }

    @Override
    public Optional<MobModel> createModel() {
        return Optional.of(new MobModelBlock(this, item));
    }

    @Override
    public void update(double delta) {
        time -= delta;
        if (time < 0.05 && !exploded) { // TODO: Replace with proper packet
            ((BlockExplosive) item.material())
                    .explodeClient(world, pos.now(), speed.now());
            exploded = true;
        }
    }
}
