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
package org.tobi29.scapes.chunk.terrain.infinite;

import java8.util.Optional;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainRenderer;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.graphics.*;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.ThreadLocalUtil;
import org.tobi29.scapes.engine.utils.graphics.Cam;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector2i;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.profiler.Profiler;
import org.tobi29.scapes.engine.utils.task.TaskLock;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerrainInfiniteRenderer implements TerrainRenderer {
    private static final ThreadLocal<ThreadLocalData> THREAD_DATA =
            ThreadLocalUtil.of(ThreadLocalData::new);
    private final List<Vector2i> sortedLocations;
    private final TerrainInfiniteClient terrain;
    private final double chunkDistanceMax;
    private final List<TerrainInfiniteRendererChunk> chunks = new ArrayList<>();
    private final Model frame;
    private final TaskLock taskLock = new TaskLock();
    private final AtomicBoolean updateVisible = new AtomicBoolean(),
            updatingVisible = new AtomicBoolean();
    private int playerX, playerY, playerZ;
    private double chunkDistance;
    private boolean disposed, staticRenderDistance;
    private Cam cam;
    private Pool<VisibleUpdate> cullingPool1 = new Pool<>(VisibleUpdate::new),
            cullingPool2 = new Pool<>(VisibleUpdate::new);

    public TerrainInfiniteRenderer(TerrainInfiniteClient terrain,
            MobPlayerClientMain player, double chunkDistance,
            List<Vector2i> sortedLocations) {
        this.terrain = terrain;
        this.sortedLocations = sortedLocations;
        chunkDistanceMax = chunkDistance * 16.0 - 16.0;
        float min = 0.001f;
        float max = 0.999f;
        frame = VAOUtility.createVI(player.game().engine(),
                new float[]{min, min, min, max, min, min, max, max, min, min,
                        max, min, min, min, max, max, min, max, max, max, max,
                        min, max, max},
                new int[]{0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4,
                        1, 5, 2, 6, 3, 7}, RenderType.LINES);
    }

    public void toggleStaticRenderDistance() {
        staticRenderDistance = !staticRenderDistance;
    }

    public void reloadGeometry() {
        Streams.forEach(terrain.loadedChunks(),
                chunk -> chunk.rendererChunk().reset());
    }

    public void dispose() {
        taskLock.lock();
        disposed = true;
    }

    public void addToQueue(TerrainInfiniteRendererChunk chunk, int i) {
        if (chunk == null || !chunk.chunk().isLoaded() || disposed) {
            return;
        }
        if (!checkLoaded(chunk)) {
            return;
        }
        terrain.world().game().engine().taskExecutor().runTask(() -> {
            ThreadLocalData threadData = THREAD_DATA.get();
            threadData.process(chunk, i, this);
        }, taskLock, "Update-Chunk-Geometry");
    }

    public void addToQueue(TerrainInfiniteRendererChunk chunk) {
        if (chunk == null || !chunk.chunk().isLoaded() || disposed) {
            return;
        }
        if (!checkLoaded(chunk)) {
            return;
        }
        terrain.world().game().engine().taskExecutor().runTask(() -> {
            ThreadLocalData threadData = THREAD_DATA.get();
            for (int i = 0; i < chunk.zSections(); i++) {
                threadData.process(chunk, i, this);
            }
        }, taskLock, "Update-Chunk-Geometry");
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean checkLoaded(TerrainInfiniteRendererChunk chunk) {
        TerrainInfiniteChunkClient terrainChunk = chunk.chunk();
        int x = terrainChunk.x();
        int y = terrainChunk.y();
        TerrainInfinite terrain = terrainChunk.terrain();
        if (!checkLoaded(terrain, x - 1, y - 1)) {
            return false;
        }
        if (!checkLoaded(terrain, x, y - 1)) {
            return false;
        }
        if (!checkLoaded(terrain, x - 1, y - 1)) {
            return false;
        }
        if (!checkLoaded(terrain, x - 1, y)) {
            return false;
        }
        if (!checkLoaded(terrain, x - 1, y)) {
            return false;
        }
        if (!checkLoaded(terrain, x - 1, y + 1)) {
            return false;
        }
        if (!checkLoaded(terrain, x, y + 1)) {
            return false;
        }
        if (!checkLoaded(terrain, x - 1, y + 1)) {
            return false;
        }
        return true;
    }

    @Override
    public void renderUpdate(Cam cam) {
        if (disposed) {
            return;
        }
        this.cam = cam;
        Vector3 camPos = cam.position.now().div(16.0);
        int newPlayerX = camPos.intX();
        int newPlayerY = camPos.intY();
        int newPlayerZ = camPos.intZ();
        if (!updatingVisible.get() &&
                (updateVisible.getAndSet(false) || playerX != newPlayerX ||
                        playerY != newPlayerY || playerZ != newPlayerZ)) {
            updatingVisible.set(true);
            playerX = newPlayerX;
            playerY = newPlayerY;
            playerZ = newPlayerZ;
            terrain.world().game().engine().taskExecutor().runTask(() -> {
                updateVisible();
                updatingVisible.set(false);
            }, taskLock, "Update-Visible-Chunks");
        }
        int camX = camPos.intX();
        int camY = camPos.intY();
        double offsetX = camX - camPos.doubleX();
        double offsetY = camY - camPos.doubleY();
        chunks.clear();
        chunkDistance = chunkDistanceMax;
        for (Vector2i pos : sortedLocations) {
            Optional<TerrainInfiniteChunkClient> chunk =
                    terrain.chunkNoLoad(camX + pos.intX(), camY + pos.intY());
            if (chunk.isPresent()) {
                TerrainInfiniteRendererChunk rendererChunk =
                        chunk.get().rendererChunk();
                if (rendererChunk.isLoaded()) {
                    chunks.add(rendererChunk);
                    continue;
                }
            }
            chunkDistance = FastMath.min(chunkDistance, FastMath.sqrt(
                    FastMath.sqr(offsetX + pos.doubleX()) +
                            FastMath.sqr(offsetY + pos.intY()) - 2.0) * 16.0);
        }
    }

    @Override
    public void render(GL gl, Shader shader1, Shader shader2, Cam cam,
            boolean debug) {
        if (disposed) {
            return;
        }
        Streams.forEach(chunks,
                chunk -> chunk.render(gl, shader1, shader2, cam));
        if (debug) {
            gl.textures().unbind(gl);
            Streams.forEach(chunks,
                    chunk -> chunk.renderFrame(gl, frame, shader1, cam));
        }
    }

    @Override
    public void renderAlpha(GL gl, Shader shader1, Shader shader2, Cam cam) {
        if (disposed) {
            return;
        }
        ListIterator<TerrainInfiniteRendererChunk> iterator =
                chunks.listIterator(chunks.size());
        while (iterator.hasPrevious()) {
            iterator.previous().renderAlpha(gl, shader1, shader2, cam);
        }
    }

    @Override
    public void blockChange(int x, int y, int z) {
        int xx = x >> 4;
        int yy = y >> 4;
        terrain.chunkC(xx, yy, chunk -> {
            TerrainInfiniteRendererChunk rendererChunk = chunk.rendererChunk();
            int zz = z >> 4;
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
            if (zzz == 15 && zz < rendererChunk.zSections() - 1) {
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
        });
    }

    @Override
    public double actualRenderDistance() {
        if (staticRenderDistance) {
            return 4096.0;
        }
        return chunkDistance;
    }

    private void updateVisible() {
        Streams.forEach(terrain.loadedChunks(),
                chunk -> chunk.rendererChunk().resetPrepareVisible());
        Optional<TerrainInfiniteChunkClient> startChunk =
                terrain.chunkNoLoad(playerX, playerY);
        checkVisible(playerX, playerY, playerZ, startChunk);
        Streams.of(terrain.loadedChunks())
                .map(TerrainInfiniteChunkClient::rendererChunk)
                .forEach(TerrainInfiniteRendererChunk::updateVisible);
    }

    private void checkVisible(int x, int y, int z,
            Optional<TerrainInfiniteChunkClient> chunk) {
        if (chunk.isPresent()) {
            chunk.get().rendererChunk().setPrepareVisible(z);
            cullingPool1.push()
                    .set(x, y, z + 1, chunk.get(), chunk.get().rendererChunk());
            cullingPool1.push()
                    .set(x, y, z - 1, chunk.get(), chunk.get().rendererChunk());
        }
        chunk = terrain.chunkNoLoad(x, y - 1);
        if (chunk.isPresent()) {
            cullingPool1.push()
                    .set(x, y - 1, z, chunk.get(), chunk.get().rendererChunk());
        }
        chunk = terrain.chunkNoLoad(x + 1, y);
        if (chunk.isPresent()) {
            cullingPool1.push()
                    .set(x + 1, y, z, chunk.get(), chunk.get().rendererChunk());
        }
        chunk = terrain.chunkNoLoad(x, y + 1);
        if (chunk.isPresent()) {
            cullingPool1.push()
                    .set(x, y + 1, z, chunk.get(), chunk.get().rendererChunk());
        }
        chunk = terrain.chunkNoLoad(x - 1, y);
        if (chunk.isPresent()) {
            cullingPool1.push()
                    .set(x - 1, y, z, chunk.get(), chunk.get().rendererChunk());
        }
        while (!cullingPool1.isEmpty()) {
            Streams.forEach(cullingPool1,
                    update -> !update.rendererChunk.isCulled(update.z),
                    update -> checkVisible(update, cullingPool2));
            Pool<VisibleUpdate> swap = cullingPool1;
            swap.reset();
            cullingPool1 = cullingPool2;
            cullingPool2 = swap;
        }
        Streams.forEach(terrain.loadedChunks(),
                visible -> visible.rendererChunk().setCulled(false));
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
                        update.chunk.rendererChunk();
                if (!rendererChunk.isCulled(update.z + 1)) {
                    pool.push()
                            .set(update.x, update.y, update.z + 1, update.chunk,
                                    rendererChunk);
                }
            }
            if (z <= 0) {
                TerrainInfiniteRendererChunk rendererChunk =
                        update.chunk.rendererChunk();
                if (!rendererChunk.isCulled(update.z - 1)) {
                    pool.push()
                            .set(update.x, update.y, update.z - 1, update.chunk,
                                    rendererChunk);
                }
            }
            if (y <= 0) {
                Optional<TerrainInfiniteChunkClient> chunk =
                        terrain.chunkNoLoad(update.x, update.y - 1);
                if (chunk.isPresent()) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.get().rendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x, update.y - 1, update.z,
                                chunk.get(), rendererChunk);
                    }
                }
            }
            if (x >= 0) {
                Optional<TerrainInfiniteChunkClient> chunk =
                        terrain.chunkNoLoad(update.x + 1, update.y);
                if (chunk.isPresent()) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.get().rendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x + 1, update.y, update.z,
                                chunk.get(), rendererChunk);
                    }
                }
            }
            if (y >= 0) {
                Optional<TerrainInfiniteChunkClient> chunk =
                        terrain.chunkNoLoad(update.x, update.y + 1);
                if (chunk.isPresent()) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.get().rendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x, update.y + 1, update.z,
                                chunk.get(), rendererChunk);
                    }
                }
            }
            if (x <= 0) {
                Optional<TerrainInfiniteChunkClient> chunk =
                        terrain.chunkNoLoad(update.x - 1, update.y);
                if (chunk.isPresent()) {
                    TerrainInfiniteRendererChunk rendererChunk =
                            chunk.get().rendererChunk();
                    if (!rendererChunk.isCulled(update.z)) {
                        pool.push().set(update.x - 1, update.y, update.z,
                                chunk.get(), rendererChunk);
                    }
                }
            }
        }
    }

    private void setDirty(int x, int y, int z) {
        terrain.chunkC(x, y,
                chunk -> chunk.rendererChunk().setGeometryDirty(z));
    }

    private boolean checkLoaded(TerrainInfinite terrain, int x, int y) {
        Optional<? extends TerrainInfiniteChunk> chunk =
                terrain.chunkNoLoad(x, y);
        return chunk.isPresent() && chunk.get().isLoaded();
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

    private static final class ThreadLocalData {
        private final ChunkMesh.VertexArrays arrays =
                new ChunkMesh.VertexArrays(), arraysAlpha =
                new ChunkMesh.VertexArrays();
        private final TerrainInfiniteSection section =
                new TerrainInfiniteSection();

        private void process(TerrainInfiniteRendererChunk chunk, int i,
                TerrainInfiniteRenderer renderer) {
            if (!chunk.unsetGeometryDirty(i)) {
                return;
            }
            TerrainInfiniteChunkModel model = null;
            TerrainInfiniteChunk terrainChunk = chunk.chunk();
            if (terrainChunk.isEmpty(i)) {
                chunk.setSolid(i, false);
            } else {
                TerrainInfiniteClient terrain = chunk.chunk().terrain();
                BlockType air = terrain.world().air();
                section.init(terrain, terrainChunk.pos());
                int bx = terrainChunk.blockX();
                int by = terrainChunk.blockY();
                int bz = i << 4;
                boolean solid = true, empty = true;
                try (Profiler.C ignored = Profiler.section("CheckSolid")) {
                    for (int xxx = 0; xxx < 16; xxx++) {
                        int bxx = bx + xxx;
                        for (int yyy = 0; yyy < 16; yyy++) {
                            int byy = by + yyy;
                            for (int zzz = 0; zzz < 16; zzz++) {
                                int bzz = bz + zzz;
                                BlockType type =
                                        terrainChunk.typeL(xxx, yyy, bzz);
                                if (type == air) {
                                    solid = false;
                                } else {
                                    empty = false;
                                    if (solid &&
                                            type.connectStage(section, bxx, byy,
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
                }
                if (chunk.isSolid(i) != solid || !chunk.isLoaded()) {
                    chunk.setSolid(i, solid);
                    renderer.updateVisible.set(true);
                }
                if (!empty && chunk.isVisible(i)) {
                    double relativeX = terrainChunk.blockX() -
                            renderer.cam.position.doubleX();
                    double relativeY = terrainChunk.blockY() -
                            renderer.cam.position.doubleY();
                    double relativeZ =
                            (i << 4) - renderer.cam.position.doubleZ();
                    boolean lod = FastMath.sqr(relativeX + 8) +
                            FastMath.sqr(relativeY + 8) +
                            FastMath.sqr(relativeZ + 8) < 9216;
                    ChunkMesh mesh = new ChunkMesh(arrays);
                    ChunkMesh meshAlpha = new ChunkMesh(arraysAlpha);
                    TerrainRenderInfo info =
                            new TerrainRenderInfo(terrain.world().infoLayers());
                    info.init(bx, by, bz, 16, 16, 16);
                    try (Profiler.C ignored = Profiler
                            .section("GenerateMesh")) {
                        for (int xxx = 0; xxx < 16; xxx++) {
                            int bxx = bx + xxx;
                            for (int yyy = 0; yyy < 16; yyy++) {
                                int byy = by + yyy;
                                for (int zzz = 0; zzz < 16; zzz++) {
                                    int bzz = bz + zzz;
                                    BlockType type =
                                            terrainChunk.typeL(xxx, yyy, bzz);
                                    int data =
                                            terrainChunk.dataL(xxx, yyy, bzz);
                                    type.addToChunkMesh(mesh, meshAlpha, data,
                                            section, info, bxx, byy, bzz, xxx,
                                            yyy, zzz, lod);
                                }
                            }
                        }
                    }
                    try (Profiler.C ignored = Profiler
                            .section("AssembleMesh")) {
                        ScapesEngine engine = terrain.world().game().engine();
                        Model vao = mesh.finish(engine);
                        AABB aabb = mesh.aabb();
                        Model vaoAlpha = meshAlpha.finish(engine);
                        AABB aabbAlpha = meshAlpha.aabb();
                        model = new TerrainInfiniteChunkModel(vao, vaoAlpha,
                                aabb, aabbAlpha, lod);
                    }
                }
                section.clear();
            }
            chunk.replaceMesh(i, model);
        }
    }
}
