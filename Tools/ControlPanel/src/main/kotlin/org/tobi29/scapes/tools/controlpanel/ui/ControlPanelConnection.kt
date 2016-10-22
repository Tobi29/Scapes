package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Composite

class ControlPanelConnection(parent: Composite) : Composite(parent, SWT.NONE) {
    val server: SashForm
    val status: Composite

    init {
        layout = GridLayout(1, false)
        server = SashForm(this, SWT.NONE)
        server.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        status = Composite(this, SWT.NONE)
        status.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        status.layout = RowLayout()
    }

    override fun checkSubclass() {
    }
}
