package org.tobi29.scapes.server.extension.event;

import java8.util.Objects;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.extension.ServerEvent;

public class PlayerAuthenticateEvent extends ServerEvent {
    private final PlayerConnection player;
    private boolean success = true;
    private String reason = "No reason given";

    public PlayerAuthenticateEvent(PlayerConnection player) {
        this.player = player;
    }

    public PlayerConnection player() {
        return player;
    }

    public boolean success() {
        return success;
    }

    public String reason() {
        return reason;
    }

    public void deny(String reason) {
        Objects.requireNonNull(reason);
        success = false;
        this.reason = reason;
    }

    public void allow() {
        success = true;
    }
}
