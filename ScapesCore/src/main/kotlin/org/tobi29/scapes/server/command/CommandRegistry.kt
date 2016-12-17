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

package org.tobi29.scapes.server.command

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CommandRegistry constructor(private val prefix: String = "") {
    private val commands = ConcurrentHashMap<String, (List<String>, Executor) -> Command.Compiled>()

    fun register(usage: String,
                 level: Int,
                 optionSupplier: Command.CommandOptions.() -> Unit,
                 compiler: (Command.Arguments, Executor, MutableCollection<() -> Unit>) -> Unit) {
        val split = usage.split(' ', limit = 2)
        val name = split[0]
        commands.put(name,
                compiler(prefix + usage, level, optionSupplier, compiler))
    }

    fun group(name: String): CommandRegistry {
        val registry = CommandRegistry(prefix + name + ' ')
        commands.put(name,
                { args, executor -> registry[args, name + ' ', executor] })
        return registry
    }

    operator fun get(line: String,
                     executor: Executor): Command.Compiled {
        return get(line.split(PATTERN), "", executor)
    }

    private operator fun get(split: List<String>,
                             prefix: String,
                             executor: Executor): Command.Compiled {
        val pair = command(split)
        val compiler = commands[pair.first] ?: return Command.Null(
                Command.Output(255,
                        "Unknown command: " + prefix + pair.first))
        return compiler(pair.second, executor)
    }

    private fun command(split: List<String>): Pair<String, List<String>> {
        if (split.isEmpty()) {
            return Pair("", emptyList())
        }
        val name = split[0]
        val args: List<String>
        if (split.size == 1) {
            args = emptyList()
        } else {
            args = (0..split.lastIndex - 1).map { split[it + 1] }
        }
        return Pair(name, args)
    }

    private fun compiler(usage: String,
                         level: Int,
                         optionSupplier: (Command.CommandOptions) -> Unit,
                         compiler: (Command.Arguments, Executor, MutableCollection<() -> Unit>) -> Unit): (List<String>, Executor) -> Command.Compiled {
        return { args, executor ->
            val parser = DefaultParser()
            try {
                requirePermission(executor, level)
                val options = Options()
                options.addOption("h", "help", false, "Display this help")
                optionSupplier(Command.CommandOptions(options))
                val commandLine = parser.parse(options, args.toTypedArray())
                if (commandLine.hasOption('h')) {
                    val helpFormatter = HelpFormatter()
                    val writer = StringWriter()
                    val printWriter = PrintWriter(writer)
                    helpFormatter.printHelp(printWriter, 74, usage, null,
                            options, 1, 3, null, false)
                    val help = writer.toString()
                    Command.Null(Command.Output(1, help))
                } else {
                    val commands = ArrayList<() -> Unit>()
                    compiler(Command.Arguments(commandLine),
                            executor, commands)
                    Command.Compiled(commands)
                }
            } catch (e: ParseException) {
                Command.Null(Command.Output(254,
                        e.javaClass.simpleName + ": " + (e.message ?: "")))
            } catch (e: Command.CommandException) {
                Command.Null(Command.Output(253, e.message ?: ""))
            }
        }
    }


    companion object {
        private val PATTERN = "[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()
    }
}
