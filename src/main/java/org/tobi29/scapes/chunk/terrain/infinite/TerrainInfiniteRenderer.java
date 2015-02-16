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

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainRenderer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TerrainInfiniteRenderer implements TerrainRenderer {
    private final List<Vector2i> sortedLocations;
    private final TerrainInfiniteRendererThread updateThread, loadThread;
    private final TerrainInfiniteClient terrain;
    private final double chunkDistanceMax;
    private final boolean keepInvisibleChunkVbos;
    private final List<TerrainInfiniteRendererChunk> chunks = new ArrayList<>();
    private final Joiner joiner;
    private int playerX, playerY, playerZ;
    private double chunkDistance;
    private boolean disposed, staticRenderDistance, updateVisible;
    private Cam cam;
    private Pool<VisibleUpdate> cullingPool1 = new Pool<>(VisibleUpdate::new),
            cullingPool2 = new Pool<>(VisibleUpdate::new);

    public TerrainInfiniteRenderer(TerrainInfiniteClient terrain,
            MobPlayerClientMain player, double chunkDistance) {
        this.terrain = terrain;
        chunkDistanceMax = FastMath.sqr(chunkDistance);
        Queue<TerrainInfiniteRendererChunk> loadQueue =
                new ConcurrentLinkedQueue<>();
        Queue<TerrainInfiniteRendererChunk> updateQueue =
                new ConcurrentLinkedQueue<>();
        updateThread =
                new TerrainInfiniteRendererThread(updateQueue, loadQueue, true);
        loadThread = new TerrainInfiniteRendererThread(loadQueue, updateQueue,
                false);
        keepInvisibleChunkVbos = player.getGame().getEngine().getTagStructure()
                .getStructure("Scapes").getBoolean("KeepInvisibleChunkVbos");
        int size = (int) FastMath.ceil(chunkDistance / 16.0);
        List<Vector2i> locations = new ArrayList<>();
        for (int yy = -size; yy <= size; yy++) {
            for (int xx = -size; xx <= size; xx++) {
                if (xx * xx + yy * yy <= chunkDistanceMax) {
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
        Joiner updateJoiner = player.getGame().getEngine().getTaskExecutor()
                .runTask(updateThread, "TerrainInfiniteChunk-Geometry-Update");
        Joiner loadJoiner = player.getGame().getEngine().getTaskExecutor()
                .runTask(loadThread, "TerrainInfiniteChunk-Geometry-Load");
        joiner = new Joiner(updateJoiner, loadJoiner);
    }

    public void toggleStaticRenderDistance() {
        staticRenderDistance = !staticRenderDistance;
    }

    public void reloadGeometry() {
        terrain.getLoadedChunks()
                .forEach(chunk -> chunk.getRendererChunk().reset());
    }

    public void dispose() {
        joiner.join();
        disposed = true;
    }

    public void addToUpdateQueue(TerrainInfiniteRendererChunk chunk) {
        if (chunk == null ||
                chunk.getChunk().isDisposed() || !chunk.getChunk().isLoaded() ||
                disposed) {
            return;
        }
        updateThread.queue.add(chunk);
    }

    public void addToLoadQueue(TerrainInfiniteRendererChunk chunk) {
        if (chunk == null ||
                chunk.getChunk().isDisposed() || !chunk.getChunk().isLoaded() ||
                disposed) {
            return;
        }
        loadThread.queue.add(chunk);
    }

    @Override
    public void renderUpdate(GraphicsSystem graphics, Cam cam) {
        if (disposed) {
            return;
        }
        this.cam = cam;
        int camX = FastMath.floor(cam.position.doubleX() / 16.0d);
        int camY = FastMath.floor(cam.position.doubleY() / 16.0d);
        double newChunkDistance = chunkDistanceMax;
        chunks.clear();
        for (Vector2i pos : sortedLocations) {
            TerrainInfiniteChunkClient chunk =
                    terrain.getChunkNoLoad(pos.intX() + camX,
                            pos.intY() + camY);
            if (chunk != null) {
                TerrainInfiniteRendererChunk rendererChunk =
                        chunk.getRendererChunk();
                if (rendererChunk.isLoaded()) {
                    chunks.add(rendererChunk);
                } else {
                    newChunkDistance = FastMath.min(newChunkDistance,
                            FastMath.sqr(
                                    (rendererChunk.getChunk().getX() << 4) + 8 -
                                            cam.position.doubleX()) +
                                    FastMath.sqr(
                                            (rendererChunk.getChunk().getY() <<
                                                    4) + 8 -
                                                    cam.position.doubleY()) -
                                    256.0d);
                }
            }
        }
        chunkDistance = FastMath.sqrt(newChunkDistance);
    }

    @Override
    public void render(GraphicsSystem graphics, Shader shader, Cam cam,
            boolean debug) {
        if (disposed) {
            return;
        }
        chunks.forEach(chunk -> chunk
                .render(graphics, shader, keepInvisibleChunkVbos, cam));
        if (debug) {
            graphics.getTextureManager().unbind(graphics);
            chunks.forEach(chunk -> chunk.renderFrame(graphics, shader, cam));
        }
    }

    @Override
    public void renderAlpha(GraphicsSystem graphics, Shader shader, Cam cam) {
        if (disposed) {
            return;
        }
        ListIterator<TerrainInfiniteRendererChunk> iterator =
                chunks.listIterator(chunks.size());
        while (iterator.hasPrevious()) {
            iterator.previous()
                    .renderAlpha(graphics, shader, keepInvisibleChunkVbos, cam);
        }
    }

    @Override
    public void blockChange(int x, int y, int z) {
        int xx = FastMath.floor(x / 16.0d);
        int yy = FastMath.floor(y / 16.0d);
        TerrainInfiniteChunkClient chunk = terrain.getChunk(xx, yy);
        if (chunk != null) {
            TerrainInfiniteRendererChunk rendererChunk =
                    chunk.getRendererChunk();
            int zz = FastMath.floor(z / 16.0d);
            int xxx = x - (xx << 4);
            int yyy = y - (yy << 4);
            int zzz = z - (zz << 4);
            rendererChunk.setGeometryDirty(zz);
            if (xxx == 0) {
                setDirty(xx - 1, yy, zz);
            } else if (xxx == 15) {
                setDirty(xx + 1, yy, zz);
            }
            if (yyy == 0) {
                setDirty(xx, yy - 1, zz);
            } else if (yyy == 15) {
                setDirty(xx, yy + 1, zz);
            }
            if (zzz == 15 && zz < rendererChunk.getZSections() - 1) {
                rendererChunk.setGeometryDirty(zz + 1);
                if (xxx == 0) {
                    setDirty(xx - 1, yy, zz + 1);
                } else if (xxx == 15) {
                    setDirty(xx + 1, yy, zz + 1);
                }
                if (yyy == 0) {
                    setDirty(xx, yy - 1, zz + 1);
                } else if (yyy == 15) {
                    setDirty(xx, yy + 1, zz + 1);
                }
            } else if (zzz == 0 && zz > 0) {
                rendererChunk.setGeometryDirty(zz - 1);
                if (xxx == 0) {
                    setDirty(xx - 1, yy, zz - 1);
                } else if (xxx == 15) {
                    setDirty(xx + 1, yy, zz - 1);
                }
                if (yyy == 0) {
                    setDirty(xx, yy - 1, zz - 1);
                } else if (yyy == 15) {
                    setDirty(xx, yy + 1, zz - 1);
                }
            }
        }
    }

    @Override
    public double getActualRenderDistance() {
        if (staticRenderDistance) {
            return chunkDistanceMax;
        }
        return chunkDistance;
    }

    private void updateVisible() {
        terrain.getLoadedChunks().forEach(
                chunk -> chunk.getRendererChunk().resetPrepareVisible());
        TerrainInfiniteChunkClient startChunk =
                terrain.getChunkNoLoad(playerX, playerY);
        checkVisible(playerX, playerY, playerZ, startChunk);
        terrain.getLoadedChunks().stream()
                .map(TerrainInfiniteChunkClient::getRendererChunk)
                .forEach(TerrainInfiniteRendererChunk::updateVisible);
    }

    private void checkVisible(int x, int y, int z,
            TerrainInfiniteChunkClient chunk) {
        if (chunk != null) {
            chunk.getRendererChunk().setPrepareVisible(z);
            cullingPool1.push()
                    .set(x, y, z + 1, chunk, chunk.getRendererChunk());
            cullingPool1.push()
                    .set(x, y, z - 1, chunk, chunk.getRendererChunk());
        }
        chunk = terrain.getChunkNoLoad(x, y - 1);
        if (chunk != null) {
            cullingPool1.push()
                    .set(x, y - 1, z, chunk, chunk.getRendererChunk());
        }
        chunk = terrain.getChunkNoLoad(x + 1, y);
        if (chunk != null) {
            cullingPool1.push()
                    .set(x + 1, y, z, chunk, chunk.getRendererChunk());
        }
        chunk = terrain.getChunkNoLoad(x, y + 1);
        if (chunk != null) {
            cullingPool1.push()
                    .set(x, y + 1, z, chunk, chunk.getRendererChunk());
        }
        chunk = terrain.getChunkNoLoad(x - 1, y);
        if (chunk != null) {
            cullingPool1.push()
                    .set(x - 1, y, z, chunk, chunk.getRendererChunk());
        }
        while (!cullingPool1.isEmpty()) {
            cullingPool1.stream()
                    .filter(update -> !update.rendererChunk.isCulled(update.z))
                    .forEach(update -> checkVisible(update, cullingPool2));
            Pool<VisibleUpdate> swap = cullingPool1;
            swap.reset();
            cullingPool1 = cullingPool2;
            cullingPool2 = swap;
        }
        terrain.getLoadedChunks().forEach(
                visible -> visible.getRendererChunk().setCulled(false));
    }

    private void checkVisible(VisibleUpdate update, Pool<VisibleUpdate> pool) {
        update.rendererChunk.setPrepareVisible(update.z);
        if (update.rendererChunk.setCulled(update.z, true) &&
                !update.rendererChunk.isSolid(update.z)) {
            int x = update.x - playerX;
            int y = update.y - playerY;
            int z = update.z - playerZ;
            if (z >= 0) {
                TerrainInfiniteRendererChunk rendererChunk =
                        update.chunk.getRendererChunk();
                if (!rendererChunk.isCulled(update.z + 1)) {
                    pool.push()
                            .set(update.x, update.y, update.z + 1, update.chunk,
                                    rendererChunk);
                }
            }
            if (z <= 0) {
                TerrainInfiniteRendererChunk rendererChunk =
                        update.chunk.getRendererChunk();
                if (!rendererChunk.isCulled(update.z - 1)) {
                    pool.push()
                            .set(update.x, update.y, update.z - 1, update.chunk,
                                    rendererChunk);
                }
            }
            if (y <= 0) {
                TerrainInfiniteChunkClient chunk =
                        terrain.getChunkNoLoad(update.x, update.y - 1);
                if (chunk != null) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.getRendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x, update.y - 1, update.z, chunk,
                                rendererChunk);
                    }
                }
            }
            if (x >= 0) {
                TerrainInfiniteChunkClient chunk =
                        terrain.getChunkNoLoad(update.x + 1, update.y);
                if (chunk != null) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.getRendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x + 1, update.y, update.z, chunk,
                                rendererChunk);
                    }
                }
            }
            if (y >= 0) {
                TerrainInfiniteChunkClient chunk =
                        terrain.getChunkNoLoad(update.x, update.y + 1);
                if (chunk != null) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.getRendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x, update.y + 1, update.z, chunk,
                                rendererChunk);
                    }
                }
            }
            if (x <= 0) {
                TerrainInfiniteChunkClient chunk =
                        terrain.getChunkNoLoad(update.x - 1, update.y);
                if (chunk != null) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.getRendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x - 1, update.y, update.z, chunk,
                                rendererChunk);
                    }
                }
            }
        }
    }

    private void setDirty(int x, int y, int z) {
        TerrainInfiniteChunkClient chunk = terrain.getChunk(x, y);
        if (chunk != null) {
            chunk.getRendererChunk().setGeometryDirty(z);
        }
    }

    private static class VisibleUpdate {
        private int x, y, z;
        private TerrainInfiniteChunkClient chunk;
        private TerrainInfiniteRendererChunk rendererChunk;

        public void set(int x, int y, int z, TerrainInfiniteChunkClient chunk,
                TerrainInfiniteRendererChunk rendererChunk) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.chunk = chunk;
            this.rendererChunk = rendererChunk;
        }
    }

    protected class TerrainInfiniteRendererThread
            implements TaskExecutor.ASyncTask {
        private final Queue<TerrainInfiniteRendererChunk> queue, idleQueue;
        private final ChunkMesh.VertexArrays arrays, arraysAlpha;
        private final boolean visibleUpdater;

        public TerrainInfiniteRendererThread(
                Queue<TerrainInfiniteRendererChunk> queue,
                Queue<TerrainInfiniteRendererChunk> idleQueue,
                boolean visibleUpdater) {
            this.queue = queue;
            this.idleQueue = idleQueue;
            this.visibleUpdater = visibleUpdater;
            arrays = new ChunkMesh.VertexArrays();
            arraysAlpha = new ChunkMesh.VertexArrays();
        }

        @Override
        public void run(Joiner joiner) {
            while (!joiner.marked()) {
                boolean idle = true;
                while (!queue.isEmpty()) {
                    process(queue.poll());
                    idle = false;
                }
                if (!idleQueue.isEmpty()) {
                    process(idleQueue.poll());
                    idle = false;
                }
                if (visibleUpdater && cam != null) {
                    int newPlayerX =
                            FastMath.floor(cam.position.doubleX() / 16.0);
                    int newPlayerY =
                            FastMath.floor(cam.position.doubleY() / 16.0);
                    int newPlayerZ =
                            FastMath.floor(cam.position.doubleZ() / 16.0);
                    if (updateVisible || playerX != newPlayerX ||
                            playerY != newPlayerY ||
                            playerZ != newPlayerZ) {
                        updateVisible = false;
                        playerX = newPlayerX;
                        playerY = newPlayerY;
                        playerZ = newPlayerZ;
                        updateVisible();
                        idle = false;
                    }
                }
                if (idle) {
                    SleepUtil.sleep(10);
                }
            }
        }

        private void process(TerrainInfiniteRendererChunk chunk) {
            if (chunk != null && !chunk.getChunk().isDisposed()) {
                for (int i = 0; i < chunk.getZSections(); i++) {
                    if (chunk.isGeometryDirty(i)) {
                        chunk.unsetGeometryDirty(i);
                        process(chunk, i);
                    }
                }
            }
        }

        private void process(TerrainInfiniteRendererChunk chunk, int i) {
            VAO vao = null, vaoAlpha = null;
            AABB aabb = null, aabbAlpha = null;
            if (chunk.getChunk().isEmpty(i)) {
                chunk.setSolid(i, false);
            } else {
                TerrainInfiniteChunk terrainChunk = chunk.getChunk();
                int bx = terrainChunk.getX() << 4;
                int by = terrainChunk.getY() << 4;
                int bz = i << 4;
                boolean solid = true, empty = true;
                for (int xxx = 0; xxx < 16; xxx++) {
                    int bxx = bx + xxx;
                    for (int yyy = 0; yyy < 16; yyy++) {
                        int byy = by + yyy;
                        for (int zzz = 0; zzz < 16; zzz++) {
                            int bzz = bz + zzz;
                            BlockType type =
                                    terrainChunk.getBlockType(xxx, yyy, bzz);
                            if (type == terrain.getWorld().getAir()) {
                                solid = false;
                            } else {
                                empty = false;
                                if (solid &&
                                        type.connectStage(terrain, bxx, byy,
                                                bzz) < 4) {
                                    solid = false;
                                }
                            }
                            if (!solid && !empty) {
                                break;
                            }
                        }
                    }
                }
                if (chunk.isSolid(i) != solid || !chunk.isLoaded()) {
                    chunk.setSolid(i, solid);
                    updateVisible = true;
                }
                if (!empty && chunk.isVisible(i)) {
                    ChunkMesh mesh = new ChunkMesh(arrays);
                    ChunkMesh meshAlpha = new ChunkMesh(arraysAlpha);
                    boolean lod = chunk.getLod(i);
                    boolean needsLod = false;
                    for (int xxx = 0; xxx < 16; xxx++) {
                        int bxx = bx + xxx;
                        for (int yyy = 0; yyy < 16; yyy++) {
                            int byy = by + yyy;
                            for (int zzz = 0; zzz < 16; zzz++) {
                                int bzz = bz + zzz;
                                BlockType type = terrainChunk
                                        .getBlockType(xxx, yyy, bzz);
                                int data = terrainChunk
                                        .getBlockData(xxx, yyy, bzz);
                                type.addToChunkMesh(mesh, meshAlpha, data,
                                        terrain, bxx, byy, bzz, xxx, yyy, zzz,
                                        lod);
                                if (!needsLod &&
                                        type.needsLodUpdate(data, terrain, bxx,
                                                byy, bzz)) {
                                    needsLod = true;
                                }
                            }
                        }
                    }
                    chunk.setNeedsLod(i, needsLod);
                    if (mesh.getSize() > 0) {
                        vao = mesh.finish();
                        aabb = mesh.getAABB();
                    }
                    if (meshAlpha.getSize() > 0) {
                        vaoAlpha = meshAlpha.finish();
                        aabbAlpha = meshAlpha.getAABB();
                    }
                }
            }
            chunk.replaceMesh(i, vao, vaoAlpha, aabb, aabbAlpha);
        }
    }
}
