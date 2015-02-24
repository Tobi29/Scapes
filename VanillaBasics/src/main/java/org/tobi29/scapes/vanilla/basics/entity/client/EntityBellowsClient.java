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

package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.vanilla.basics.entity.model.EntityModelBellows;

import java.util.Optional;

public class EntityBellowsClient extends EntityClient {
    private float scale;
    private Face face;

    public EntityBellowsClient(WorldClient world) {
        this(world, Vector3d.ZERO, Face.NONE);
    }

    public EntityBellowsClient(WorldClient world, Vector3 pos, Face face) {
        super(world, pos);
        this.face = face;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        scale = tagStructure.getFloat("Scale");
        face = Face.get(tagStructure.getByte("Face"));
    }

    @Override
    public void update(double delta) {
        scale += 0.4f * delta;
        scale %= 2;
    }

    @Override
    public Optional<EntityModel> createModel() {
        return Optional.of(new EntityModelBellows(this));
    }

    public float getScale() {
        return scale;
    }

    public Face getFace() {
        return face;
    }
}
