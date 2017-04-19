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

package org.tobi29.scapes.tools.controlpanel

import org.eclipse.swt.layout.FillLayout
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.swt.util.Shortcut
import org.tobi29.scapes.engine.swt.util.framework.Document
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar
import org.tobi29.scapes.tools.controlpanel.extensions.*
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection

class ControlPanelDocument(address: RemoteAddress,
                           private val connection: ControlPanelProtocol,
                           commands: Array<String>,
                           private val requestClose: () -> Unit) : Document {
    private val extensions = ArrayList<Extension>()
    private val commands: Set<String>

    override val title = address.address + ':' + address.port
    override val shortTitle = address.address

    init {
        this.commands = commands.toSet()
    }

    override fun forceClose() {
        requestClose()
    }

    override fun destroy() {
        extensions.clear()
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar,
                          application: MultiDocumentApplication) {
        assert(extensions.isEmpty())
        val connection = menu.menu("Connection")
        connection.action("", { application.closeTab(composite) },
                Shortcut["Connection.Disconnect", 'D', Shortcut.Modifier.CONTROL])
        composite.layout = FillLayout()
        val panel = ControlPanelConnection(composite)
        if (ExtensionPing.available(commands)) {
            extensions.add(ExtensionPing(application, panel,
                    this.connection))
        }
        if (ExtensionPlayers.available(commands)) {
            extensions.add(ExtensionPlayers(application, panel,
                    this.connection))
        }
        if (ExtensionConsole.available(commands)) {
            extensions.add(ExtensionConsole(application, panel,
                    this.connection))
        }
        if (ExtensionStat.available(commands)) {
            extensions.add(ExtensionStat(application, panel,
                    this.connection))
        }
    }
}
