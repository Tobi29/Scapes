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

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.io.tag.MultiTag;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.model.EntityModel;
import org.tobi29.scapes.packets.PacketEntityMetaData;

public class EntityClient implements MultiTag.Readable {
    protected final WorldClient world;
    protected final GameRegistry registry;
    protected final MutableVector3d pos;
    protected int entityID;
    protected TagStructure metaData = new TagStructure();

    protected EntityClient(WorldClient world, Vector3 pos) {
        this.world = world;
        registry = world.getRegistry();
        this.pos = new MutableVector3d(pos);
    }

    public static EntityClient make(int id, WorldClient world) {
        return world.getRegistry().<Supplier>get("Core", "EntityClient").get(id)
                .get(world);
    }

    public int getEntityID() {
        return entityID;
    }

    public void setEntityID(int entityID) {
        this.entityID = entityID;
    }

    public WorldClient getWorld() {
        return world;
    }

    public Vector3 getPos() {
        return pos.now();
    }

    public double getX() {
        return pos.doubleX();
    }

    public double getY() {
        return pos.doubleY();
    }

    public double getZ() {
        return pos.doubleZ();
    }

    @Override
    public void read(TagStructure tagStructure) {
        tagStructure.getMultiTag("Pos", pos);
        metaData = tagStructure.getStructure("MetaData");
    }

    public TagStructure getMetaData(String category) {
        return metaData.getStructure(category);
    }

    public void update(double delta) {
    }

    public EntityModel createModel() {
        return null;
    }

    public void processPacket(PacketEntityMetaData packet) {
        metaData.setStructure(packet.getCategory(), packet.getTag());
    }

    public interface Supplier {
        EntityClient get(WorldClient world);
    }
}
