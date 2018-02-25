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

package org.tobi29.scapes.client.gui

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.loopUntilCancel
import org.tobi29.scapes.client.connection.ConnectionProfiler
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues

class GuiWidgetConnectionProfiler(
        parent: GuiLayoutData,
        private val profiler: ConnectionProfiler
) : GuiWidgetDebugValues(parent) {
    private var updateJob: Job? = null

    override fun init() = updateVisible()

    override fun updateVisible() {
        synchronized(this) {
            dispose()
            if (!isVisible) return@synchronized
            updateJob = launch(engine.taskExecutor) {
                Timer().apply { init() }.loopUntilCancel(Timer.toDiff(4.0)) {
                    profiler.bytes.entries.forEach { entry ->
                        get(entry.key.simpleName).setValue(entry.value)
                    }
                }
            }
        }
    }

    override fun dispose() {
        synchronized(this) {
            updateJob?.cancel()
        }
    }
}
