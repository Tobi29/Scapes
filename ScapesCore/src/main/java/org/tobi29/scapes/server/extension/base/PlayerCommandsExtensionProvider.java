package org.tobi29.scapes.server.extension.base;

import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.extension.ServerExtension;
import org.tobi29.scapes.server.extension.spi.ServerExtensionProvider;

public class PlayerCommandsExtensionProvider
        implements ServerExtensionProvider {
    @Override
    public boolean available() {
        return true;
    }

    @Override
    public ServerExtension create(ScapesServer server) {
        return new PlayerCommandsExtension(server);
    }
}
