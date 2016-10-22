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
package org.tobi29.scapes.client

import mu.KLogging
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.GuiNotificationSimple
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import java.io.IOException
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.concurrent.ThreadLocalRandom

class Playlist(private val engine: ScapesEngine) {
    private var currentMusic: Music? = null
    private var musicWait = 5.0

    init {
        val security = System.getSecurityManager()
        security?.checkPermission(RuntimePermission("scapes.playlist"))
    }

    fun update(player: MobPlayerClientMain,
               delta: Double) {
        if (!engine.sounds.isPlaying("music")) {
            if (musicWait >= 0.0) {
                musicWait -= delta
            } else {
                val music: Music
                val pos = player.getCurrentPos()
                if (player.world.environment.sunLightReduction(
                        pos.intX().toDouble(), pos.intY().toDouble()) > 8) {
                    music = Music.NIGHT
                } else {
                    music = Music.DAY
                }
                playMusic(music)
                val random = ThreadLocalRandom.current()
                musicWait = random.nextDouble() * 4.0 + 4.0
            }
        }
    }

    private fun playMusic(music: Music) {
        currentMusic = music
        if (engine.config.volume("music") <= 0.0) {
            return
        }
        val title = playMusic(
                engine.home.resolve("playlists").resolve(music.dirName))
        if (title != null) {
            val fileName = title.fileName.toString()
            val index = fileName.lastIndexOf('.')
            val name: String
            if (index == -1) {
                name = fileName
            } else {
                name = fileName.substring(0, index)
            }
            engine.notifications.add {
                GuiNotificationSimple(it,
                        engine.graphics.textures()["Scapes:image/gui/Playlist"],
                        name)
            }
        }
    }

    private fun playMusic(path: FilePath): FilePath? {
        return AccessController.doPrivileged(PrivilegedAction {
            try {
                val files = listRecursive(path,
                        ::isRegularFile,
                        ::isNotHidden)
                if (!files.isEmpty()) {
                    val random = ThreadLocalRandom.current()
                    val title = files[random.nextInt(files.size)]
                    engine.sounds.stop("music")
                    engine.sounds.playMusic(read(title),
                            "music.Playlist", 1.0f, 1.0f, false)
                    return@PrivilegedAction title
                }
            } catch (e: IOException) {
                logger.warn { "Failed to play music: $e" }
            }
            null
        })
    }

    fun setMusic(music: Music) {
        if (music != currentMusic) {
            playMusic(music)
            val random = ThreadLocalRandom.current()
            musicWait = random.nextDouble() * 20.0 + 4.0
        }
    }

    enum class Music(val dirName: String) {
        DAY("day"),
        NIGHT("night"),
        BATTLE("battle");
    }

    companion object : KLogging()
}
