/*
 * Copyright 2012-2018 Tobi29
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

package org.tobi29.scapes.client

import kotlinx.coroutines.experimental.async
import org.tobi29.graphics.*
import org.tobi29.io.IOException
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.filesystem.*
import org.tobi29.io.tag.*
import org.tobi29.logging.KLogging
import org.tobi29.scapes.engine.ComponentLifecycle
import org.tobi29.scapes.engine.ComponentStep
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.plugins.Plugins
import org.tobi29.utils.ComponentTypeRegistered
import org.tobi29.utils.ComponentTypeRegisteredPermission
import kotlin.collections.set

class ScapesClient(
    val engine: ScapesEngine,
    val home: FilePath,
    savesSupplier: (ScapesClient) -> SaveStorage,
    lightDefaults: Boolean = false
) : ComponentLifecycle, ComponentStep {
    val configMap = engine[ScapesEngine.CONFIG_MAP_COMPONENT].mapMut("Scapes")
    val saves = savesSupplier(this)

    var animations by configMap.tagBoolean("Animations", !lightDefaults)
    var bloom by configMap.tagBoolean("Bloom", !lightDefaults)
    var autoExposure by configMap.tagBoolean("AutoExposure", !lightDefaults)
    var fxaa by configMap.tagBoolean("FXAA", !lightDefaults)
    var renderDistance by configMap.tagDouble(
        "RenderDistance",
        if (lightDefaults) 64.0 else 128.0
    )
    var resolutionMultiplier by configMap.tagDouble(
        "ResolutionMultiplier",
        if (lightDefaults) 0.5 else 1.0
    )

    override fun init(holder: ScapesEngine) {
        if (!configMap.containsKey("IntegratedServer")) {
            configMap["IntegratedServer"] = TagMap {
                this["Server"] = TagMap {
                    this["MaxLoadingRadius"] = 288.toTag()
                    this["Socket"] = TagMap {
                        this["MaxPlayers"] = 5.toTag()
                        this["WorkerCount"] = 1.toTag()
                    }
                }
            }
        }
        Plugins.setupAssets(engine.files)
        holder.registerComponent(Screenshots.COMPONENT, object : Screenshots {
            override fun saveScreenshot(image: Bitmap<*, *>) {
                val path = home.resolve("screenshots")
                    .resolve("${System.currentTimeMillis()}.png")
                try {
                    write(path) { encodePng(image, it, 9, false) }
                } catch (e: IOException) {
                    logger.warn(e) { "Error saving screenshot" }
                }
            }

            override fun screenshots(): List<Screenshots.Screenshot> = try {
                val path = home.resolve("screenshots")
                listRecursive(path) {
                    filter { isRegularFile(it) }
                        .filter { isNotHidden(it) }.toList()
                }.sorted()
            } catch (e: IOException) {
                logger.warn { "Failed to list screenshots: $e" }
                emptyList<FilePath>()
            }.map { file ->
                object : Screenshots.Screenshot {
                    override val image by lazy {
                        async(engine.taskExecutor) {
                            try {
                                decodePng(file)
                            } catch (e: IOException) {
                                logger.warn(e) { "Failed to decode screenshot" }
                                MutableIntByteViewBitmap(1, 1, RGBA)
                            }
                        }
                    }

                    override fun delete(): Boolean {
                        try {
                            delete(file)
                            return true
                        } catch (e: IOException) {
                            logger.warn(e) { "Failed to delete screenshot" }
                        }
                        return false
                    }
                }
            }
        })
    }

    override fun dispose(holder: ScapesEngine) {
        holder.unregisterComponent(Screenshots.COMPONENT)
    }

    companion object : KLogging() {
        val COMPONENT =
            ComponentTypeRegistered<ScapesEngine, ScapesClient, Any>()
    }
}

interface DialogProvider {
    fun openMusicDialog(result: (String, ReadableByteStream) -> Unit)

    fun openSkinDialog(result: (String, ReadableByteStream) -> Unit)

    fun saveScreenshotDialog(result: (FilePath) -> Unit)

    companion object {
        val COMPONENT =
            ComponentTypeRegisteredPermission<ScapesEngine, DialogProvider, Any>(
                "scapes.dialogs"
            )
    }
}
