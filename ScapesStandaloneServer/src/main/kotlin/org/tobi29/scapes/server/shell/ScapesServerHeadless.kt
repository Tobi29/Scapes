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

package org.tobi29.scapes.server.shell

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.tobi29.io.IOException
import org.tobi29.io.filesystem.FilePath
import org.tobi29.logging.KLogging
import org.tobi29.scapes.server.MessageLevel
import org.tobi29.scapes.server.ScapesServer
import org.tobi29.scapes.server.command.Executor
import org.tobi29.scapes.server.extension.event.MessageEvent
import org.tobi29.utils.ListenerRegistrar
import java.io.BufferedReader
import java.io.InputStreamReader

class ScapesServerHeadless(
        taskExecutor: CoroutineDispatcher,
        path: FilePath
) : ScapesStandaloneServer(taskExecutor, path) {
    override fun ListenerRegistrar.listeners() {
        listen<MessageEvent> { event ->
            when (event.level) {
                MessageLevel.SERVER_ERROR,
                MessageLevel.FEEDBACK_ERROR -> logger.error { event.message }
                else -> logger.info { event.message }
            }
        }
    }

    override fun init(executor: Executor): () -> Unit {
        val reader = BufferedReader(InputStreamReader(System.`in`))
        return {
            try {
                while (reader.ready()) {
                    val line = reader.readLine()
                    if (line != null) {
                        server.commandRegistry[line, executor].execute().forEach { output ->
                            executor.events.fire(
                                    MessageEvent(executor,
                                            MessageLevel.FEEDBACK_ERROR,
                                            output.toString(), executor))
                        }
                    }
                }
            } catch (e: IOException) {
                logger.error { "Error reading console input: $e" }
                server.scheduleStop(ScapesServer.ShutdownReason.ERROR)
            }
        }
    }

    companion object : KLogging()
}
