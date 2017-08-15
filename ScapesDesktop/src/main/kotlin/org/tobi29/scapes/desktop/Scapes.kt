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
import org.tobi29.scapes.engine.server.ConnectionManager
import org.tobi29.scapes.engine.utils.Crashable
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
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import org.tobi29.scapes.plugins.Sandbox
import org.tobi29.scapes.server.format.sqlite.SQLiteSaveStorage
import org.tobi29.scapes.server.shell.ScapesServerHeadless
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val optionsList = ArrayList<CommandOption>()
    val helpOption = CommandOption(setOf('h'), setOf("help"),
            "Print this text and exit").also { optionsList.add(it) }
    val versionOption = CommandOption(setOf('v'), setOf("version"),
            "Print version and exit").also { optionsList.add(it) }
    val modeOption = CommandOption(setOf('m'), setOf("mode"), 1,
            "Specify which mode to run").also { optionsList.add(it) }
    val debugOption = CommandOption(setOf('d'), setOf("debug"),
            "Run in debug mode").also { optionsList.add(it) }
    val glesOption = CommandOption(setOf('e'), setOf("gles"),
            "Use OpenGL ES").also { optionsList.add(it) }
    val socketspOption = CommandOption(setOf('r'), setOf("socketsp"),
            "Use network socket for singleplayer").also { optionsList.add(it) }
    val touchOption = CommandOption(setOf('t'), setOf("touch"),
            "Emulate touch interface").also { optionsList.add(it) }
    val configOption = CommandOption(setOf('c'), setOf("config"), 1,
            "Config directory for server").also { optionsList.add(it) }
    val nosandboxOption = CommandOption(setOf('n'), setOf("nosandbox"),
            "Disable sandbox").also { optionsList.add(it) }
    val options = optionsList.asSequence()

    val commandLine = try {
        val parser = TokenParser(options)
        args.forEach { parser.append(it) }
        val tokens = parser.finish()

        val commandLine = tokens.assemble()
        commandLine.validate()
        commandLine
    } catch (e: InvalidCommandLineException) {
        printerrln(e.message)
        exitProcess(255)
    }

    if (commandLine.getBoolean(helpOption)) {
        val help = StringBuilder()
        help.append("Usage: scapes\n")
        options.printHelp(help)
        println(help)
        return
    }
    if (commandLine.getBoolean(versionOption)) {
        println("Scapes $VERSION")
        return
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
    val home: FilePath
    if (commandLine.arguments.isNotEmpty()) {
        home = path(commandLine.arguments[0])
        System.setProperty("user.dir", home.toAbsolutePath().toString())
    } else {
        home = path(System.getProperty("user.dir")).toAbsolutePath()
    }
    when (mode) {
        "client" -> {
            val config = home.resolve("ScapesEngine.json")
            val cache = home.resolve("cache")
            val pluginCache = cache.resolve("plugins")
            var engine: ScapesEngine? = null
            val crashHandler = crashHandler(home, { engine })
            val taskExecutor = TaskExecutor(crashHandler, "Engine")
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
            engine = ScapesEngine(backend, defaultGuiStyle, taskExecutor,
                    configMap)

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
            val client = ScapesClient(engine, home, pluginCache, saves)
            engine.registerComponent(ScapesClient.COMPONENT, client)
            engine.switchState(
                    GameStateStartup(engine) { GameStateMenu(engine!!) })
            try {
                engine.start()
                try {
                    engine.container.run()
                } catch (e: GraphicsCheckException) {
                    ScapesEngine.logger.error(
                            e) { "Failed to initialize graphics" }
                    engine.container.message(Container.MessageType.ERROR,
                            "Scapes",
                            "Unable to initialize graphics:\n${e.message}")
                    exitProcess(1)
                }
                engine.dispose()
            } finally {
                writeConfig(config, configMap.toTag())
            }
        }
        "server" -> {
            val configPath = commandLine.get(configOption)
            val config = if (configPath != null) {
                home.resolve(path(configPath)).toAbsolutePath()
            } else {
                home
            }
            try {
                val server = ScapesServerHeadless(config)
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

private fun crashHandler(path: FilePath,
                         engine: () -> ScapesEngine?) = object : Crashable {
    override fun crash(e: Throwable): Nothing {
        try {
            printerrln("Engine crashed: $e")
            e.printStackTrace()
            crashReport(path, engine, e)
            ContainerGLFW.openFile(path)
        } finally {
            exitProcess(1)
        }
    }
}

private fun crashReport(path: FilePath,
                        engine: () -> ScapesEngine?,
                        e: Throwable) {
    val time = timeZoneLocal.encodeWithOffset(systemClock()).first()
    val crashReportFile = path.resolve(crashReportName(
            "${
            time.dateTime.date.run {
                "${isoYear(year)}-${isoMonth(month)}-${isoDay(day)}"
            }}_${
            time.dateTime.time.run {
                "${isoHour(hour)}-${isoMinute(minute)}-${isoSecond(second)}"
            }}"))
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
}
