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

package org.tobi29.scapes.tools.controlpanel.extensions

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Text
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.ComponentTypeRegistered
import org.tobi29.scapes.engine.utils.task.Timer
import org.tobi29.scapes.engine.utils.task.loop
import org.tobi29.scapes.tools.controlpanel.ControlPanelDocument
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ExtensionPing(
        connection: ControlPanelProtocol
) : Extension(connection) {
    private var job: Pair<Job, AtomicBoolean>? = null
    private var label: Text? = null

    override fun init(holder: ControlPanelDocument) {
        val stop = AtomicBoolean(false)
        job = launch(holder.application + CoroutineName("Extension-Ping")) {
            Timer().apply { init() }.loop(Timer.toDiff(1.0),
                    { delay(it, TimeUnit.NANOSECONDS) }) {
                if (stop.get()) return@loop false

                label.ifPresent { it.text = connection.ping.toString() }

                true
            }
        } to stop

    }

    override fun populate(composite: ControlPanelConnection) {
        val label = Text(composite.status, SWT.NONE)

        this.label = label
    }

    override fun dispose() {
        job?.let { (_, stop) ->
            stop.set(true)
            this.job = null
        }
    }

    companion object {
        val COMPONENT = ComponentTypeRegistered<ControlPanelDocument, ExtensionPing, Any>()

        fun available(commands: Set<String>): Boolean {
            return commands.contains("Ping")
        }
    }
}
