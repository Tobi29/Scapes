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

import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.runBlocking
import org.tobi29.scapes.Debug
import org.tobi29.scapes.VERSION
import org.tobi29.scapes.client.DialogProvider
import org.tobi29.scapes.client.InputManagerScapes
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.states.GameStateMenu
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.GameStateStartup
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.args.*
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.ContainerGLFW
import org.tobi29.scapes.engine.chrono.*
import org.tobi29.scapes.engine.graphics.FontRenderer
import org.tobi29.scapes.engine.graphics.GraphicsCheckException
import org.tobi29.scapes.engine.gui.GuiBasicStyle
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.platform.appIDForCache
import org.tobi29.scapes.engine.platform.appIDForData
import org.tobi29.scapes.engine.platform.cacheHome
import org.tobi29.scapes.engine.platform.dataHome
import org.tobi29.scapes.engine.server.ConnectionManager
import org.tobi29.scapes.engine.utils.io.*
import org.tobi29.scapes.engine.utils.io.classpath.ClasspathPath
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.tag.json.readJSON
import org.tobi29.scapes.engine.utils.io.tag.json.writeJSON
import org.tobi29.scapes.engine.utils.printerrln
import org.tobi29.scapes.engine.utils.systemClock
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.mapMut
import org.tobi29.scapes.engine.utils.tag.toMutTag
import org.tobi29.scapes.engine.utils.tag.toTag
import org.tobi29.scapes.engine.utils.tryWrap
import org.tobi29.scapes.engine.utils.unwrapOr
import org.tobi29.scapes.plugins.Sandbox
import org.tobi29.scapes.server.format.sqlite.SQLiteSaveStorage
import org.tobi29.scapes.server.shell.ScapesServerHeadless
import java.lang.ref.WeakReference
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import kotlin.system.exitProcess

const val APP_ID = "org.tobi29.scapes"
const val APP_NAME = "Scapes"
const val APP_EXEC = "scapes"

private val helpOption = CommandOption(
        setOf('h'), setOf("help"), "Print this text and exit")
private val versionOption = CommandOption(
        setOf('v'), setOf("version"), "Print version and exit")
private val modeOption = CommandOption(
        setOf('m'), setOf("mode"), 1, "Specify which mode to run")
private val debugOption = CommandOption(
        setOf('d'), setOf("debug"), "Run in debug mode")
private val glesOption = CommandOption(
        setOf('e'), setOf("gles"), "Use OpenGL ES")
private val socketspOption = CommandOption(
        setOf('r'), setOf("socketsp"), "Use network socket for singleplayer")
private val touchOption = CommandOption(
        setOf('t'), setOf("touch"), "Emulate touch interface")
private val configOption = CommandOption(
        setOf('c'), setOf("config"), 1, "Config directory for server")
private val nosandboxOption = CommandOption(
        setOf('n'), setOf("nosandbox"), "Disable sandbox")
private val options = listOf(helpOption, versionOption, modeOption, debugOption,
        glesOption, socketspOption, touchOption, configOption, nosandboxOption)

fun main(args: Array<String>) {
    val commandLine = tryWrap<CommandLine, InvalidCommandLineException> {
        options.parseCommandLine(args.asIterable())
    }.unwrapOr { e ->
        printerrln(e.message)
        exitProcess(1)
    }

    if (commandLine.getBoolean(helpOption)) {
        val help = StringBuilder()
        help.append("Usage: $APP_EXEC [run directory]\n")
        options.printHelp(help)
        println(help)
        exitProcess(0)
    }
    if (commandLine.getBoolean(versionOption)) {
        println("Scapes $VERSION")
        exitProcess(0)
    }
    if (commandLine.getBoolean(nosandboxOption)) {
        println("----------------------------------------")
        println("Sandbox disabled by command line option!")
        println("Do NOT connect to untrusted servers with")
        println("this option enabled!")
        println("----------------------------------------")
    } else {
        Sandbox.sandbox()
    }
    if (commandLine.getBoolean(debugOption)) {
        Debug.enable()
    }
    if (commandLine.getBoolean(socketspOption)) {
        Debug.socketSingleplayer(true)
    }
    val mode = commandLine.get(modeOption) ?: "client"
    val runDir = if (commandLine.arguments.isNotEmpty()) {
        path(commandLine.arguments[0])
    } else null

    when (mode) {
        "client" -> {
            val home = runDir
                    ?: dataHome.resolve(appIDForData(APP_ID, APP_NAME))
            System.setProperty("user.dir", home.toAbsolutePath().toString())
            val cache = cacheHome.resolve(appIDForCache(APP_ID, APP_NAME))
            val config = home.resolve("ScapesEngine.json")
            val pluginCache = cache.resolve("plugins")
            var engineRef = WeakReference<ScapesEngine?>(null)
            Thread.setDefaultUncaughtExceptionHandler { _, e ->
                try {
                    printerrln("Scapes crashed: $e")
                    e.printStackTrace()
                    ContainerGLFW.openFile(
                            crashReport(home, { engineRef.get() }, e))
                } finally {
                    exitProcess(1)
                }
            }
            val taskExecutor =
                    ForkJoinPool(ForkJoinPool.getCommonPoolParallelism(),
                            PermissiveThreadFactory, null, false)
                            .asCoroutineDispatcher()
            val configMap = readConfig(config).toMutTag()
            val saves: (ScapesClient) -> SaveStorage = {
                SQLiteSaveStorage(home.resolve("saves"))
            }
            val useGLES = commandLine.getBoolean(glesOption)
            val emulateTouch = commandLine.getBoolean(touchOption)
            val backend: (ScapesEngine) -> Container = {
                ContainerGLFW(it, "Scapes", emulateTouch, useGLES = useGLES)
            }
            val defaultGuiStyle: (ScapesEngine) -> GuiStyle = { engine ->
                val font = FontRenderer(engine, engine.container.loadFont(
                        ClasspathPath(ScapesClient::class.java.classLoader,
                                "assets/scapes/tobi29/font/QuicksandPro-Regular.ttf")) ?:
                        engine.container.loadFont(ClasspathPath(
                                ScapesClient::class.java.classLoader,
                                "assets/scapes/tobi29/font/QuicksandPro-Regular.otf")) ?:
                        throw IllegalStateException(
                                "Failed to load default font"))
                GuiBasicStyle(engine, font)
            }
            val engine = ScapesEngine(backend, defaultGuiStyle, taskExecutor,
                    configMap)
            engineRef = WeakReference(engine)

            val playlistsPath = home.resolve("playlists")
            createDirectories(playlistsPath.resolve("day"))
            createDirectories(playlistsPath.resolve("night"))
            createDirectories(playlistsPath.resolve("battle"))
            createDirectories(home.resolve("plugins"))
            createDirectories(home.resolve("screenshots"))
            createDirectories(pluginCache)
            FileCache.check(pluginCache)

            engine.files.registerFileSystem("Scapes",
                    ClasspathPath(ScapesClient::class.java.classLoader,
                            "assets/scapes/tobi29"))
            engine.registerComponent(DialogProvider.COMPONENT,
                    DialogProviderDesktop(engine.container as? ContainerGLFW))
            engine.registerComponent(InputManagerScapes.COMPONENT,
                    InputManagerScapes(engine, configMap.mapMut("Scapes")))
            engine.registerComponent(ConnectionManager.COMPONENT,
                    ConnectionManager(engine.taskExecutor, 10)
                            .apply { workers(1) })
            engine.registerComponent(ScapesClient.COMPONENT,
                    ScapesClient(engine, home, pluginCache, saves))
            engine.switchState(
                    GameStateStartup(engine) { GameStateMenu(engine) })
            try {
                engine.start()
                try {
                    engine.container.run()
                } catch (e: GraphicsCheckException) {
                    engine.container.message(Container.MessageType.ERROR,
                            "Scapes",
                            "Unable to initialize graphics:\n${e.message}")
                    exitProcess(1)
                }
                runBlocking { engine.dispose() }
            } finally {
                writeConfig(config, configMap.toTag())
            }
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
            val taskExecutor =
                    ForkJoinPool(ForkJoinPool.getCommonPoolParallelism(),
                            PermissiveThreadFactory, null, false)
                            .asCoroutineDispatcher()
            try {
                val server = ScapesServerHeadless(taskExecutor, config)
                server.run(home)
            } catch (e: IOException) {
                e.printStackTrace()
                exitProcess(200)
            }
        }
        else -> {
            printerrln("Unknown mode: $mode")
            exitProcess(254)
        }
    }
}

private object PermissiveThreadFactory : ForkJoinPool.ForkJoinWorkerThreadFactory {
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

private fun writeConfig(path: FilePath,
                        config: TagMap) =
        try {
            write(path) { config.writeJSON(it) }
        } catch (e: IOException) {
            printerrln("Failed to store config file: $e")
        }

private fun crashReport(path: FilePath?,
                        engine: () -> ScapesEngine?,
                        e: Throwable): FilePath {
    val time = timeZoneLocal.encodeWithOffset(systemClock()).first()
    val crashReportFile = path?.resolve(crashReportName(
            "${
            time.dateTime.date.run {
                "${isoYear(year)}-${isoMonth(month)}-${isoDay(day)}"
            }}_${
            time.dateTime.time.run {
                "${isoHour(hour)}-${isoMinute(minute)}-${isoSecond(second)}"
            }}")) ?: createTempFile("CrashReport", ".txt")
    val debug = try {
        engine()?.debugMap()?.let {
            arrayOf(Pair("Debug values", crashReportSectionProperties(it)))
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    } ?: emptyArray()
    write(crashReportFile) {
        it.writeCrashReport(e, "Scapes",
                crashReportSectionStacktrace(e),
                crashReportSectionActiveThreads(),
                crashReportSectionSystemProperties(),
                *debug,
                crashReportSectionTime(isoOffsetDateTimeWithMillis(time)))
    }
    return crashReportFile
}

private fun crashReport(path: FilePath?,
                        e: Throwable): FilePath {
    val time = timeZoneLocal.encodeWithOffset(systemClock()).first()
    val crashReportFile = path?.resolve(crashReportName(
            "${
            time.dateTime.date.run {
                "${isoYear(year)}-${isoMonth(month)}-${isoDay(day)}"
            }}_${
            time.dateTime.time.run {
                "${isoHour(hour)}-${isoMinute(minute)}-${isoSecond(second)}"
            }}")) ?: createTempFile("CrashReport", ".txt")
    write(crashReportFile) {
        it.writeCrashReport(e, "Scapes",
                crashReportSectionStacktrace(e),
                crashReportSectionActiveThreads(),
                crashReportSectionSystemProperties(),
                crashReportSectionTime(isoOffsetDateTimeWithMillis(time)))
    }
    return crashReportFile
}
