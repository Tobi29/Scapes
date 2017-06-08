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
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toTag
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConsole

class ExtensionConsole(application: Application,
                       composite: ControlPanelConnection,
                       connection: ControlPanelProtocol) : Extension(
        composite, connection) {
    val console: ControlPanelConsole

    init {
        val group = Group(composite.server, SWT.NONE)
        group.layout = FillLayout()
        group.text = "Console"
        console = ControlPanelConsole(group)
        connection.addCommand("Message") { payload ->
            application.accessAsync {
                if (console.isDisposed) {
                    return@accessAsync
                }
                payload["Message"]?.toString()?.let {
                    console.console.append(it)
                }
            }
        }
        console.input.addListener(SWT.DefaultSelection) {
            consoleCommandLineReturn()
        }
    }

    fun consoleCommandLineReturn() {
        connection.send("Command", TagMap {
            this["Command"] = console.input.text.toTag()
        })
        console.input.text = ""
    }

    companion object {
        fun available(commands: Set<String>): Boolean {
            return commands.contains("Command")
        }
    }
}
