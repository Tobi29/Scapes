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

package org.tobi29.scapes.chunk.lighting;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3i;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LightingEngineThreaded
        implements LightingEngine, TaskExecutor.ASyncTask {
    private final Terrain terrain;
    private final Queue<Vector3> updates = new ConcurrentLinkedQueue<>();
    private final Joiner joiner;

    public LightingEngineThreaded(Terrain terrain, TaskExecutor taskExecutor) {
        this.terrain = terrain;
        joiner = taskExecutor.runTask(this, "Lighting-Engine");
    }

    @Override
    public void updateLight(int x, int y, int z) {
        updates.add(new Vector3i(x, y, z));
    }

    @Override
    public void initLight(int x, int y, int dx, int dy) {
        int maxx = x + dx;
        int maxy = y + dy;
        Pool<LightSpread> spreads = new Pool<>(LightSpread::new);
        Pool<LightSpread> newSpreads = new Pool<>(LightSpread::new);
        for (int xx = x; xx < maxx; xx++) {
            for (int yy = y; yy < maxy; yy++) {
                int spread = terrain.getHighestBlockZAt(xx - 1, yy);
                spread = FastMath.max(terrain.getHighestBlockZAt(xx + 1, yy),
                        spread);
                spread = FastMath.max(terrain.getHighestBlockZAt(xx, yy - 1),
                        spread);
                spread = FastMath.max(terrain.getHighestBlockZAt(xx, yy + 1),
                        spread);
                for (int zz = spread; zz >= 0; zz--) {
                    int light = terrain.getSunLight(xx, yy, zz);
                    if (light > 0) {
                        spreads.push().set(xx - 1, yy, zz, light);
                        spreads.push().set(xx + 1, yy, zz, light);
                        spreads.push().set(xx, yy - 1, zz, light);
                        spreads.push().set(xx, yy + 1, zz, light);
                    }
                }
            }
        }
        while (!spreads.isEmpty()) {
            for (LightSpread s : spreads) {
                if (terrain.isBlockAvailable(s.x, s.y, s.z)) {
                    s.l += terrain.getBlockType(s.x, s.y, s.z)
                            .lightTrough(terrain, s.x, s.y, s.z);
                    s.l = FastMath.clamp(s.l, (byte) 0, (byte) 15);
                    if (s.l > terrain.getSunLight(s.x, s.y, s.z)) {
                        terrain.setSunLight(s.x, s.y, s.z, s.l);
                        newSpreads.push().set(s.x - 1, s.y, s.z, s.l);
                        newSpreads.push().set(s.x + 1, s.y, s.z, s.l);
                        newSpreads.push().set(s.x, s.y - 1, s.z, s.l);
                        newSpreads.push().set(s.x, s.y + 1, s.z, s.l);
                        newSpreads.push().set(s.x, s.y, s.z - 1, s.l);
                        newSpreads.push().set(s.x, s.y, s.z + 1, s.l);
                    }
                }
            }
            Pool<LightSpread> swapUpdates = spreads;
            swapUpdates.reset();
            spreads = newSpreads;
            newSpreads = swapUpdates;
        }
    }

    @Override
    public void dispose() {
        joiner.join();
    }

    private void updateBlockLight(Pool<MutableVector3> updates,
            Pool<MutableVector3> newUpdates, int x, int y, int z) {
        updates.push().set(x, y, z);
        while (!updates.isEmpty()) {
            for (MutableVector3 update : updates) {
                if (terrain.isBlockLoaded(update.intX(), update.intY(),
                        update.intZ())) {
                    BlockType type =
                            terrain.getBlockType(update.intX(), update.intY(),
                                    update.intZ());
                    byte lightTrough = type.lightTrough(terrain, update.intX(),
                            update.intY(), update.intZ());
                    byte light = type.lightEmit(terrain, update.intX(),
                            update.intY(), update.intZ());
                    light = (byte) FastMath.max(terrain
                            .getBlockLight(update.intX() - 1, update.intY(),
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getBlockLight(update.intX() + 1, update.intY(),
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getBlockLight(update.intX(), update.intY() - 1,
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getBlockLight(update.intX(), update.intY() + 1,
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getBlockLight(update.intX(), update.intY(),
                                    update.intZ() - 1) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getBlockLight(update.intX(), update.intY(),
                                    update.intZ() + 1) + lightTrough, light);
                    light = FastMath.clamp(light, (byte) 0, (byte) 15);
                    if (light !=
                            terrain.getBlockLight(update.intX(), update.intY(),
                                    update.intZ())) {
                        terrain.setBlockLight(update.intX(), update.intY(),
                                update.intZ(), light);
                        newUpdates.push().set(update.intX() - 1, update.intY(),
                                update.intZ());
                        newUpdates.push().set(update.intX() + 1, update.intY(),
                                update.intZ());
                        newUpdates.push().set(update.intX(), update.intY() - 1,
                                update.intZ());
                        newUpdates.push().set(update.intX(), update.intY() + 1,
                                update.intZ());
                        newUpdates.push().set(update.intX(), update.intY(),
                                update.intZ() - 1);
                        newUpdates.push().set(update.intX(), update.intY(),
                                update.intZ() + 1);
                    }
                }
            }
            Pool<MutableVector3> swapUpdates = updates;
            swapUpdates.reset();
            updates = newUpdates;
            newUpdates = swapUpdates;
        }
    }

    private void updateSunLight(Pool<MutableVector3> updates,
            Pool<MutableVector3> newUpdates, int x, int y, int z) {
        updates.push().set(x, y, z);
        while (!updates.isEmpty()) {
            for (MutableVector3 update : updates) {
                if (terrain.isBlockLoaded(update.intX(), update.intY(),
                        update.intZ())) {
                    BlockType type =
                            terrain.getBlockType(update.intX(), update.intY(),
                                    update.intZ());
                    byte lightTrough = type.lightTrough(terrain, update.intX(),
                            update.intY(), update.intZ());
                    byte light = calcSunLightAt(update.intX(), update.intY(),
                            update.intZ());
                    light = (byte) FastMath.max(terrain
                            .getSunLight(update.intX() - 1, update.intY(),
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getSunLight(update.intX() + 1, update.intY(),
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getSunLight(update.intX(), update.intY() - 1,
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getSunLight(update.intX(), update.intY() + 1,
                                    update.intZ()) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getSunLight(update.intX(), update.intY(),
                                    update.intZ() - 1) + lightTrough, light);
                    light = (byte) FastMath.max(terrain
                            .getSunLight(update.intX(), update.intY(),
                                    update.intZ() + 1) + lightTrough, light);
                    light = FastMath.clamp(light, (byte) 0, (byte) 15);
                    if (light !=
                            terrain.getSunLight(update.intX(), update.intY(),
                                    update.intZ())) {
                        terrain.setSunLight(update.intX(), update.intY(),
                                update.intZ(), light);
                        newUpdates.push().set(update.intX() - 1, update.intY(),
                                update.intZ());
                        newUpdates.push().set(update.intX() + 1, update.intY(),
                                update.intZ());
                        newUpdates.push().set(update.intX(), update.intY() - 1,
                                update.intZ());
                        newUpdates.push().set(update.intX(), update.intY() + 1,
                                update.intZ());
                        newUpdates.push().set(update.intX(), update.intY(),
                                update.intZ() - 1);
                        newUpdates.push().set(update.intX(), update.intY(),
                                update.intZ() + 1);
                    }
                }
            }
            Pool<MutableVector3> swapUpdates = updates;
            swapUpdates.reset();
            updates = newUpdates;
            newUpdates = swapUpdates;
        }
    }

    private byte calcSunLightAt(int x, int y, int z) {
        byte sunLight = 15;
        for (int zz = terrain.getHighestBlockZAt(x, y); zz >= z && zz >= 0;
                zz--) {
            BlockType type = terrain.getBlockType(x, y, zz);
            if (type.isSolid(terrain, x, y, zz) ||
                    !type.isTransparent(terrain, x, y, zz)) {
                sunLight = FastMath.clamp(
                        (byte) (sunLight + type.lightTrough(terrain, x, y, zz)),
                        (byte) 0, (byte) 15);
            }
        }
        return sunLight;
    }

    @Override
    public void run(Joiner joiner) {
        while (!joiner.marked()) {
            Pool<MutableVector3> updates = new Pool<>(MutableVector3i::new);
            Pool<MutableVector3> newUpdates = new Pool<>(MutableVector3i::new);
            while (!this.updates.isEmpty()) {
                Vector3 update = this.updates.poll();
                if (update != null) {
                    updateBlockLight(updates, newUpdates, update.intX(),
                            update.intY(), update.intZ());
                    updateSunLight(updates, newUpdates, update.intX(),
                            update.intY(), update.intZ());
                }
            }
            SleepUtil.sleep(10);
        }
    }

    private static class LightSpread {
        private int x, y, z, l;

        private void set(int x, int y, int z, int l) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.l = l;
        }
    }
}
