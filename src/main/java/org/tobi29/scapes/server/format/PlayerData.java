package org.tobi29.scapes.server.format;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.util.Optional;

public interface PlayerData {
    Player player(String id);

    void save(String id, MobPlayerServer entity, int permissions,
            PlayerStatistics statistics);

    void add(String id);

    void remove(String id);

    boolean playerExists(String id);

    interface Player {
        MobPlayerServer createEntity(PlayerConnection player,
                Optional<WorldServer> overrideWorld);

        int permissions();

        PlayerStatistics statistics(GameRegistry registry);
    }
}
