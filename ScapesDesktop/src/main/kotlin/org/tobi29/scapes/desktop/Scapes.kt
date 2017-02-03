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
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.get
import org.tobi29.scapes.plugins.Sandbox
import org.tobi29.scapes.server.format.sqlite.SQLiteSaveStorage
import org.tobi29.scapes.server.shell.ScapesServerHeadless
import java.io.IOException

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
        home = get(
                cmdArgs[0].replace("\$HOME", System.getProperty("user.home")))
        System.setProperty("user.dir", home.toAbsolutePath().toString())
    } else {
        home = get(System.getProperty("user.dir")).toAbsolutePath()
    }
    when (mode) {
        "client" -> {
            val saves: (ScapesClient) -> SaveStorage = { scapes ->
                SQLiteSaveStorage(home.resolve("saves"))
            }
            var backend = ScapesEngine.loadBackend()
            if (commandLine.hasOption('t')) {
                backend = ScapesEngine.emulateTouch(backend)
            }
            val engine = ScapesEngine(
                    { ScapesClient(it, saves) },
                    backend,
                    home, Debug.enabled())
            System.exit(engine.run())
        }
        "server" -> {
            val config: FilePath
            if (commandLine.hasOption('c')) {
                config = home.resolve(
                        get(commandLine.getOptionValue('c'))).toAbsolutePath()
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
