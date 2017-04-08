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

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.ProgressBar
import org.tobi29.scapes.engine.server.ConnectionManager
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.server.SSLHandle
import org.tobi29.scapes.engine.swt.util.framework.Document
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent

class ReconnectDocument(private val address: RemoteAddress,
                        private val password: String,
                        private val ssl: SSLHandle,
                        private val connection: ConnectionManager) : Document {
    private var timer = 5

    override val title = address.address + ':' + address.port
    override val shortTitle = address.address

    override fun forceClose() {
    }

    override fun destroy() {
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar,
                          application: MultiDocumentApplication) {
        val connection = menu.menu("Connection")
        val connectionClose = MenuItem(connection, SWT.PUSH)
        connectionClose.text = "Close"
        connectionClose.addListener(SWT.Selection
        ) { event -> application.closeTab(composite) }
        composite.layout = GridLayout(1, false)
        val pane = Composite(composite, SWT.NONE)
        pane.layout = GridLayout(1, false)
        pane.layoutData = GridData(SWT.FILL, SWT.CENTER, true, true)
        val text = Label(pane, SWT.NONE)
        text.layoutData = GridData(SWT.CENTER, SWT.CENTER, true, false)
        text.text = "Reconnecting in $timer..."
        val progress = ProgressBar(pane, SWT.INDETERMINATE)
        progress.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
        application.taskExecutor.addTask({
            if (timer <= 0) {
                application.access(this) { composite ->
                    application.replaceTab(composite,
                            ConnectDocument(address, password, ssl,
                                    this.connection, application))
                }
                return@addTask -1
            } else {
                timer--
            }
            text.ifPresent {
                it.text = "Reconnecting in $timer..."
                return@addTask 1000
            }
            -1
        }, "Reconnect-Timer", 1000)
    }
}
