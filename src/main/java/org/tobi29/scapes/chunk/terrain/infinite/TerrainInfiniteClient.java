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

package org.tobi29.scapes.chunk.terrain.infinite;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketBlockChange;

import java.util.*;
import java.util.stream.Collectors;

public class TerrainInfiniteClient extends TerrainInfinite
        implements TerrainClient {
    protected final TerrainInfiniteRenderer renderer;
    private final List<Vector2i> sortedLocations;
    private final WorldClient world;
    private final MobPlayerClientMain player;
    private final TerrainInfiniteChunkManagerClient chunkManager;
    private final int loadingRadius, loadingRadiusSqr;
    protected int requestedChunks;

    public TerrainInfiniteClient(WorldClient world, int loadingRadius,
            int zSize, TaskExecutor taskExecutor) {
        super(zSize, new TerrainInfiniteChunkManagerClient(loadingRadius),
                taskExecutor, world.air());
        this.world = world;
        this.loadingRadius = loadingRadius;
        loadingRadiusSqr = loadingRadius * loadingRadius;
        player = world.player();
        chunkManager = (TerrainInfiniteChunkManagerClient) super.chunkManager;
        List<Vector2i> locations = new ArrayList<>();
        int loadingRadiusSqr = FastMath.sqr(loadingRadius);
        for (int yy = -loadingRadius; yy <= loadingRadius; yy++) {
            for (int xx = -loadingRadius; xx <= loadingRadius; xx++) {
                if (xx * xx + yy * yy <= loadingRadiusSqr) {
                    locations.add(new Vector2i(xx, yy));
                }
            }
        }
        Collections.sort(locations, (vector1, vector2) -> {
            double distance1 =
                    FastMath.pointDistanceSqr(vector1, Vector2i.ZERO),
                    distance2 =
                            FastMath.pointDistanceSqr(vector2, Vector2i.ZERO);
            return distance1 == distance2 ? 0 : distance1 > distance2 ? 1 : -1;
        });
        sortedLocations = locations.stream().collect(Collectors.toList());
        renderer = new TerrainInfiniteRenderer(this, player, loadingRadius,
                sortedLocations);
    }

    public int requestedChunks() {
        return requestedChunks;
    }

    public void changeRequestedChunks(int add) {
        requestedChunks += add;
    }

    @Override
    public WorldClient world() {
        return world;
    }

    @Override
    public TerrainRenderer renderer() {
        return renderer;
    }

    @Override
    public void update(double delta) {
        int xx = FastMath.floor(player.x() / 16.0);
        int yy = FastMath.floor(player.y() / 16.0);
        for (Vector2i pos : sortedLocations) {
            Optional<TerrainInfiniteChunkClient> chunk =
                    chunkManager.get(pos.intX() + xx, pos.intY() + yy);
            if (chunk.isPresent()) {
                chunk.get().updateClient();
            }
        }
        chunkManager.setCenter(xx, yy);
        for (int x = -loadingRadius; x <= loadingRadius; x++) {
            int xxx = x + xx;
            for (int y = -loadingRadius; y <= loadingRadius; y++) {
                int yyy = y + yy;
                if (xxx >= cxMin && xxx <= cxMax && yyy >= cyMin &&
                        yyy <= cyMax) {
                    if (x * x + y * y < loadingRadiusSqr) {
                        if (!chunkManager.get(xxx, yyy).isPresent()) {
                            addChunk(xxx, yyy);
                        }
                    } else {
                        chunkManager.get(xxx, yyy).ifPresent(this::removeChunk);
                    }
                }
            }
        }
    }

    @Override
    public void toggleStaticRenderDistance() {
        renderer.toggleStaticRenderDistance();
    }

    @Override
    public void reloadGeometry() {
        renderer.reloadGeometry();
    }

    @Override
    public void process(PacketBlockChange packet) {
        int x = packet.x();
        int y = packet.y();
        int z = packet.z();
        chunk(x >> 4, y >> 4).ifPresent(chunk -> chunk.typeDataG(x, y, z,
                world.plugins().registry().block(packet.id()), packet.data()));
    }

    @Override
    public void dispose(GL gl) {
        lighting.dispose();
        chunkManager.iterator().forEach(this::removeChunk);
        renderer.dispose(gl);
    }

    @Override
    public Optional<TerrainInfiniteChunkClient> chunkNoLoad(int x, int y) {
        return chunkManager.get(x, y);
    }

    @Override
    public Optional<TerrainInfiniteChunkClient> chunk(int x, int y) {
        return chunkManager.get(x, y);
    }

    @Override
    public Collection<TerrainInfiniteChunkClient> loadedChunks() {
        return chunkManager.iterator();
    }

    public Optional<TerrainInfiniteChunkClient> addChunk(int x, int y) {
        if (x < cxMin || x > cxMax || y < cyMin || y > cyMax) {
            return Optional.empty();
        }
        Optional<TerrainInfiniteChunkClient> chunk;
        synchronized (chunkManager) {
            chunk = chunkManager.get(x, y);
            if (!chunk.isPresent()) {
                TerrainInfiniteChunkClient chunk2 =
                        new TerrainInfiniteChunkClient(new Vector2i(x, y), this,
                                zSize, renderer);
                chunkManager.add(chunk2);
                chunk = chunk2.optional();
            }
        }
        return chunk;
    }

    private void removeChunk(TerrainInfiniteChunkClient chunk) {
        int x = chunk.x();
        int y = chunk.y();
        chunkManager.remove(x, y);
    }
}
