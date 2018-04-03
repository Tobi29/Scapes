/*
 * Copyright 2012-2017 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.gui.*
import org.tobi29.server.parseX500
import java.security.cert.X509Certificate

class GuiCertificateWarning(state: GameState,
                            certificate: X509Certificate,
                            output: (Boolean) -> Unit,
                            style: GuiStyle) : GuiMenuDouble(
        state, "Invalid Certificate", "Ignore", "Disconnect", style) {
    init {
        val subject = parseX500(certificate.subjectX500Principal)
        val issuer = parseX500(certificate.issuerX500Principal)

        pane.addVert(11.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it,
                    "An invalid certificate was detected,")
        }
        pane.addVert(11.0, 5.0, -1.0, 18.0) {
            GuiComponentText(it,
                    "would you like to connect anyways?")
        }
        pane.addVert(16.0, 5.0, -1.0, 12.0) {
            GuiComponentText(it, "Subject")
        }
        row("Common Name", subject["CN"] ?: "???")
        row("Organization", subject["O"] ?: "???")
        row("Organizational Unit", subject["OU"] ?: "???")
        row("City", subject["L"] ?: "???")
        row("State", subject["ST"] ?: "???")
        row("Country", subject["C"] ?: "???")

        pane.addVert(16.0, 5.0, -1.0, 12.0) {
            GuiComponentText(it, "Issuer")
        }
        row("Common Name", issuer["CN"] ?: "???")
        row("Organization", issuer["O"] ?: "???")
        row("Organizational Unit", issuer["OU"] ?: "???")
        row("City", issuer["L"] ?: "???")
        row("State", issuer["ST"] ?: "???")
        row("Country", issuer["C"] ?: "???")

        save.on(GuiEvent.CLICK_LEFT) { event -> output(true) }
        on(GuiAction.BACK) { output(false) }
    }

    private fun row(label: String,
                    text: String) {
        val row = pane.addVert(21.0, 0.0, -1.0, 20.0, ::GuiComponentGroupSlab)
        row.addHori(2.0, 2.0, -1.0, 12.0) { GuiComponentText(it, label) }
        row.addHori(2.0, 2.0, -1.0, -1.0) { button(it, 12, text) }
    }
}
