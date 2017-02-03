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

package org.tobi29.scapes.server.extension

import mu.KLogging
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.extension.spi.ServerExtensionProvider
import java.util.*

class ServerExtensions(private val server: ScapesServer) {
    private val extensions = ArrayList<ServerExtension>()

    fun init() {
        extensions.forEach { it.init() }
        extensions.forEach { it.initLate() }
    }

    fun loadExtensions(configStructure: TagStructure) {
        for (provider in ServiceLoader.load(
                ServerExtensionProvider::class.java)) {
            try {
                val name = provider.name
                val extension = provider.create(server,
                        configStructure.getStructure(name))
                if (extension != null) {
                    logger.info { "Loaded extension: $name" }
                    extensions.add(extension)
                }
            } catch (e: ServiceConfigurationError) {
                logger.warn { "Unable to load extension: $e" }
            }
        }
    }

    companion object : KLogging()
}
