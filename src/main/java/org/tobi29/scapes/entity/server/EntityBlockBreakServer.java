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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.packets.PacketEntityChange;

public class EntityBlockBreakServer extends EntityServer {
    private double progress, wait;

    public EntityBlockBreakServer(WorldServer world) {
        this(world, Vector3d.ZERO);
    }

    public EntityBlockBreakServer(WorldServer world, Vector3 pos) {
        super(world, pos);
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        tag.setDouble("Progress", progress);
        tag.setDouble("Wait", wait);
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        progress = tagStructure.getDouble("Progress");
        wait = tagStructure.getDouble("Wait");
    }

    @Override
    public void update(double delta) {
        wait += delta;
        if (wait >= 6.0 || progress >= 1.0) {
            world.deleteEntity(this);
        }
    }

    public boolean punch(WorldServer world, double strength) {
        wait = 0.0;
        progress = FastMath.clamp(progress + strength, 0.0, 1.0);
        world.getConnection().send(new PacketEntityChange(this));
        return progress >= 1.0;
    }
}
