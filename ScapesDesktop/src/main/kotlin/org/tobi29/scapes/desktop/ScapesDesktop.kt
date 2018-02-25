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

package org.tobi29.scapes.desktop

import org.tobi29.scapes.client.DialogProvider
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.ContainerGLFW
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.PlatformDialogs
import org.tobi29.io.ReadableByteStream
import org.tobi29.io.filesystem.FilePath

internal class DialogProviderDesktop(
        private val container: ContainerGLFW?
) : DialogProvider {
    override fun openMusicDialog(result: (String, ReadableByteStream) -> Unit) {
        container?.let { container ->
            container.exec {
                PlatformDialogs.openFileDialog(container,
                        arrayOf("*.mp3" to "MP3 Audio",
                                "*.ogg" to "OGG Audio",
                                "*.wav" to "WAV Audio"), true, result)
            }
        }
    }

    override fun openPluginDialog(result: (String, ReadableByteStream) -> Unit) {
        container?.let { container ->
            container.exec {
                PlatformDialogs.openFileDialog(container,
                        arrayOf("*.jar" to "Jar Archive"), true,
                        result)
            }
        }
    }

    override fun openSkinDialog(result: (String, ReadableByteStream) -> Unit) {
        container?.let { container ->
            container.exec {
                PlatformDialogs.openFileDialog(container,
                        arrayOf("*.png" to "PNG Picture"), false, result)
            }
        }
    }

    override fun saveScreenshotDialog(result: (FilePath) -> Unit) {
        container?.let { container ->
            container.exec {
                PlatformDialogs.saveFileDialog(container,
                        arrayOf("*.png" to "PNG Picture"))?.let {
                    result(it)
                }
            }
        }
    }
}
