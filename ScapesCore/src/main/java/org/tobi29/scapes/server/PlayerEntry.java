package org.tobi29.scapes.server;

import java8.util.Optional;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.connection.PlayerConnection;

public interface PlayerEntry {
    Optional<MobPlayerServer> createEntity(PlayerConnection player,
            Optional<WorldServer> world, Optional<Vector3> pos);

    int permissions();
}