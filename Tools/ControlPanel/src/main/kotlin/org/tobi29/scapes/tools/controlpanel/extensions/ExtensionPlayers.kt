package org.tobi29.scapes.tools.controlpanel.extensions

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Group
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.io.tag.structure
import org.tobi29.scapes.engine.utils.notNull
import org.tobi29.scapes.engine.utils.stream
import org.tobi29.scapes.engine.utils.toTypedArray
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelPlayers

class ExtensionPlayers(application: Application,
                       composite: ControlPanelConnection, connection: ControlPanelProtocol) : Extension(
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
                payload.getList("Players")?.let {
                    players.items = it.stream().map {
                        it.getString("Name")
                    }.notNull().toTypedArray()
                }
            }
        }
        application.taskExecutor.addTask({
            players.ifPresent {
                connection.send("Players:List", structure {})
                return@addTask 1000
            }
            -1
        }, "Extension-Ping")
    }

    companion object {
        fun available(commands: Set<String>): Boolean {
            return commands.contains("Players-List")
        }
    }
}
