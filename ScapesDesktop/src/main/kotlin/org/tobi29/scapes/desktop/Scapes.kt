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

import org.apache.commons.cli.*
import org.tobi29.scapes.Debug
import org.tobi29.scapes.VERSION
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.Container
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.ContainerGLFW
import org.tobi29.scapes.engine.graphics.GraphicsCheckException
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
import java.io.IOException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val options = Options()
    options.run {
        addOption("h", "help", false, "Print this text and exit")
        addOption("v", "version", false, "Print version and exit")
        addOption("m", "mode", true, "Specify which mode to run")
        addOption("d", "debug", false, "Run in debug mode")
        addOption("r", "socketsp", false, "Use network socket for singleplayer")
        addOption("t", "touch", false, "Emulate touch interface")
        addOption("c", "config", true, "Config directory for server")
        addOption("n", "nosandbox", false, "Disable sandbox")
    }
    val parser = DefaultParser()
    val commandLine: CommandLine
    try {
        commandLine = parser.parse(options, args)
    } catch (e: ParseException) {
        System.err.println(e.message)
        System.exit(255)
        return
    }

    if (commandLine.hasOption('h')) {
        val helpFormatter = HelpFormatter()
        helpFormatter.printHelp("scapes", options)
        System.exit(0)
        return
    }
    if (commandLine.hasOption('v')) {
        println("Scapes $VERSION")
        System.exit(0)
        return
    }
    if (commandLine.hasOption('n')) {
        println("----------------------------------------")
        println("Sandbox disabled by command line option!")
        println("Do NOT connect to untrusted servers with")
        println("this option enabled!")
        println("----------------------------------------")
    } else {
        Sandbox.sandbox()
    }
    if (commandLine.hasOption('d')) {
        Debug.enable()
    }
    if (commandLine.hasOption('r')) {
        Debug.socketSingleplayer(true)
    }
    val cmdArgs = commandLine.args
    val mode = commandLine.getOptionValue('m', "client")
    val home: FilePath
    if (cmdArgs.isNotEmpty()) {
        home = path(
                cmdArgs[0].replace("\$HOME", System.getProperty("user.home")))
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
            val crashHandler = ScapesEngine.crashHandler(home, { engine })
            val taskExecutor = TaskExecutor(crashHandler, "Engine")
            val configMap = readConfig(config).toMutTag()
            val saves: (ScapesClient) -> SaveStorage = {
                SQLiteSaveStorage(home.resolve("saves"))
            }
            val emulateTouch = commandLine.hasOption('t')
            val backend: (ScapesEngine) -> Container = {
                ContainerGLFW(it, emulateTouch)
            }
            engine = ScapesEngine(
                    { ScapesClient(it, home, pluginCache, saves) }, backend,
                    taskExecutor, configMap)
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
            val config: FilePath
            if (commandLine.hasOption('c')) {
                config = home.resolve(
                        path(commandLine.getOptionValue('c'))).toAbsolutePath()
            } else {
                config = home
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
