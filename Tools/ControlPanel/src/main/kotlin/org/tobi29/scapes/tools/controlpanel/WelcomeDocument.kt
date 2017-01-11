package org.tobi29.scapes.tools.controlpanel

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Label
import org.tobi29.scapes.engine.swt.util.framework.Document
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar

class WelcomeDocument : Document {
    override val title = ""
    override val isEmpty = true

    override fun forceClose() {
    }

    override fun destroy() {
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar,
                          application: MultiDocumentApplication) {
        composite.layout = GridLayout(1, false)
        val text = Label(composite, SWT.NONE)
        text.layoutData = GridData(SWT.CENTER, SWT.CENTER, true, true)
        text.text = "Click on Connection > Connect.. to connect"
    }
}
