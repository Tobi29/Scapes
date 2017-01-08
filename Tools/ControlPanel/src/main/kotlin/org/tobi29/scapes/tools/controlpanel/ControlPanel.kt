/*
 * Copyright 2012-2015 Tobi29
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
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Spinner
import org.eclipse.swt.widgets.Text
import org.tobi29.scapes.engine.server.ConnectionManager
import org.tobi29.scapes.engine.server.RemoteAddress
import org.tobi29.scapes.engine.server.SSLProvider
import org.tobi29.scapes.engine.swt.util.Shortcut
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.InputDialog
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar
import org.tobi29.scapes.engine.utils.version
import org.tobi29.scapes.tools.controlpanel.ui.Certificate
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

fun main(args: Array<String>) {
    ControlPanel().run()
}

class ControlPanel : MultiDocumentApplication("Scapes Control Panel",
        "ControlPanel", version("0.0.0")) {
    val connection = ConnectionManager(taskExecutor)

    override fun init() {
        connection.workers(1)
        openTab(WelcomeDocument())
    }

    override fun dispose() {
        connection.stop()
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar) {
        val connection = menu.menu("Connection")
        connection.action("Connect...", {
            try {
                val ssl = SSLProvider.sslHandle { certificates ->
                    val output = AtomicBoolean(true)
                    access {
                        val shell = activeShell
                        if (shell == null) {
                            output.set(false)
                            return@access
                        }
                        for (certificate in certificates) {
                            val dialog = InputDialog(shell,
                                    "Untrusted certificate", "Disconnect")
                            dialog.add("Certificate details", {
                                Certificate(it, SWT.BORDER, certificate)
                            })
                            val ignore = dialog.add("",
                                    { Button(it, SWT.NONE) })
                            ignore.text = "Ignore"
                            ignore.layoutData = GridData(SWT.CENTER, SWT.CENTER,
                                    true,
                                    false, 1, 1)
                            var ignored = false
                            ignore.addListener(SWT.Selection) { event ->
                                ignored = true
                                dialog.dismiss()
                            }
                            dialog.open()
                            if (!ignored) {
                                output.set(false)
                                return@access
                            }
                        }
                    }
                    output.get()
                }
                val dialog = InputDialog(composite.shell, "Connect...",
                        "Connect")
                val addressField = dialog.add("Address",
                        { Text(it, SWT.BORDER) })
                val portField = dialog.add("Address", { Spinner(it, SWT.NONE) })
                portField.setValues(12345, 0, 65535, 0, 1, 100)
                val passwordField = dialog.add("Password",
                        { Text(it, SWT.BORDER or SWT.PASSWORD) })
                dialog.open {
                    openNewTab(composite, ConnectDocument(
                            RemoteAddress(addressField.text,
                                    portField.selection), passwordField.text,
                            ssl, this.connection, this))
                }
            } catch (e: IOException) {
                message(SWT.ERROR, "Failed to initialize SSL",
                        "Failed to initialize SSL:\n" + e.toString())
            }
        }, Shortcut["Connection.Connect", 'C', Shortcut.Modifier.CONTROL])
    }
}
