package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.ProgressBar

class ControlPanelConnecting(parent: Composite) : Composite(parent, SWT.NONE) {
    val progress: ProgressBar

    init {
        layout = GridLayout(1, false)
        progress = ProgressBar(this, SWT.INDETERMINATE)
        progress.layoutData = GridData(SWT.FILL, SWT.CENTER, true, true)
    }

    override fun checkSubclass() {
    }
}
