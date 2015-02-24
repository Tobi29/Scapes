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
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.packets.PacketBlockChange;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TerrainInfiniteClient extends TerrainInfinite
        implements TerrainClient {
    protected final TerrainInfiniteRenderer renderer;
    private final WorldClient world;
    private final MobPlayerClientMain player;
    private final TerrainInfiniteChunkManagerClient chunkManager;
    private final int loadingRadius, loadingRadiusSqr;
    protected int requestedChunks;

    public TerrainInfiniteClient(WorldClient world, int loadingRadius,
            int zSize, TaskExecutor taskExecutor) {
        super(zSize, new TerrainInfiniteChunkManagerClient(loadingRadius),
                taskExecutor, world.getAir());
        this.world = world;
        this.loadingRadius = loadingRadius;
        loadingRadiusSqr = loadingRadius * loadingRadius;
        player = world.getPlayer();
        renderer = new TerrainInfiniteRenderer(this, player,
                loadingRadius - 1 << 4);
        chunkManager = (TerrainInfiniteChunkManagerClient) super.chunkManager;
    }

    public int getRequestedChunks() {
        return requestedChunks;
    }

    public void changeRequestedChunks(int add) {
        requestedChunks += add;
    }

    @Override
    public WorldClient getWorld() {
        return world;
    }

    @Override
    public TerrainRenderer getTerrainRenderer() {
        return renderer;
    }

    @Override
    public void update(double delta) {
        Vector2i playerPos = new Vector2i(FastMath.floor(player.getX() / 16.0d),
                FastMath.floor(player.getY() / 16.0d));
        chunkManager.getIterator().stream().filter(chunk ->
                FastMath.pointDistanceSqr(playerPos, chunk.getPos()) <
                        loadingRadiusSqr).sorted((chunk1, chunk2) -> {
            double distance1 =
                    FastMath.pointDistanceSqr(chunk1.getPos(), playerPos),
                    distance2 = FastMath.pointDistanceSqr(chunk2.getPos(),
                            playerPos);
            return distance1 == distance2 ? 0 : distance1 > distance2 ? 1 : -1;
        }).forEach(TerrainInfiniteChunkClient::updateClient);
        int xx = FastMath.floor(player.getX() / 16.0d);
        int yy = FastMath.floor(player.getY() / 16.0d);
        chunkManager.setCenter(xx, yy);
        List<TerrainInfiniteChunkClient> dumpedChunks =
                chunkManager.getDumpedChunks();
        dumpedChunks.forEach(this::removeChunk);
        dumpedChunks.clear();
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
    public void processPacket(PacketBlockChange packet) {
        int x = packet.getX();
        int y = packet.getY();
        int z = packet.getZ();
        getChunk(FastMath.floor((double) x / 16),
                FastMath.floor((double) y / 16)).ifPresent(chunk -> chunk
                .setBlockIdAndData(x - (chunk.getX() << 4),
                        y - (chunk.getY() << 4), z,
                        world.getPlugins().getRegistry()
                                .getBlock(packet.getBlockId()),
                        packet.getBlockData()));
    }

    @Override
    public Optional<TerrainInfiniteChunkClient> getChunkNoLoad(int x, int y) {
        return chunkManager.get(x, y);
    }

    @Override
    public void dispose() {
        super.dispose();
        renderer.dispose();
    }

    @Override
    public Optional<TerrainInfiniteChunkClient> getChunk(int x, int y) {
        Optional<TerrainInfiniteChunkClient> chunk = chunkManager.get(x, y);
        if (chunk.isPresent()) {
            return chunk;
        }
        return addChunk(x, y);
    }

    @Override
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
                chunk = chunk2.getOptional();
            }
        }
        return chunk;
    }

    @Override
    public Collection<TerrainInfiniteChunkClient> getLoadedChunks() {
        return chunkManager.getIterator();
    }

    private void removeChunk(TerrainInfiniteChunkClient chunk) {
        int x = chunk.getX();
        int y = chunk.getY();
        chunkManager.remove(x, y);
        chunk.disposeClient();
    }
}
