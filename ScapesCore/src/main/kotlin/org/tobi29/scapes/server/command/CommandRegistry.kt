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

import org.tobi29.args.*
import org.tobi29.stdex.ConcurrentHashMap
import org.tobi29.stdex.computeAbsent

class CommandRegistry : CommandRegistrar {
    private val commands1 = ConcurrentHashMap<List<String>, Pair<CommandConfig, MutableList<CommandElement>>>()
    private val commands2 = ConcurrentHashMap<String, CommandConfig>()
    private val commands3 = ConcurrentHashMap<List<String>, (CommandLine, Executor) -> Command.Compiled>()

    private val helpOption = CommandOption(
            setOf('h'), setOf("help"), "Display this help")

    private fun configFor(path: List<String>,
                          parentAdd: (CommandConfig) -> Unit) =
            commands1.computeAbsent(path) {
                ArrayList<CommandElement>().let {
                    it.add(helpOption)
                    (CommandConfig(path.last(), it) to it)
                            .also { (element, _) ->
                                parentAdd(element)
                            }
                }
            }

    fun subcommand(path: Iterable<String>): Pair<List<CommandConfig>, MutableCollection<CommandElement>> {
        val pathList = path.toList()
        if (pathList.isEmpty()) throw IllegalArgumentException("Empty path")
        val (parent, parentElements: (CommandConfig) -> Unit) =
                if (pathList.size == 1) emptyList<CommandConfig>() to { element: CommandConfig ->
                    commands2[pathList.last()] = element
                }
                else subcommand(pathList.dropLast(1))
                        .let { (parent, parentElements) ->
                            parent to { element: CommandConfig ->
                                parentElements.add(element)
                                Unit
                            }
                        }
        val (command, elements) = configFor(pathList, parentElements)
        return (parent + command) to elements
    }

    override fun register(
            path: Iterable<String>,
            level: Int,
            block: MutableCollection<CommandElement>.() -> Command.(CommandLine, Executor, MutableCollection<() -> Unit>) -> Unit
    ) {
        val (command, elements) = subcommand(path)
        val compiler = block(elements)
        commands3[command.map { it.name }] = compiler(level, helpOption,
                compiler)
    }

    operator fun get(line: String,
                     executor: Executor): Command.Compiled {
        val commandLine = try {
            val tokens = line.tokenize()
            if (tokens.isEmpty()) throw EmptyCommandException()
            val first = tokens.first()
            val args = tokens.subList(1, tokens.size)
            val command = commands2[first]
                    ?: throw UnknownCommandException(tokens, listOf(first))
            command.parseCommandLine(args)
        } catch (e: InvalidTokensException) {
            return Command.Null(Command.Output(255, e.message ?: ""))
        }
        val compiler = commands3[commandLine.command.map { it.name }]
                ?: return Command.Null(Command.Output(255,
                        "Unknown command: ${commandLine.command.joinToString(
                                " ") { it.name }}"))
        return compiler(commandLine, executor)
    }

    private fun compiler(
            level: Int,
            helpOption: CommandOption,
            compiler: Command.(CommandLine, Executor, MutableCollection<() -> Unit>) -> Unit
    ): (CommandLine, Executor) -> Command.Compiled {
        return { commandLine, executor ->
            try {
                Command.requirePermission(executor, level)

                if (commandLine.getBoolean(helpOption)) {
                    val help = commandLine.command.printHelp()
                    Command.Null(Command.Output(1, help))
                } else {
                    val commands = ArrayList<() -> Unit>()
                    compiler(Command, commandLine, executor, commands)
                    Command.Compiled(commands)
                }
            } catch (e: InvalidTokensException) {
                Command.Null(Command.Output(255, e.message ?: ""))
            } catch (e: Command.CommandException) {
                Command.Null(Command.Output(254, e.message ?: ""))
            }
        }
    }
}

class CommandGroupRegistry(private val parent: CommandRegistrar,
                           private val name: String) : CommandRegistrar {
    override fun register(
            path: Iterable<String>,
            level: Int,
            block: MutableCollection<CommandElement>.() -> Command.(CommandLine, Executor, MutableCollection<() -> Unit>) -> Unit
    ) = parent.register(listOf(name) + path, level, block)

}

interface CommandRegistrar {
    fun register(
            path: Iterable<String>,
            level: Int,
            block: MutableCollection<CommandElement>.() -> Command.(CommandLine, Executor, MutableCollection<() -> Unit>) -> Unit)

    fun register(
            name: String,
            level: Int,
            block: MutableCollection<CommandElement>.() -> Command.(CommandLine, Executor, MutableCollection<() -> Unit>) -> Unit
    ) = register(listOf(name), level, block)

    fun group(name: String): CommandGroupRegistry =
            CommandGroupRegistry(this, name)
}
