package org.tobi29.scapes.client.gui.desktop;

import java8.util.function.Consumer;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab;
import org.tobi29.scapes.engine.gui.GuiComponentSlab;
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
        row("Common Name", subject.getOrDefault("CN", "???"));
        row("Organization", subject.getOrDefault("O", "???"));
        row("Organizational Unit", subject.getOrDefault("OU", "???"));
        row("City", subject.getOrDefault("L", "???"));
        row("State", subject.getOrDefault("ST", "???"));
        row("Country", subject.getOrDefault("C", "???"));

        pane.addVert(16, 5, -1, 12, p -> new GuiComponentText(p, "Issuer"));
        row("Common Name", issuer.getOrDefault("CN", "???"));
        row("Organization", issuer.getOrDefault("O", "???"));
        row("Organizational Unit", issuer.getOrDefault("OU", "???"));
        row("City", issuer.getOrDefault("L", "???"));
        row("State", issuer.getOrDefault("ST", "???"));
        row("Country", issuer.getOrDefault("C", "???"));

        save.onClickLeft(event -> output.accept(true));
        back.onClickLeft(event -> output.accept(false));
    }

    private void row(String label, String text) {
        GuiComponentSlab row =
                pane.addVert(21, 0, -1, 20, GuiComponentGroupSlab::new);
        row.addHori(2, 2, -1, 12, p -> new GuiComponentText(p, label));
        row.addHori(2, 2, -1, -1, p -> button(p, 12, text));
    }
}
