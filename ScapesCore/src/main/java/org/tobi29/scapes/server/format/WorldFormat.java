package org.tobi29.scapes.server.format;

import java8.util.function.Function;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;

public interface WorldFormat {
    IDStorage idStorage();

    PlayerData playerData();

    long seed();

    Plugins plugins();

    WorldServer registerWorld(ScapesServer server,
            Function<WorldServer, EnvironmentServer> environmentSupplier,
            String name, long seed) throws IOException;

    void removeWorld(WorldServer world);

    boolean deleteWorld(String name);

    void dispose() throws IOException;
}
