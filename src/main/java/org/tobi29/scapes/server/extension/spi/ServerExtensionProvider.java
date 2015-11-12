package org.tobi29.scapes.server.extension.spi;

import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.extension.ServerExtension;

public interface ServerExtensionProvider {
    boolean available();

    ServerExtension create(ScapesServer server);
}
