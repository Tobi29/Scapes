package org.tobi29.scapes.tools.controlpanel.extensions

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Group
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.utils.io.tag.structure
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConsole

class ExtensionConsole(application: Application,
                       composite: ControlPanelConnection, connection: ControlPanelProtocol) : Extension(
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
                payload.getString("Message")?.let { console.console.append(it) }
            }
        }
        console.input.addListener(SWT.DefaultSelection) {
            consoleCommandLineReturn()
        }
    }

    fun consoleCommandLineReturn() {
        connection.send("Command", structure {
            setString("Command", console.input.text)
        })
        console.input.text = ""
    }

    companion object {
        fun available(commands: Set<String>): Boolean {
            return commands.contains("Command")
        }
    }
}
