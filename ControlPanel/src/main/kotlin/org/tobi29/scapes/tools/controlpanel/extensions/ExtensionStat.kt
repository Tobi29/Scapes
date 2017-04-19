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

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Group
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.tag.toLong
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelGraphs

class ExtensionStat(application: Application,
                    composite: ControlPanelConnection,
                    connection: ControlPanelProtocol) : Extension(
        composite, connection) {
    init {
        val group = Group(composite.server, SWT.NONE)
        group.layout = FillLayout()
        group.text = "Resources"
        val graphs = ControlPanelGraphs(group)
        connection.addCommand("Stats") { payload ->
            application.accessAsync {
                if (graphs.isDisposed) {
                    return@accessAsync
                }
                val cpu = payload["CPU"]?.toDouble() ?: 0.0
                graphs.cpu.addStamp(cpu)
                val memory = (payload["Memory"]?.toLong() ?: 0L) / (1L shl 20)
                graphs.memory.addStamp(memory.toDouble())
            }
        }
        application.loop.addTask({
            graphs.ifPresent {
                connection.send("Stats", TagMap())
                return@addTask 250
            }
            -1
        }, "Extension-Ping")
    }

    companion object {
        fun available(commands: Set<String>): Boolean {
            return commands.contains("Stats")
        }
    }
}
