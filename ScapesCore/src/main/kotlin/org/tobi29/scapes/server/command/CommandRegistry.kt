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

package org.tobi29.scapes.server.command

import org.tobi29.scapes.engine.args.*
import org.tobi29.scapes.engine.utils.ConcurrentHashMap

class CommandRegistry constructor(private val prefix: String = "") {
    private val commands = ConcurrentHashMap<String, (List<String>, Executor) -> Command.Compiled>()

    fun register(
            usage: String,
            level: Int,
            block: MutableCollection<CommandOption>.() -> Command.(CommandLine, Executor, MutableCollection<() -> Unit>
            ) -> Unit) {
        val split = usage.split(' ', limit = 2)
        val name = split[0]
        val helpOption = CommandOption(
                setOf('h'), setOf("help"), "Display this help")
        val options = ArrayList<CommandOption>()
        options.add(helpOption)
        val compiler = block(options)
        commands.put(name,
                compiler(prefix + usage, level, helpOption, options, compiler))
    }

    fun group(name: String): CommandRegistry {
        val registry = CommandRegistry(prefix + name + ' ')
        commands.put(name,
                { args, executor -> registry[args, name + ' ', executor] })
        return registry
    }

    operator fun get(line: String,
                     executor: Executor): Command.Compiled {
        return get(line.tokenize(), "", executor)
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
            args = (0 until split.lastIndex).map { split[it + 1] }
        }
        return Pair(name, args)
    }

    private fun compiler(
            usage: String,
            level: Int,
            helpOption: CommandOption,
            options: Iterable<CommandOption>,
            compiler: Command.(CommandLine, Executor, MutableCollection<() -> Unit>) -> Unit
    ): (List<String>, Executor) -> Command.Compiled {
        return { args, executor ->
            try {
                Command.requirePermission(executor, level)
                val parser = TokenParser(options)
                args.forEach { parser.append(it) }
                val tokens = parser.finish()

                val commandLine = tokens.assemble()
                commandLine.validate()

                if (commandLine.getBoolean(helpOption)) {
                    val help = StringBuilder()
                    help.append("Usage: ").append(usage).append('\n')
                    options.printHelp(help)
                    Command.Null(Command.Output(1, help.toString()))
                } else {
                    val commands = ArrayList<() -> Unit>()
                    compiler(Command, commandLine, executor, commands)
                    Command.Compiled(commands)
                }
            } catch (e: InvalidCommandLineException) {
                Command.Null(Command.Output(255, e.message ?: ""))
            } catch (e: Command.CommandException) {
                Command.Null(Command.Output(254, e.message ?: ""))
            }
        }
    }
}
