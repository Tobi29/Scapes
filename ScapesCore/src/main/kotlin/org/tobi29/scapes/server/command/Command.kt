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

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.tobi29.scapes.engine.utils.PlatformName
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.readOnly

object Command {

    class CommandOptions(private val options: Options) {

        fun add(opt: String,
                longOpt: String,
                hasArg: Boolean,
                description: String) {
            options.addOption(opt, longOpt, hasArg, description)
        }

        fun add(opt: String,
                longOpt: String,
                arguments: Int,
                description: String) {
            val option = Option(opt, longOpt, true, description)
            option.args = arguments
            options.addOption(option)
        }
    }

    class Arguments(private val commandLine: CommandLine) {

        val args = commandLine.argList.readOnly()

        fun hasOption(option: Char): Boolean {
            return commandLine.hasOption(option)
        }

        fun option(option: Char): String? {
            return commandLine.getOptionValue(option)
        }

        fun optionArray(option: Char): Array<String>? {
            return commandLine.getOptionValues(option)
        }

        fun option(option: Char,
                   def: String): String {
            return commandLine.getOptionValue(option, def)
        }

        @PlatformName("optionNullable")
        fun option(option: Char,
                   def: String?): String? {
            return commandLine.getOptionValue(option, def)
        }

        fun arg(i: Int): String? {
            val args = commandLine.args
            if (i < 0 || i >= args.size) {
                return null
            }
            return args[i]
        }
    }

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

        override fun toString(): String {
            return "$out ($returnCode)"
        }
    }

    class Null(output: Output) : Compiled(
            listOf<() -> Unit>({ throw CommandException(output) }))

    class CommandException(val returnCode: Int,
                           message: String) : Exception(
            message) {

        constructor(output: Output) : this(output.returnCode, output.out)
    }
}

fun getInt(value: String): Int {
    try {
        return value.toInt()
    } catch (e: NumberFormatException) {
        throw Command.CommandException(253, "Unable to parse int: " + value)
    }

}

fun getLong(value: String): Long {
    try {
        return value.toLong()
    } catch (e: NumberFormatException) {
        throw Command.CommandException(253,
                "Unable to parse long: " + value)
    }

}

fun getFloat(value: String): Float {
    try {
        return value.toFloat()
    } catch (e: NumberFormatException) {
        throw Command.CommandException(253, "Unable to parse float: $value")
    }

}

fun getDouble(value: String): Double {
    try {
        return value.toDouble()
    } catch (e: NumberFormatException) {
        throw Command.CommandException(253, "Unable to parse double: $value")
    }

}

fun getVector3d(values: Array<String>): Vector3d {
    if (values.size != 3) {
        throw Command.CommandException(253,
                "Unable to parse vector3d: ${values.joinToString(
                        separator = " ")}")
    }
    try {
        return Vector3d(values[0].toDouble(), values[1].toDouble(),
                values[2].toDouble())
    } catch (e: NumberFormatException) {
        throw Command.CommandException(253,
                "Unable to parse vector3d: ${values.joinToString(
                        separator = " ")}")
    }
}

fun Command.Arguments.requireOption(option: Char): String {
    return require(option(option), option)
}

fun Command.Arguments.requireOptionArray(option: Char): Array<String> {
    return require(optionArray(option), option)
}

fun Command.Arguments.requireOption(option: Char,
                                    def: String?): String {
    return require(option(option, def), option)
}

fun require(args: Command.Arguments,
            name: Char) {
    if (!args.hasOption(name)) {
        throw Command.CommandException(253, "Missing argument: $name")
    }
}

fun <T> require(value: T?,
                name: Char): T {
    if (value == null) {
        throw Command.CommandException(253, "Missing argument: $name")
    }
    return value
}

fun <T> require(value: T?,
                name: String): T {
    if (value == null) {
        throw Command.CommandException(253, "Missing argument: $name")
    }
    return value
}

fun <O, T> requireGet(supplier: (O) -> T?,
                      option: O): T {
    return supplier(option) ?: throw Command.CommandException(253,
            "Missing argument: $option")
}

fun error(msg: String) {
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
                      value: Char) {
    if (executor.permissionLevel() < level) {
        throw Command.CommandException(243,
                "Missing permissions for: " + value)
    }
}
