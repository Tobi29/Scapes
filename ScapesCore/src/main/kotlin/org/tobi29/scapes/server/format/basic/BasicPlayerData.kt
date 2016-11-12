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
package org.tobi29.scapes.server.format.basic

import mu.KLogging
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.engine.utils.io.tag.getInt
import org.tobi29.scapes.engine.utils.io.tag.setInt
import org.tobi29.scapes.engine.utils.io.tag.structure
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.server.PlayerEntry
import org.tobi29.scapes.server.format.PlayerData
import java.io.IOException
import java.security.AccessController
import java.security.PrivilegedAction

class BasicPlayerData(private val path: FilePath) : PlayerData {

    init {
        val security = System.getSecurityManager()
        security?.checkPermission(
                RuntimePermission("scapes.playerData"))
        createDirectories(path)
    }

    @Synchronized override fun player(id: String): PlayerEntry {
        val tagStructure = load(id)
        val permissions = tagStructure.getInt("Permissions") ?: 0
        val worldName = tagStructure.getString("World")
        val entityStructure = tagStructure.getStructure("Entity")
        return PlayerEntry(permissions, worldName, entityStructure)
    }

    @Synchronized override fun save(id: String,
                                    entity: MobPlayerServer,
                                    permissions: Int) {
        save(structure {
            setStructure("Entity", entity.write(false))
            setString("World", entity.world.id)
            setInt("Permissions", permissions)
        }, id)
    }

    @Synchronized override fun add(id: String) {
        save(load(id), id)
    }

    @Synchronized override fun remove(id: String) {
        AccessController.doPrivileged(PrivilegedAction {
            try {
                deleteIfExists(path.resolve(id + ".stag"))
            } catch (e: IOException) {
                logger.error { "Error writing player data: $e" }
            }
        })
    }

    override fun playerExists(id: String): Boolean {
        return AccessController.doPrivileged(PrivilegedAction {
            exists(path.resolve(id + ".stag"))
        })
    }

    @Synchronized private fun load(id: String): TagStructure {
        return AccessController.doPrivileged(PrivilegedAction {
            try {
                val file = path.resolve(id + ".stag")
                if (exists(file)) {
                    return@PrivilegedAction read(file) {
                        TagStructureBinary.read(it)
                    }
                }
            } catch (e: IOException) {
                logger.error { "Error reading player data: $e" }
            }
            TagStructure()
        })
    }

    @Synchronized private fun save(tagStructure: TagStructure,
                                   id: String) {
        AccessController.doPrivileged(PrivilegedAction {
            try {
                val file = path.resolve(id + ".stag")
                write(file, { stream ->
                    TagStructureBinary.write(stream, tagStructure)
                })
            } catch (e: IOException) {
                logger.error { "Error writing player data: $e" }
            }
        })
    }

    companion object : KLogging()
}
