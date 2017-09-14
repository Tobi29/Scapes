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

import kotlinx.coroutines.experimental.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Group
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.ComponentTypeRegistered
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toTag
import org.tobi29.scapes.tools.controlpanel.ControlPanelDocument
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConsole

class ExtensionConsole(
        application: Application,
        connection: ControlPanelProtocol
) : Extension(connection) {
    private var console: ControlPanelConsole? = null

    init {
        connection.addCommand("Message") { payload ->
            launch(application) {
                console.ifPresent { console ->
                    payload["Message"]?.toString()?.let {
                        console.console.append(it)
                    }
                }
            }
        }
    }

    override fun populate(composite: ControlPanelConnection) {
        val group = Group(composite.server, SWT.NONE)
        group.layout = FillLayout()
        group.text = "Console"
        val console = ControlPanelConsole(group)

        console.input.addListener(SWT.DefaultSelection) {
            connection.send("Command", TagMap {
                this["Command"] = console.input.text.toTag()
            })
            console.input.text = ""
        }

        this.console = console
    }

    companion object {
        val COMPONENT = ComponentTypeRegistered<ControlPanelDocument, ExtensionConsole, Any>()

        fun available(commands: Set<String>): Boolean {
            return commands.contains("Command")
        }
    }
}
