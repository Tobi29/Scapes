/*
 * Copyright 2012-2017 Tobi29
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
package org.tobi29.scapes.server.format.basic

import mu.KLogging
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.exists
import org.tobi29.scapes.engine.utils.io.filesystem.read
import org.tobi29.scapes.engine.utils.io.filesystem.write
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureArchive
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.server.format.TerrainInfiniteFormat
import java.io.IOException
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BasicTerrainInfiniteFormat(private val path: FilePath) : TerrainInfiniteFormat {
    private val regions = ConcurrentHashMap<Vector2i, RegionFile>()

    init {
        val security = System.getSecurityManager()
        security?.checkPermission(
                RuntimePermission("scapes.terrainInfiniteFormat"))
    }

    @Synchronized override fun chunkTags(chunks: List<Vector2i>): List<TagMap?> {
        val maps = ArrayList<TagMap?>(chunks.size)
        for (chunk in chunks) {
            try {
                maps.add(chunkTag(chunk.x, chunk.y))
            } catch (e: IOException) {
                logger.error { "Failed to load chunk: $e" }
                maps.add(null)
            }
        }
        return maps
    }

    @Synchronized override fun putChunkTags(chunks: List<Pair<Vector2i, TagMap>>) {
        for (chunk in chunks) {
            putChunkTag(chunk.first.x, chunk.first.y, chunk.second)
        }
    }

    @Synchronized override fun dispose() {
        regions.keys.forEach { this.removeRegion(it) }
    }

    private fun chunkTag(x: Int,
                         y: Int): TagMap? {
        val location = Vector2i(x shr 4, y shr 4)
        var region: RegionFile? = regions[location]
        if (region == null) {
            region = createRegion(location)
        }
        region.lastUse = System.currentTimeMillis()
        return region.tag.getTagMap(
                filename(x - (location.x shl 4), y - (location.y shl 4)))
    }

    private fun putChunkTag(x: Int,
                            y: Int,
                            map: TagMap) {
        val location = Vector2i(x shr 4, y shr 4)
        var region: RegionFile? = regions[location]
        if (region == null) {
            region = createRegion(location)
        }
        region.lastUse = System.currentTimeMillis()
        region.tag.setTagMap(filename(x - (location.x shl 4),
                y - (location.y shl 4)), map)
    }

    private fun createRegion(location: Vector2i): RegionFile {
        val region = AccessController.doPrivileged(
                PrivilegedAction {
                    RegionFile(
                            path.resolve(
                                    filename(location.x,
                                            location.y) + ".star"))
                })
        val time = System.currentTimeMillis()
        regions.entries.asSequence()
                .filter { entry -> time - entry.value.lastUse > 1000 }
                .map { it.key }.forEach { removeRegion(it) }
        regions.put(location, region)
        return region
    }

    private fun removeRegion(location: Vector2i) {
        AccessController.doPrivileged(PrivilegedAction {
            regions.remove(location)?.write()
        })
    }

    private class RegionFile(val path: FilePath) {
        val tag: TagStructureArchive
        var lastUse = 0L

        init {
            tag = TagStructureArchive()
            if (exists(path)) {
                try {
                    read(path, { tag.read(it) })
                } catch (e: IOException) {
                    logger.error(
                            e) { "Error whilst loading tag-list-container" }
                }

            }
        }

        fun write() {
            try {
                write(path, { tag.write(it) })
            } catch (e: IOException) {
                logger.error(e) { "Error whilst saving tag-list-container" }
            }

        }
    }

    companion object : KLogging() {
        private fun filename(x: Int,
                             y: Int): String {
            return "${Integer.toString(x, 36).toUpperCase()}_${Integer.toString(
                    y, 36).toUpperCase()}"
        }
    }
}
