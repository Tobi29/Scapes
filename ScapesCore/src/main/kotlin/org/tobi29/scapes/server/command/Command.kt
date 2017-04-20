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

import org.tobi29.scapes.engine.args.CommandOption

object Command {
    open class Compiled(private val commands: Collection<() -> Unit>) {
        fun execute(): List<Output> {
            val outputs = ArrayList<Output>(commands.size)
            commands.forEach { command ->
                try {
                    command()
                } catch (e: CommandException) {
                    outputs.add(Output(252, e.message ?: ""))
                }
            }
            return outputs
        }
    }

    class Output(val returnCode: Int,
                 val out: String) {

        init {
            if (returnCode < 0 || returnCode > 255) {
                throw IllegalArgumentException(
                        "Return code must be in range of 0 to 255")
            }
        }

        override fun toString() = out
    }

    class Null(output: Output) : Compiled(
            listOf<() -> Unit>({ throw CommandException(output) }))

    class CommandException(val returnCode: Int,
                           message: String) : Exception(
            message) {

        constructor(output: Output) : this(output.returnCode, output.out)
    }

    fun <O, T> requireGet(supplier: (O) -> T?,
                          option: O): T {
        return supplier(option) ?: error("Missing argument: $option")
    }

    fun error(msg: String): Nothing {
        throw Command.CommandException(253, msg)
    }

    fun requirePermission(executor: Executor,
                          level: Int) {
        if (executor.permissionLevel() < level) {
            throw Command.CommandException(243, "Missing permissions")
        }
    }

    fun requirePermission(executor: Executor,
                          level: Int,
                          option: CommandOption) {
        if (executor.permissionLevel() < level) {
            throw Command.CommandException(243,
                    "Missing permissions for: ${option.simpleName}")
        }
    }
}
