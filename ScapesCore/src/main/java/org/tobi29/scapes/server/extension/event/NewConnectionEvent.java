package org.tobi29.scapes.server.extension.event;

import java8.util.Objects;
import org.tobi29.scapes.server.extension.ServerEvent;

import java.nio.channels.SocketChannel;

public class NewConnectionEvent extends ServerEvent {
    private final SocketChannel channel;
    private boolean success = true;
    private String reason = "No reason given";

    public NewConnectionEvent(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel channel() {
        return channel;
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
