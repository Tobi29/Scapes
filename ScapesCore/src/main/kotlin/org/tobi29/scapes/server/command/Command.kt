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
import org.tobi29.scapes.engine.args.InvalidCommandLineException
import org.tobi29.scapes.engine.math.vector.Vector2d
import org.tobi29.scapes.engine.math.vector.Vector2i
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.math.vector.Vector3i

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

/**
 * Reads a 2 dimensional vector from the given strings
 * @param values The string to read from
 * @returns A vector containing the values from the strings
 * @throws InvalidCommandLineException If there were not 2 strings or invalid numbers
 */
fun getVector2d(values: List<String>): Vector2d {
    if (values.size != 2) {
        throw InvalidCommandLineException(
                "Unable to parse vector2d: ${values.joinToString(
                        separator = " ")}")
    }
    try {
        return Vector2d(values[0].toDouble(), values[1].toDouble())
    } catch (e: NumberFormatException) {
        throw InvalidCommandLineException(
                "Unable to parse vector2d: ${values.joinToString(
                        separator = " ")}")
    }
}

/**
 * Reads a 3 dimensional vector from the given strings
 * @param values The string to read from
 * @returns A vector containing the values from the strings
 * @throws InvalidCommandLineException If there were not 3 strings or invalid numbers
 */
fun getVector3d(values: List<String>): Vector3d {
    if (values.size != 3) {
        throw InvalidCommandLineException(
                "Unable to parse vector3d: ${values.joinToString(
                        separator = " ")}")
    }
    try {
        return Vector3d(values[0].toDouble(), values[1].toDouble(),
                values[2].toDouble())
    } catch (e: NumberFormatException) {
        throw InvalidCommandLineException(
                "Unable to parse vector3d: ${values.joinToString(
                        separator = " ")}")
    }
}

/**
 * Reads a 2 dimensional vector from the given strings
 * @param values The string to read from
 * @returns A vector containing the values from the strings
 * @throws InvalidCommandLineException If there were not 2 strings or invalid numbers
 */
fun getVector2i(values: List<String>): Vector2i {
    if (values.size != 2) {
        throw InvalidCommandLineException(
                "Unable to parse vector2i: ${values.joinToString(
                        separator = " ")}")
    }
    try {
        return Vector2i(values[0].toInt(), values[1].toInt())
    } catch (e: NumberFormatException) {
        throw InvalidCommandLineException(
                "Unable to parse vector2i: ${values.joinToString(
                        separator = " ")}")
    }
}

/**
 * Reads a 3 dimensional vector from the given strings
 * @param values The string to read from
 * @returns A vector containing the values from the strings
 * @throws InvalidCommandLineException If there were not 3 strings or invalid numbers
 */
fun getVector3i(values: List<String>): Vector3i {
    if (values.size != 3) {
        throw InvalidCommandLineException(
                "Unable to parse vector3i: ${values.joinToString(
                        separator = " ")}")
    }
    try {
        return Vector3i(values[0].toInt(), values[1].toInt(),
                values[2].toInt())
    } catch (e: NumberFormatException) {
        throw InvalidCommandLineException(
                "Unable to parse vector3i: ${values.joinToString(
                        separator = " ")}")
    }
}
