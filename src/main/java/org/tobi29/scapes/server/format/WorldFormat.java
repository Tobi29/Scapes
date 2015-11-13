package org.tobi29.scapes.server.format;

import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.plugins.Dimension;
import org.tobi29.scapes.plugins.Plugins;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public interface WorldFormat {
    IDStorage idStorage();

    PlayerData playerData();

    long seed();

    Plugins plugins();

    Optional<WorldServer> world(String name);

    WorldServer defaultWorld();

    Collection<String> worldNames();

    WorldServer registerWorld(Dimension dimension) throws IOException;

    WorldServer registerWorld(
            Function<WorldServer, EnvironmentServer> environmentSupplier,
            String name, long seed) throws IOException;

    boolean removeWorld(String name);

    void deleteWorld(String name) throws IOException;

    void save() throws IOException;

    void savePanorama(Image[] images) throws IOException;
}
