package org.tobi29.scapes.server.extension;

@FunctionalInterface
public interface ServerListener<E extends ServerEvent> {
    void event(E event);
}
