package org.tobi29.scapes.tools.controlpanel.extensions

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Text
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.io.tag.getLong
import org.tobi29.scapes.engine.utils.io.tag.setLong
import org.tobi29.scapes.engine.utils.io.tag.structure
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection

class ExtensionPing(application: Application,
                    composite: ControlPanelConnection, connection: ControlPanelProtocol) : Extension(
        composite, connection) {
    init {
        val label = Text(composite.status, SWT.NONE)
        connection.addCommand("Pong") { payload ->
            application.accessAsync {
                if (label.isDisposed) {
                    return@accessAsync
                }
                payload.getLong("Timestamp")?.let { timestamp ->
                    try {
                        val ping = System.currentTimeMillis() - timestamp
                        label.text = ping.toString()
                    } catch (e: NumberFormatException) {
                    }
                }
            }
        }
        application.taskExecutor.addTask({
            label.ifPresent {
                connection.send("Ping", structure {
                    setLong("Timestamp", System.currentTimeMillis())
                })
                return@addTask 1000
            }
            -1
        }, "Extension-Ping")
    }

    companion object {
        fun available(commands: Set<String>): Boolean {
            return commands.contains("Ping")
        }
    }
}
