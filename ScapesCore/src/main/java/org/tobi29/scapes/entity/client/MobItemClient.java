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

import java8.util.Optional;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.entity.model.MobModelItem;

public class MobItemClient extends MobClient {
    private final ItemStack item;

    public MobItemClient(WorldClient world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO,
                new ItemStack(world.registry()));
    }

    public MobItemClient(WorldClient world, Vector3 pos, Vector3 speed,
            ItemStack item) {
        super(world, pos, speed, new AABB(-0.2, -0.2, -0.2, 0.2, 0.2, 0.2));
        this.item = new ItemStack(item);
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        item.load(tagStructure.getStructure("Inventory"));
    }

    @Override
    public Optional<MobModel> createModel() {
        return Optional.of(new MobModelItem(this, item));
    }

    public ItemStack item() {
        return item;
    }
}
