package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.List

class ControlPanelPlayers(parent: Composite) : List(parent, SWT.BORDER) {

    override fun checkSubclass() {
    }
}
