package org.tobi29.scapes.client;

import java8.util.stream.Stream;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;

public interface SaveStorage {
    Stream<String> list() throws IOException;

    boolean exists(String name) throws IOException;

    WorldSource get(String name) throws IOException;

    boolean delete(String name) throws IOException;

    boolean loadClasses();
}
