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

package org.tobi29.scapes.block;

import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.entity.server.EntityContainerServer;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;

public abstract class BlockTypeContainer extends BlockType {
    protected BlockTypeContainer(GameRegistry registry, String nameID) {
        super(registry, nameID);
    }

    @Override
    public boolean click(TerrainServer terrain, int x, int y, int z, Face face,
            MobPlayerServer player) {
        for (EntityServer entity : terrain.getWorld().getEntities(x, y, z)) {
            if (entity instanceof EntityContainerServer) {
                player.openGui((EntityContainerServer) entity);
                return true;
            }
        }
        player.openGui(placeEntity(terrain, x, y, z));
        return true;
    }

    @Override
    public boolean causesTileUpdate() {
        return true;
    }

    protected abstract EntityContainerServer placeEntity(TerrainServer terrain,
            int x, int y, int z);
}
