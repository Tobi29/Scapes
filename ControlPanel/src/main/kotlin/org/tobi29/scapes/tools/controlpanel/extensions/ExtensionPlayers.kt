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
import org.tobi29.scapes.engine.utils.tag.Tag
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toList
import org.tobi29.scapes.engine.utils.tag.toMap
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelPlayers

class ExtensionPlayers(application: Application,
                       composite: ControlPanelConnection,
                       connection: ControlPanelProtocol) : Extension(
        composite, connection) {
    init {
        val group = Group(composite.server, SWT.NONE)
        group.layout = FillLayout()
        group.text = "Players"
        val players = ControlPanelPlayers(group)
        connection.addCommand("Players-List") { payload ->
            application.accessAsync {
                if (players.isDisposed) {
                    return@accessAsync
                }
                payload["Players"]?.toList()?.let {
                    players.items = it.asSequence().mapNotNull(
                            Tag::toMap).mapNotNull {
                        it["Name"]?.toString()
                    }.toArray()
                }
            }
        }
        application.loop.addTask({
            players.ifPresent {
                connection.send("Players:List", TagMap())
                return@addTask 1000
            }
            -1
        }, "Extension-Ping", 0, false)
    }

    companion object {
        fun available(commands: Set<String>): Boolean {
            return commands.contains("Players-List")
        }
    }
}
