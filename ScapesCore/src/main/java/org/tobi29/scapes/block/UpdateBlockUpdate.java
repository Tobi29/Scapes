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
package org.tobi29.scapes.block;

import org.tobi29.scapes.chunk.terrain.TerrainServer;

public class UpdateBlockUpdate extends Update {
    @Override
    public void run(TerrainServer.TerrainMutable terrain) {
        updateBlock(terrain, x, y, z, false);
        updateBlock(terrain, x - 1, y, z, false);
        updateBlock(terrain, x + 1, y, z, false);
        updateBlock(terrain, x, y - 1, z, false);
        updateBlock(terrain, x, y + 1, z, false);
        updateBlock(terrain, x, y, z - 1, false);
        updateBlock(terrain, x, y, z + 1, false);
    }

    @Override
    public boolean isValidOn(BlockType type, TerrainServer terrain) {
        return true;
    }

    protected void updateBlock(TerrainServer.TerrainMutable terrain, int x,
            int y, int z, boolean updateTile) {
        BlockType type = terrain.type(x, y, z);
        if (updateTile || type.causesTileUpdate()) {
            terrain.entities(x, y, z, stream -> stream
                    .forEach(entity -> entity.updateTile(terrain, x, y, z)));
        }
        type.update(terrain, x, y, z);
    }
}
