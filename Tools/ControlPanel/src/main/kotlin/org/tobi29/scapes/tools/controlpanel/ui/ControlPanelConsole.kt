package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import org.tobi29.scapes.engine.swt.util.Fonts
import org.tobi29.scapes.engine.swt.util.HTMLLineStyler

class ControlPanelConsole(parent: Composite) : Composite(parent, SWT.NONE) {
    val console: StyledText
    val input: Text

    init {
        layout = GridLayout(1, false)
        console = StyledText(this,
                SWT.BORDER or SWT.READ_ONLY or SWT.WRAP or SWT.V_SCROLL or
                        SWT.MULTI)
        val font = Font(display, Fonts.monospace)
        addDisposeListener { e -> font.dispose() }
        console.font = font
        console.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)
        HTMLLineStyler(console)
        input = Text(this, SWT.BORDER)
        input.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 1, 1)
    }

    override fun checkSubclass() {
    }
}
