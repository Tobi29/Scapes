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
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Group
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.ComponentTypeRegistered
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.tag.toLong
import org.tobi29.scapes.engine.utils.task.Timer
import org.tobi29.scapes.engine.utils.task.loop
import org.tobi29.scapes.tools.controlpanel.ControlPanelDocument
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelGraphs
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ExtensionStat(
        application: Application,
        connection: ControlPanelProtocol
) : Extension(connection) {
    private var job: Pair<Job, AtomicBoolean>? = null
    private var graphs: ControlPanelGraphs? = null

    init {
        connection.addCommand("Stats") { payload ->
            launch(application) {
                graphs.ifPresent { graphs ->
                    val cpu = payload["CPU"]?.toDouble() ?: 0.0
                    graphs.cpu.addStamp(cpu)
                    val memory = (payload["Memory"]?.toLong() ?: 0L) / (1L shl 20)
                    graphs.memory.addStamp(memory.toDouble())
                }
            }
        }

    }

    override fun init(holder: ControlPanelDocument) {
        val stop = AtomicBoolean(false)
        job = launch(holder.application + CoroutineName("Extension-Players")) {
            Timer().apply { init() }.loop(Timer.toDiff(4.0),
                    { delay(it, TimeUnit.NANOSECONDS) }) {
                if (stop.get()) return@loop false

                connection.send("Stats", TagMap())

                true
            }
        } to stop
    }

    override fun populate(composite: ControlPanelConnection) {
        val group = Group(composite.server, SWT.NONE)
        group.layout = FillLayout()
        group.text = "Resources"
        val graphs = ControlPanelGraphs(group)

        this.graphs = graphs
    }

    override fun dispose() {
        job?.let { (_, stop) ->
            stop.set(true)
            this.job = null
        }
    }

    companion object {
        val COMPONENT = ComponentTypeRegistered<ControlPanelDocument, ExtensionStat, Any>()

        fun available(commands: Set<String>): Boolean {
            return commands.contains("Stats")
        }
    }
}
