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
import org.tobi29.math.vector.Vector2d
import org.tobi29.math.vector.Vector2i
import org.tobi29.math.vector.Vector3d
import org.tobi29.math.vector.Vector3i

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
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector2d]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector2d] or `null` if the option was not found
 */
fun CommandLine.getVector2d(option: CommandOption) =
        getList(option)?.let {
            run {
                if (it.size != 2) return@run null
                val x = it[0].toDoubleOrNull() ?: return@run null
                val y = it[1].toDoubleOrNull() ?: return@run null
                Vector2d(x, y)
            } ?: throw InvalidOptionArgumentException(null, this, option, it)
        }

/**
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector3d]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector3d] or `null` if the option was not found
 */
fun CommandLine.getVector3d(option: CommandOption) =
        getList(option)?.let {
            run {
                if (it.size != 3) return@run null
                val x = it[0].toDoubleOrNull() ?: return@run null
                val y = it[1].toDoubleOrNull() ?: return@run null
                val z = it[2].toDoubleOrNull() ?: return@run null
                Vector3d(x, y, z)
            } ?: throw InvalidOptionArgumentException(null, this, option, it)
        }

/**
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector2i]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector2i] or `null` if the option was not found
 */
fun CommandLine.getVector2i(option: CommandOption) =
        getList(option)?.let {
            run {
                if (it.size != 2) return@run null
                val x = it[0].toIntOrNull() ?: return@run null
                val y = it[1].toIntOrNull() ?: return@run null
                Vector2i(x, y)
            } ?: throw InvalidOptionArgumentException(null, this, option, it)
        }

/**
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector3i]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector3i] or `null` if the option was not found
 */
fun CommandLine.getVector3i(option: CommandOption) =
        getList(option)?.let {
            run {
                if (it.size != 3) return@run null
                val x = it[0].toIntOrNull() ?: return@run null
                val y = it[1].toIntOrNull() ?: return@run null
                val z = it[2].toIntOrNull() ?: return@run null
                Vector3i(x, y, z)
            } ?: throw InvalidOptionArgumentException(null, this, option, it)
        }

/**
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector2d]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector2d]
 * @throws MissingOptionException If the option was not found
 */
fun CommandLine.requireVector2d(option: CommandOption): Vector2d =
        requireVector2d(option) { it }

/**
 * Fetches the first argument for the option from the given [CommandLine],
 * converts it to a [Vector2d] and calls [block] with it before returning,
 * allowing computing default values
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @param block Called right after retrieving the value
 * @return A [Vector2d]
 * @throws MissingOptionException If the option was not found
 */
inline fun <R> CommandLine.requireVector2d(option: CommandOption,
                                           block: (Vector2d?) -> R?): R =
        block(getVector2d(option))
                ?: throw MissingOptionException(null, this, option)

/**
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector3d]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector3d]
 * @throws MissingOptionException If the option was not found
 */
fun CommandLine.requireVector3d(option: CommandOption): Vector3d =
        requireVector3d(option) { it }

/**
 * Fetches the first argument for the option from the given [CommandLine],
 * converts it to a [Vector3d] and calls [block] with it before returning,
 * allowing computing default values
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @param block Called right after retrieving the value
 * @return A [Vector3d]
 * @throws MissingOptionException If the option was not found
 */
inline fun <R> CommandLine.requireVector3d(option: CommandOption,
                                           block: (Vector3d?) -> R?): R =
        block(getVector3d(option))
                ?: throw MissingOptionException(null, this, option)

/**
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector2i]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector2i]
 * @throws MissingOptionException If the option was not found
 */
fun CommandLine.requireVector2i(option: CommandOption): Vector2i =
        requireVector2i(option) { it }

/**
 * Fetches the first argument for the option from the given [CommandLine],
 * converts it to a [Vector2i] and calls [block] with it before returning,
 * allowing computing default values
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @param block Called right after retrieving the value
 * @return A [Vector2i]
 * @throws MissingOptionException If the option was not found
 */
inline fun <R> CommandLine.requireVector2i(option: CommandOption,
                                           block: (Vector2i?) -> R?): R =
        block(getVector2i(option))
                ?: throw MissingOptionException(null, this, option)

/**
 * Fetches the first argument for the option from the given [CommandLine] and
 * converts it to a [Vector3i]
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @return A [Vector3i]
 * @throws MissingOptionException If the option was not found
 */
fun CommandLine.requireVector3i(option: CommandOption): Vector3i =
        requireVector3i(option) { it }

/**
 * Fetches the first argument for the option from the given [CommandLine],
 * converts it to a [Vector3i] and calls [block] with it before returning,
 * allowing computing default values
 * @receiver The [CommandLine] to read
 * @param option The [CommandOption] to look for
 * @param block Called right after retrieving the value
 * @return A [Vector3i]
 * @throws MissingOptionException If the option was not found
 */
inline fun <R> CommandLine.requireVector3i(option: CommandOption,
                                           block: (Vector3i?) -> R?): R =
        block(getVector3i(option))
                ?: throw MissingOptionException(null, this, option)
