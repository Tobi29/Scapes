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

package org.tobi29.scapes.chunk.terrain;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.chunk.MobSpawner;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;

import java.util.Collection;

public interface TerrainServer extends Terrain {
    WorldServer world();

    void update(double delta, Collection<MobSpawner> spawners);

    void queue(BlockChanges blockChanges);

    void addDelayedUpdate(Update update);

    boolean hasDelayedUpdate(int x, int y, int z);

    boolean isBlockSendable(MobPlayerServer player, int x, int y, int z,
            boolean chunkContent);

    void dispose();

    interface BlockChanges {
        void run(TerrainMutable handler);
    }

    interface TerrainMutable extends TerrainServer {
        void type(int x, int y, int z, BlockType type);

        void data(int x, int y, int z, int data);

        void typeData(int x, int y, int z, BlockType block, int data);
    }
}
