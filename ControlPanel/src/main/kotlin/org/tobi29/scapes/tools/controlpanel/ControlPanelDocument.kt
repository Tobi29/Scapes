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
import org.tobi29.scapes.engine.utils.ComponentHolder
import org.tobi29.scapes.engine.utils.ComponentStorage
import org.tobi29.scapes.tools.controlpanel.extensions.*
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection

class ControlPanelDocument(
        val application: MultiDocumentApplication,
        address: RemoteAddress,
        connection: ControlPanelProtocol,
        commands: Set<String>,
        private val requestClose: () -> Unit
) : ComponentHolder<Any>, Document {
    override val componentStorage = ComponentStorage<Any>()
    private val extensions = ArrayList<Extension>()

    override val title = address.address + ':' + address.port
    override val shortTitle = address.address

    init {
        if (ExtensionPing.available(commands)) {
            extensions.add(registerComponent(ExtensionPing.COMPONENT,
                    ExtensionPing(connection)))
        }
        if (ExtensionPlayers.available(commands)) {
            extensions.add(registerComponent(ExtensionPlayers.COMPONENT,
                    ExtensionPlayers(application, connection)))
        }
        if (ExtensionConsole.available(commands)) {
            extensions.add(registerComponent(ExtensionConsole.COMPONENT,
                    ExtensionConsole(application, connection)))
        }
        if (ExtensionStat.available(commands)) {
            extensions.add(registerComponent(ExtensionStat.COMPONENT,
                    ExtensionStat(application, connection)))
        }
    }

    override fun forceClose() {
        requestClose()
    }

    override fun destroy() {
        clearComponents()
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar,
                          application: MultiDocumentApplication) {
        val connection = menu.menu("Connection")
        connection.action("", { application.closeTab(composite) },
                Shortcut["Connection.Disconnect", 'D', Shortcut.Modifier.CONTROL])
        composite.layout = FillLayout()
        val panel = ControlPanelConnection(composite)
        components.asSequence().filterIsInstance<ComponentUIControlPanel>()
                .sortedByDescending { it.priority }.forEach {
            it.populate(panel)
        }
    }
}
