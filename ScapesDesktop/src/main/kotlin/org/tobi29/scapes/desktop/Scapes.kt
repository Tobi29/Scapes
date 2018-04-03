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

import kotlinx.coroutines.experimental.runBlocking
import org.tobi29.application.Application
import org.tobi29.application.StatusCode
import org.tobi29.application.executeMain
import org.tobi29.args.*
import org.tobi29.chrono.*
import org.tobi29.coroutines.defaultBackgroundExecutor
import org.tobi29.io.*
import org.tobi29.io.classpath.ClasspathPath
import org.tobi29.io.filesystem.*
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.json.readJSON
import org.tobi29.io.tag.json.writeJSON
import org.tobi29.io.tag.mapMut
import org.tobi29.io.tag.toMutTag
import org.tobi29.io.tag.toTag
import org.tobi29.platform.appIDForData
import org.tobi29.platform.dataHome
import org.tobi29.scapes.Debug
import org.tobi29.scapes.client.DialogProvider
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.input.InputManagerScapes
import org.tobi29.scapes.client.states.GameStateMenu
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.GameStateStartup
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.backends.lwjgl3.ScapesEngineLWJGL3
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.ContainerGLFW
import org.tobi29.scapes.engine.graphics.FontRenderer
import org.tobi29.scapes.engine.graphics.GraphicsCheckException
import org.tobi29.scapes.engine.gui.GuiBasicStyle
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.sound.DefaultSoundManager
import org.tobi29.scapes.engine.sound.SoundManager
import org.tobi29.scapes.server.ScapesServerExecutor
import org.tobi29.scapes.server.ShutdownSafeScapesServerExecutor
import org.tobi29.scapes.server.format.sqlite.SQLiteSaveStorage
import org.tobi29.scapes.server.shell.ScapesServerHeadless
import org.tobi29.server.ConnectionManager
import org.tobi29.stdex.printerrln
import org.tobi29.utils.Version
import org.tobi29.utils.systemClock
import java.lang.ref.WeakReference
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import kotlin.system.exitProcess

object Scapes : Application() {
    override val id = "org.tobi29.scapes"

    override val execName = "scapes"
    override val fullName = "Scapes"

    override val version = Version(0, 0, 0)

    private val modeOption = cli.commandOption(
        setOf('m'), setOf("mode"), listOf("mode"),
        "Specify which mode to run"
    )
    private val debugOption = cli.commandFlag(
        setOf('d'), setOf("debug"),
        "Run in debug mode"
    )
    private val glesOption = cli.commandFlag(
        setOf('e'), setOf("gles"),
        "Use OpenGL ES"
    )
    private val socketspOption = cli.commandFlag(
        setOf('r'), setOf("socketsp"),
        "Use network socket for singleplayer"
    )
    private val touchOption = cli.commandFlag(
        setOf('t'), setOf("touch"),
        "Emulate touch interface"
    )
    private val configOption = cli.commandOption(
        setOf('c'), setOf("config"), listOf("directory"),
        "Config directory for server"
    )
    private val runDirArgument = cli.commandArgument(
        "run-dir", 0..1
    )

    override suspend fun execute(commandLine: CommandLine): StatusCode {
        if (commandLine.getBoolean(debugOption)) {
            Debug.enable()
        }
        if (commandLine.getBoolean(socketspOption)) {
            Debug.socketSingleplayer(true)
        }
        val mode = commandLine.get(modeOption) ?: "client"
        val runDir = commandLine.arguments[runDirArgument]
            ?.firstOrNull()?.let { path(it) }

        when (mode) {
            "client" -> {
                val home = runDir
                        ?: dataHome.resolve(appIDForData(id, name))
                System.setProperty("user.dir", home.toAbsolutePath().toString())

                System.setProperty(
                    "org.sqlite.lib.path",
                    System.getProperty("java.library.path")
                )

                var engineRef = WeakReference<ScapesEngine?>(null)
                Thread.setDefaultUncaughtExceptionHandler { _, e ->
                    try {
                        printerrln("Scapes crashed: $e")
                        e.printStackTrace()
                        val report = AccessController.doPrivileged(
                            PrivilegedAction {
                                crashReport(home, { engineRef.get() }, e)
                            })
                        ContainerGLFW.openFile(report)
                    } finally {
                        exitProcess(1)
                    }
                }

                val config = home.resolve("ScapesEngine.json")
                val configMap = readConfig(config).toMutTag()
                val saves: (ScapesClient) -> SaveStorage = {
                    SQLiteSaveStorage(home.resolve("saves"))
                }
                val useGLES = commandLine.getBoolean(glesOption)
                val emulateTouch = commandLine.getBoolean(touchOption)
                val font = try {
                    ScapesEngineLWJGL3.loadFont(
                        ClasspathPath(
                            ScapesClient::class.java.classLoader,
                            "assets/scapes/tobi29/font/QuicksandPro-Regular.otf"
                        )
                    )
                } catch (e: IOException) {
                    ScapesEngineLWJGL3.loadFont(
                        ClasspathPath(
                            ScapesClient::class.java.classLoader,
                            "assets/scapes/tobi29/font/QuicksandPro-Regular.ttf"
                        )
                    )
                }
                val container = ContainerGLFW(
                    "Scapes", emulateTouch,
                    useGLES = useGLES
                )
                val defaultGuiStyle: (ScapesEngine) -> GuiStyle = { engine ->
                    GuiBasicStyle(engine, FontRenderer(engine, font))
                }
                val engine = ScapesEngine(
                    container, defaultGuiStyle,
                    defaultBackgroundExecutor, configMap
                )
                engineRef = WeakReference(engine)

                val playlistsPath = home.resolve("playlists")
                createDirectories(playlistsPath.resolve("day"))
                createDirectories(playlistsPath.resolve("night"))
                createDirectories(playlistsPath.resolve("battle"))
                createDirectories(home.resolve("screenshots"))

                engine.files.registerFileSystem(
                    "Scapes",
                    ClasspathPath(
                        ScapesClient::class.java.classLoader,
                        "assets/scapes/tobi29"
                    )
                )
                engine.files.registerFileSystem(
                    "ScapesFrontend",
                    ClasspathPath(
                        ScapesClient::class.java.classLoader,
                        "assets/scapes/frontend/tobi29"
                    )
                )
                engine.registerComponent(
                    ScapesServerExecutor.COMPONENT,
                    ShutdownSafeScapesServerExecutor
                )
                engine.registerComponent(
                    DialogProvider.COMPONENT,
                    DialogProviderDesktop(
                        engine.container as? ContainerGLFW
                    )
                )
                engine.registerComponent(
                    InputManagerScapes.COMPONENT,
                    InputManagerScapes(engine, configMap.mapMut("Scapes"))
                )
                engine.registerComponent(
                    SoundManager.COMPONENT,
                    DefaultSoundManager(engine.sounds)
                )
                engine.registerComponent(ConnectionManager.COMPONENT,
                    ConnectionManager(engine.taskExecutor, 10)
                        .apply { workers(1) })
                engine.registerComponent(
                    ScapesClient.COMPONENT,
                    ScapesClient(engine, home, saves)
                )
                engine.switchState(
                    GameStateStartup(engine) { GameStateMenu(engine) })
                try {
                    engine.start()
                    try {
                        container.run(engine)
                    } catch (e: GraphicsCheckException) {
                        container.message(
                            Container.MessageType.ERROR,
                            "Scapes",
                            "Unable to initialize graphics:\n${e.message}"
                        )
                        return 1
                    }
                    runBlocking { engine.dispose() }
                } finally {
                    writeConfig(config, configMap.toTag())
                }
                return 0
            }
            "server" -> {
                val home = runDir
                        ?: path(System.getProperty("user.dir")).toAbsolutePath()
                System.setProperty("user.dir", home.toAbsolutePath().toString())
                val configPath = commandLine.get(configOption)
                val config = if (configPath != null) {
                    home.resolve(path(configPath)).toAbsolutePath()
                } else {
                    home
                }

                Thread.setDefaultUncaughtExceptionHandler { _, e ->
                    try {
                        printerrln("Scapes crashed: $e")
                        e.printStackTrace()
                        crashReport(home, e)
                    } finally {
                        exitProcess(1)
                    }
                }

                try {
                    val server =
                        ScapesServerHeadless(defaultBackgroundExecutor, config)
                    server.run(home)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return 200
                }
                return 0
            }
            else -> {
                printerrln("Unknown mode: $mode")
                return 254
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = executeMain(args)
}

private object PermissiveThreadFactory :
    ForkJoinPool.ForkJoinWorkerThreadFactory {
    override fun newThread(pool: ForkJoinPool?) =
        object : ForkJoinWorkerThread(pool) {}
}

private fun readConfig(path: FilePath) =
    try {
        if (exists(path)) {
            read(path, ::readJSON)
        } else {
            TagMap()
        }
    } catch (e: IOException) {
        printerrln("Failed to load config file: $e")
        TagMap()
    }

private fun writeConfig(
    path: FilePath,
    config: TagMap
) =
    try {
        write(path) { config.writeJSON(it) }
    } catch (e: IOException) {
        printerrln("Failed to store config file: $e")
    }

private fun crashReport(
    path: FilePath?,
    engine: () -> ScapesEngine?,
    e: Throwable
): FilePath {
    val time = timeZoneLocal.encodeWithOffset(systemClock())
    val crashReportFile = path?.resolve(
        crashReportName(
            "${
            time.dateTime.date.run {
                "${isoYear(year)}-${isoMonth(month)}-${isoDay(day)}"
            }}_${
            time.dateTime.time.run {
                "${isoHour(hour)}-${isoMinute(minute)}-${isoSecond(second)}"
            }}"
        )
    ) ?: createTempFile("CrashReport", ".txt")
    val debug = try {
        engine()?.debugMap()?.let {
            arrayOf(Pair("Debug values", crashReportSectionProperties(it)))
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    } ?: emptyArray()
    write(crashReportFile) {
        it.writeCrashReport(
            e, "Scapes",
            crashReportSectionStacktrace(e),
            crashReportSectionActiveThreads(),
            crashReportSectionSystemProperties(),
            *debug,
            crashReportSectionTime(isoOffsetDateTimeWithMillis(time))
        )
    }
    return crashReportFile
}

private fun crashReport(
    path: FilePath?,
    e: Throwable
): FilePath {
    val time = timeZoneLocal.encodeWithOffset(systemClock())
    val crashReportFile = path?.resolve(
        crashReportName(
            "${
            time.dateTime.date.run {
                "${isoYear(year)}-${isoMonth(month)}-${isoDay(day)}"
            }}_${
            time.dateTime.time.run {
                "${isoHour(hour)}-${isoMinute(minute)}-${isoSecond(second)}"
            }}"
        )
    ) ?: createTempFile("CrashReport", ".txt")
    write(crashReportFile) {
        it.writeCrashReport(
            e, "Scapes",
            crashReportSectionStacktrace(e),
            crashReportSectionActiveThreads(),
            crashReportSectionSystemProperties(),
            crashReportSectionTime(isoOffsetDateTimeWithMillis(time))
        )
    }
    return crashReportFile
}
