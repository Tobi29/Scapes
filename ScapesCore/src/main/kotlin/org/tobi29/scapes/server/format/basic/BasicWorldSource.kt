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

import org.tobi29.scapes.engine.utils.graphics.decodePNG
import org.tobi29.scapes.engine.utils.graphics.encodePNG
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.binary.TagStructureBinary
import org.tobi29.scapes.engine.utils.io.tag.setLong
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.format.WorldFormat
import org.tobi29.scapes.server.format.WorldSource
import org.tobi29.scapes.server.format.newPanorama

class BasicWorldSource(private val path: FilePath) : WorldSource {
    private val tagStructure: TagStructure

    init {
        val data = path.resolve("Data.stag")
        if (exists(data)) {
            tagStructure = read(data, { TagStructureBinary.read(it) })
        } else {
            tagStructure = TagStructure()
        }
    }

    override fun init(seed: Long,
                      plugins: List<FilePath>) {
        createDirectories(path)
        tagStructure.setLong("Seed", seed)
        val pluginsDir = path.resolve("plugins")
        createDirectories(pluginsDir)
        for (plugin in plugins) {
            copy(plugin, pluginsDir.resolve(plugin.fileName))
        }
    }

    override fun panorama(images: WorldSource.Panorama) {
        images.elements.indices.forEach {
            val image = images.elements[it]
            write(path.resolve("Panorama$it.png")) {
                encodePNG(image, it, 9, false)
            }
        }
    }

    override fun panorama(): WorldSource.Panorama? {
        return newPanorama {
            val background = path.resolve("Panorama$it.png")
            if (exists(background)) {
                read(background) { decodePNG(it) }
            } else {
                return null
            }
        }
    }

    override fun open(server: ScapesServer): WorldFormat {
        return BasicWorldFormat(path, tagStructure)
    }

    override fun close() {
        write(path.resolve("Data.stag")) { streamOut ->
            TagStructureBinary.write(streamOut, tagStructure)
        }
    }
}
