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

package org.tobi29.scapes.vanilla.basics.entity.server;

import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class EntityBellowsServer extends EntityServer {
    private float scale;
    private Face face;

    public EntityBellowsServer(WorldServer world) {
        this(world, Vector3d.ZERO, Face.NONE);
    }

    public EntityBellowsServer(WorldServer world, Vector3 pos, Face face) {
        super(world, pos);
        this.face = face;
    }

    @Override
    public TagStructure write() {
        TagStructure tagStructure = super.write();
        tagStructure.setFloat("Scale", scale);
        tagStructure.setByte("Face", face.getData());
        return tagStructure;
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
    public void updateTile(TerrainServer terrain, int x, int y, int z) {
        WorldServer world = terrain.getWorld();
        VanillaBasics plugin =
                (VanillaBasics) world.getPlugins().getPlugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        if (terrain.getBlockType(pos.intX(), pos.intY(), pos.intZ()) !=
                materials.bellows) {
            world.deleteEntity(this);
        }
    }

    public Face getFace() {
        return face;
    }
}
