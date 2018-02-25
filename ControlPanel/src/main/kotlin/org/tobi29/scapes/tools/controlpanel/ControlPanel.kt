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
import org.eclipse.swt.widgets.Spinner
import org.eclipse.swt.widgets.Text
import org.tobi29.application.executeMain
import org.tobi29.args.CommandLine
import org.tobi29.server.ConnectionManager
import org.tobi29.server.RemoteAddress
import org.tobi29.application.swt.Shortcut
import org.tobi29.application.swt.framework.DocumentComposite
import org.tobi29.application.swt.framework.MultiDocumentApplication
import org.tobi29.application.swt.widgets.InputDialog
import org.tobi29.application.swt.widgets.SmartMenuBar
import org.tobi29.utils.Version
import org.tobi29.io.IOException

object ControlPanel : MultiDocumentApplication() {
    override val id = "org.tobi29.scapes.controlpanel"

    override val execName = "control-panel"
    override val fullName = "Scapes Control Panel"
    override val name = "ControlPanel"

    override val version = Version(0, 0, 0)

    val connection = ConnectionManager(taskExecutor)

    override fun init(commandLine: CommandLine) {
        connection.workers(1)
        openTab(WelcomeDocument())
    }

    override fun dispose() {
        connection.dispose()
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar) {
        val connection = menu.menu("Connection")
        connection.action("Connect...", {
            try {
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
                            this.connection, this))
                }
            } catch (e: IOException) {
                message(SWT.ERROR, "Failed to initialize SSL",
                        "Failed to initialize SSL:\n" + e.toString())
            }
        }, Shortcut["Connection.Connect", 'C', Shortcut.Modifier.CONTROL])
    }

    @JvmStatic
    fun main(args: Array<String>) = executeMain(args)
}
