package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.tobi29.scapes.engine.utils.parseX500
import java.security.cert.X509Certificate

class Certificate(parent: Composite, style: Int,
                  certificate: X509Certificate) : Composite(parent, style) {
    init {
        val subject = parseX500(certificate.subjectX500Principal)
        val issuer = parseX500(certificate.issuerX500Principal)
        layout = GridLayout(2, false)
        var label: Label

        label = Label(this, SWT.NONE)
        label.text = "Subject"
        label.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1)
        row("Common Name", subject["CN"] ?: "???")
        row("Organization", subject["O"] ?: "???")
        row("Organizational Unit", subject["OU"] ?: "???")
        row("City", subject["L"] ?: "???")
        row("State", subject["ST"] ?: "???")
        row("Country", subject["C"] ?: "???")

        label = Label(this, SWT.NONE)
        label.text = "Issuer"
        label.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1)
        row("Common Name", issuer["CN"] ?: "???")
        row("Organization", issuer["O"] ?: "???")
        row("Organizational Unit", issuer["OU"] ?: "???")
        row("City", issuer["L"] ?: "???")
        row("State", issuer["ST"] ?: "???")
        row("Country", issuer["C"] ?: "???")
    }

    private fun row(key: String,
                    value: String): Label {
        val label = row(key, { Label(it, SWT.BORDER) })
        label.text = value
        return label
    }

    private fun <C : Control> row(label: String,
                                  supplier: (Composite) -> C): C {
        val fieldLabel = Label(this, SWT.NONE)
        fieldLabel.text = label
        fieldLabel.layoutData = GridData(SWT.RIGHT, SWT.CENTER, false, false, 1,
                1)
        val field = supplier(this)
        field.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1)
        return field
    }

    override fun checkSubclass() {
    }
}
