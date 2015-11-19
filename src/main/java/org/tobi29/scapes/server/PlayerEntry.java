package org.tobi29.scapes.server;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.format.PlayerStatistics;

import java.util.Optional;

public interface PlayerEntry {
    Optional<MobPlayerServer> createEntity(PlayerConnection player,
            Optional<WorldServer> spawnWorld);

    int permissions();

    PlayerStatistics statistics(GameRegistry registry);
}