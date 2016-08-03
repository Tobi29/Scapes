/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.client.gui.desktop;

import java8.util.Maps;
import java8.util.function.Consumer;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.*;
import org.tobi29.scapes.engine.utils.SSLUtil;

import java.security.cert.X509Certificate;
import java.util.Map;

public class GuiCertificateWarning extends GuiMenuDouble {
    public GuiCertificateWarning(GameState state, X509Certificate certificate,
            Consumer<Boolean> output, GuiStyle style) {
        super(state, "Invalid Certificate", "Ignore", "Disconnect", style);
        Map<String, String> subject =
                SSLUtil.parseX500(certificate.getSubjectX500Principal());
        Map<String, String> issuer =
                SSLUtil.parseX500(certificate.getIssuerX500Principal());

        pane.addVert(11, 5, -1, 18, p -> new GuiComponentText(p,
                "An invalid certificate was detected,"));
        pane.addVert(11, 5, -1, 18, p -> new GuiComponentText(p,
                "would you like to connect anyways?"));
        pane.addVert(16, 5, -1, 12, p -> new GuiComponentText(p, "Subject"));
        row("Common Name", Maps.getOrDefault(subject, "CN", "???"));
        row("Organization", Maps.getOrDefault(subject, "O", "???"));
        row("Organizational Unit", Maps.getOrDefault(subject, "OU", "???"));
        row("City", Maps.getOrDefault(subject, "L", "???"));
        row("State", Maps.getOrDefault(subject, "ST", "???"));
        row("Country", Maps.getOrDefault(subject, "C", "???"));

        pane.addVert(16, 5, -1, 12, p -> new GuiComponentText(p, "Issuer"));
        row("Common Name", Maps.getOrDefault(issuer, "CN", "???"));
        row("Organization", Maps.getOrDefault(issuer, "O", "???"));
        row("Organizational Unit", Maps.getOrDefault(issuer, "OU", "???"));
        row("City", Maps.getOrDefault(issuer, "L", "???"));
        row("State", Maps.getOrDefault(issuer, "ST", "???"));
        row("Country", Maps.getOrDefault(issuer, "C", "???"));

        save.on(GuiEvent.CLICK_LEFT, event -> output.accept(true));
        on(GuiAction.BACK, () -> output.accept(false));
    }

    private void row(String label, String text) {
        GuiComponentGroupSlab row =
                pane.addVert(21, 0, -1, 20, GuiComponentGroupSlab::new);
        row.addHori(2, 2, -1, 12, p -> new GuiComponentText(p, label));
        row.addHori(2, 2, -1, -1, p -> button(p, 12, text));
    }
}
