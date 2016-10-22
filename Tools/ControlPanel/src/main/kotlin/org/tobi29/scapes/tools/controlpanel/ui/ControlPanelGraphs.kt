package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite

class ControlPanelGraphs(parent: Composite) : Composite(parent, SWT.NONE) {
    val cpu: ControlPanelGraph
    val memory: ControlPanelGraph

    init {
        layout = FillLayout(SWT.VERTICAL)
        cpu = ControlPanelGraph(this, 1.0, 1.0) { "CPU: ${(it * 100.0).toInt()}%" }
        memory = ControlPanelGraph(this, 4096.0, 1.0) { "Memory: ${it.toInt()}MB" }
    }

    override fun checkSubclass() {
    }
}
