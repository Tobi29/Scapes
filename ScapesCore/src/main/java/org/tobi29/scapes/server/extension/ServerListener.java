package org.tobi29.scapes.server.extension;

public interface ServerListener<E extends ServerEvent> {
    void event(E event);
}
