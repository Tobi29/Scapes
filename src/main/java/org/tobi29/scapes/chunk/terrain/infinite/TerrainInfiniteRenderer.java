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
import org.tobi29.scapes.engine.utils.math.Face;
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
        checkVisible(playerX, playerY, playerZ, 1, 0, 0, 1, (byte) 2, Face.UP,
                true, startChunk);
        checkVisible(playerX, playerY, playerZ, 1, 0, 0, 1, (byte) 1, Face.DOWN,
                true, startChunk);
        checkVisible(playerX, playerY, playerZ, 1, 0, 1, 0, (byte) 16,
                Face.NORTH, true, startChunk);
        checkVisible(playerX, playerY, playerZ, 1, 1, 0, 0, (byte) 32,
                Face.EAST, true, startChunk);
        checkVisible(playerX, playerY, playerZ, 1, 0, 1, 0, (byte) 4,
                Face.SOUTH, true, startChunk);
        checkVisible(playerX, playerY, playerZ, 1, 1, 0, 0, (byte) 8, Face.WEST,
                true, startChunk);
        terrain.getLoadedChunks().stream()
                .map(TerrainInfiniteChunkClient::getRendererChunk)
                .forEach(TerrainInfiniteRendererChunk::updateVisible);
    }

    private void checkVisible(int x, int y, int z, int l, int xl, int yl,
            int zl, byte face, Face primary, boolean prime,
            TerrainInfiniteChunkClient chunk) {
        cullingPool1.push()
                .set(x, y, z, l, xl, yl, zl, face, primary, prime, chunk);
        while (!cullingPool1.isEmpty()) {
            cullingPool1.stream()
                    .filter(update -> update.prime && update.chunk != null &&
                            !update.rendererChunk.isCulled(update.z))
                    .forEach(update -> checkVisible(update, cullingPool2));
            cullingPool1.stream()
                    .filter(update -> !update.prime && update.chunk != null &&
                            !update.rendererChunk.isCulled(update.z))
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
            if ((update.face & 1) != 1) {
                if (update.primary == Face.UP) {
                    pool.push()
                            .set(update.x, update.y, update.z + 1, 1, 0, 0, 1,
                                    (byte) (update.face | 2), update.primary,
                                    true, update.chunk);
                } else if (update.zl <= update.l) {
                    pool.push().set(update.x, update.y, update.z + 1, update.l,
                            update.xl, update.yl, update.zl + 1,
                            (byte) (update.face | 2), update.primary, false,
                            update.chunk);
                }
            }
            if ((update.face & 2) != 2) {
                if (update.primary == Face.DOWN) {
                    pool.push()
                            .set(update.x, update.y, update.z - 1, 1, 0, 0, 1,
                                    (byte) (update.face | 1), update.primary,
                                    true, update.chunk);
                } else if (update.zl <= update.l) {
                    pool.push().set(update.x, update.y, update.z - 1, update.l,
                            update.xl, update.yl, update.zl + 1,
                            (byte) (update.face | 1), update.primary, false,
                            update.chunk);
                }
            }
            if ((update.face & 4) != 4) {
                if (update.primary == Face.NORTH) {
                    pool.push()
                            .set(update.x, update.y - 1, update.z, 1, 0, 1, 0,
                                    (byte) (update.face | 16), update.primary,
                                    true, terrain.getChunkNoLoad(update.x,
                                            update.y - 1));
                } else if (update.yl <= update.l) {
                    pool.push().set(update.x, update.y - 1, update.z, update.l,
                            update.xl, update.yl + 1, update.zl,
                            (byte) (update.face | 16), update.primary, false,
                            terrain.getChunkNoLoad(update.x, update.y - 1));
                }
            }
            if ((update.face & 8) != 8) {
                if (update.primary == Face.EAST) {
                    pool.push()
                            .set(update.x + 1, update.y, update.z, 1, 1, 0, 0,
                                    (byte) (update.face | 32), update.primary,
                                    true, terrain.getChunkNoLoad(update.x + 1,
                                            update.y));
                } else if (update.xl <= update.l) {
                    pool.push().set(update.x + 1, update.y, update.z, update.l,
                            update.xl + 1, update.yl, update.zl,
                            (byte) (update.face | 32), update.primary, false,
                            terrain.getChunkNoLoad(update.x + 1, update.y));
                }
            }
            if ((update.face & 16) != 16) {
                if (update.primary == Face.SOUTH) {
                    pool.push()
                            .set(update.x, update.y + 1, update.z, 1, 0, 1, 0,
                                    (byte) (update.face | 4), update.primary,
                                    true, terrain.getChunkNoLoad(update.x,
                                            update.y + 1));
                } else if (update.yl <= update.l) {
                    pool.push().set(update.x, update.y + 1, update.z, update.l,
                            update.xl, update.yl + 1, update.zl,
                            (byte) (update.face | 4), update.primary, false,
                            terrain.getChunkNoLoad(update.x, update.y + 1));
                }
            }
            if ((update.face & 32) != 32) {
                if (update.primary == Face.WEST) {
                    pool.push()
                            .set(update.x - 1, update.y, update.z, 1, 1, 0, 0,
                                    (byte) (update.face | 8), update.primary,
                                    true, terrain.getChunkNoLoad(update.x - 1,
                                            update.y));
                } else if (update.xl <= update.l) {
                    pool.push().set(update.x - 1, update.y, update.z, update.l,
                            update.xl + 1, update.yl, update.zl,
                            (byte) (update.face | 8), update.primary, false,
                            terrain.getChunkNoLoad(update.x - 1, update.y));
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
        private int x, y, z, l, xl, yl, zl;
        private byte face;
        private Face primary;
        private boolean prime;
        private TerrainInfiniteChunkClient chunk;
        private TerrainInfiniteRendererChunk rendererChunk;

        public VisibleUpdate() {
        }

        public void set(int x, int y, int z, int l, int xl, int yl, int zl,
                byte face, Face primary, boolean prime,
                TerrainInfiniteChunkClient chunk) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.l = l;
            this.xl = xl;
            this.yl = yl;
            this.zl = zl;
            this.face = face;
            this.primary = primary;
            this.prime = prime;
            this.chunk = chunk;
            if (chunk == null) {
                rendererChunk = null;
            } else {
                rendererChunk = chunk.getRendererChunk();
            }
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
