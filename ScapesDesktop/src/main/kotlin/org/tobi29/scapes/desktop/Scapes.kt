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
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.args.*
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.ContainerGLFW
import org.tobi29.scapes.engine.graphics.GraphicsCheckException
import org.tobi29.scapes.engine.utils.Crashable
import org.tobi29.scapes.engine.utils.IOException
import org.tobi29.scapes.engine.utils.io.filesystem.*
import org.tobi29.scapes.engine.utils.io.tag.json.readJSON
import org.tobi29.scapes.engine.utils.io.tag.json.writeJSON
import org.tobi29.scapes.engine.utils.tag.TagMap
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
        System.err.println(e.message)
        System.exit(255)
        return
    }

    if (commandLine.getBoolean(helpOption)) {
        val help = StringBuilder()
        help.append("Usage: scapes\n")
        options.printHelp(help)
        println(help)
        System.exit(0)
        return
    }
    if (commandLine.getBoolean(versionOption)) {
        println("Scapes $VERSION")
        System.exit(0)
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
            val dialogs: (ScapesClient) -> DialogProvider = {
                ScapesDesktop(it, { engine?.container as? ContainerGLFW })
            }
            val useGLES = commandLine.getBoolean(glesOption)
            val emulateTouch = commandLine.getBoolean(touchOption)
            val backend: (ScapesEngine) -> Container = {
                ContainerGLFW(it, emulateTouch, useGLES = useGLES)
            }
            engine = ScapesEngine(
                    { ScapesClient(it, home, pluginCache, saves, dialogs) },
                    backend, taskExecutor, configMap)
            try {
                engine.start()
                try {
                    engine.container.run()
                } catch (e: GraphicsCheckException) {
                    ScapesEngine.logger.error(
                            e) { "Failed to initialize graphics" }
                    engine.container.message(Container.MessageType.ERROR,
                            engine.game.name,
                            "Unable to initialize graphics:\n${e.message}")
                    exitProcess(1)
                }
            } finally {
                engine.dispose()
            }
            writeConfig(config, configMap.toTag())
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
                System.exit(200)
                return
            }
        }
        else -> {
            System.err.println("Unknown mode: " + mode)
            System.exit(254)
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
            System.err.println("Failed to load config file: $e")
            TagMap()
        }

private fun writeConfig(path: FilePath,
                        config: TagMap) =
        try {
            write(path) { config.writeJSON(it) }
        } catch (e: IOException) {
            System.err.println("Failed to store config file: $e")
        }

private fun crashHandler(path: FilePath,
                         engine: () -> ScapesEngine?) = object : Crashable {
    override fun crash(e: Throwable): Nothing {
        try {
            System.err.println("Engine crashed: $e")
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
    val crashReportFile = file(path)
    val debug = try {
        engine()?.debugMap()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    } ?: emptyMap<String, String>()
    writeCrashReport(e, crashReportFile, "ScapesEngine",
            debug)
}
