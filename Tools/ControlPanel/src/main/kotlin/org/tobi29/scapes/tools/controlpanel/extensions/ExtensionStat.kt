package org.tobi29.scapes.tools.controlpanel.extensions

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Group
import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.swt.util.widgets.ifPresent
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.getLong
import org.tobi29.scapes.engine.utils.io.tag.structure
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelGraphs

class ExtensionStat(application: Application,
                    composite: ControlPanelConnection, connection: ControlPanelProtocol) : Extension(
        composite, connection) {
    init {
        val group = Group(composite.server, SWT.NONE)
        group.layout = FillLayout()
        group.text = "Resources"
        val graphs = ControlPanelGraphs(group)
        connection.addCommand("Stats") { payload ->
            application.accessAsync {
                if (graphs.isDisposed) {
                    return@accessAsync
                }
                val cpu = payload.getDouble("CPU") ?: 0.0
                graphs.cpu.addStamp(cpu)
                val memory = (payload.getLong("Memory") ?: 0) / (1 shl 20)
                graphs.memory.addStamp(memory.toDouble())
            }
        }
        application.taskExecutor.addTask({
            graphs.ifPresent {
                connection.send("Stats", structure {})
                return@addTask 250
            }
            -1
        }, "Extension-Ping")
    }

    companion object {
        fun available(commands: Set<String>): Boolean {
            return commands.contains("Stats")
        }
    }
}
