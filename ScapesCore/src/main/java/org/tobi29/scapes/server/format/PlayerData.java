package org.tobi29.scapes.server.format;

import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.server.PlayerEntry;

public interface PlayerData {
    PlayerEntry player(String id);

    void save(String id, MobPlayerServer entity, int permissions);

    void add(String id);

    void remove(String id);

    boolean playerExists(String id);
}
