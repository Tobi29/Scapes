package org.tobi29.scapes.server.extension;

import org.tobi29.scapes.server.ScapesServer;

public abstract class ServerExtension {
    protected final ScapesServer server;

    protected ServerExtension(ScapesServer server) {
        this.server = server;
    }

    public void initEarly() {
    }

    public abstract void init(ServerExtensions.Registrar registrar);

    public void initLate(ServerExtensions.Registrar registrar) {
    }
}
