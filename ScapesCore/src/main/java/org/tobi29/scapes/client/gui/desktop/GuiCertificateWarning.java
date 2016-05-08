package org.tobi29.scapes.client.gui.desktop;

import java8.util.Maps;
import java8.util.function.Consumer;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiStyle;
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

        save.onClickLeft(event -> output.accept(true));
        back.onClickLeft(event -> output.accept(false));
    }

    private void row(String label, String text) {
        GuiComponentGroupSlab row =
                pane.addVert(21, 0, -1, 20, GuiComponentGroupSlab::new);
        row.addHori(2, 2, -1, 12, p -> new GuiComponentText(p, label));
        row.addHori(2, 2, -1, -1, p -> button(p, 12, text));
    }
}
