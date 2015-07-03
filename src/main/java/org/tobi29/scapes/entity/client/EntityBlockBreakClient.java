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
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.entity.model.EntityModelBlockBreak;

import java.util.Optional;

public class EntityBlockBreakClient extends EntityClient {
    private final Pool<PointerPane> pointerPanes = new Pool<>(PointerPane::new);
    private double progress;

    public EntityBlockBreakClient(WorldClient world) {
        this(world, Vector3d.ZERO);
    }

    public EntityBlockBreakClient(WorldClient world, Vector3 pos) {
        super(world, pos);
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        progress = tagStructure.getDouble("Progress");
    }

    @Override
    public void update(double delta) {
        pointerPanes.reset();
        world.getTerrain().type(pos.intX(), pos.intY(), pos.intZ())
                .addPointerCollision(world.getTerrain()
                                .data(pos.intX(), pos.intY(), pos.intZ()),
                        pointerPanes, pos
                                .intX(),
                        pos.intY(), pos.intZ());
    }

    @Override
    public Optional<EntityModel> createModel() {
        return Optional.of(new EntityModelBlockBreak(this));
    }

    public double getProgress() {
        return progress;
    }

    public Pool<PointerPane> getPointerPanes() {
        return pointerPanes;
    }
}
