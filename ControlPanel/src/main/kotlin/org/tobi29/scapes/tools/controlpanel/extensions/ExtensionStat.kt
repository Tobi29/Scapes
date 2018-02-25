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
import kotlinx.coroutines.experimental.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Group
import org.tobi29.coroutines.Timer
import org.tobi29.coroutines.loopUntilCancel
import org.tobi29.server.ControlPanelProtocol
import org.tobi29.application.swt.framework.GuiApplication
import org.tobi29.application.swt.widgets.ifPresent
import org.tobi29.utils.ComponentTypeRegistered
import org.tobi29.scapes.tools.controlpanel.ControlPanelDocument
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelGraphs
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toDouble
import org.tobi29.io.tag.toLong

class ExtensionStat(
        application: GuiApplication,
        connection: ControlPanelProtocol
) : Extension(connection) {
    override val priority = -100
    private var job: Job? = null
    private var graphs: ControlPanelGraphs? = null

    init {
        connection.addCommand("Stats") { payload ->
            launch(application.uiContext) {
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
        job = launch(holder.application.uiContext +
                CoroutineName("Extension-Stat")) {
            Timer().apply { init() }.loopUntilCancel(Timer.toDiff(1.0)) {
                connection.send("Stats", TagMap())
            }
        }
    }

    override fun populate(composite: ControlPanelConnection) {
        val group = Group(composite.server, SWT.NONE)
        group.layout = FillLayout()
        group.text = "Resources"
        val graphs = ControlPanelGraphs(group)

        this.graphs = graphs
    }

    override fun dispose() {
        job?.cancel()
    }

    companion object {
        val COMPONENT = ComponentTypeRegistered<ControlPanelDocument, ExtensionStat, Any>()

        fun available(commands: Set<String>): Boolean {
            return commands.contains("Stats")
        }
    }
}
