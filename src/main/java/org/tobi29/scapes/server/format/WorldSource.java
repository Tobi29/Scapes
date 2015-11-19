package org.tobi29.scapes.server.format;

import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface WorldSource extends AutoCloseable {
    void init(long seed, List<Path> plugins) throws IOException;

    void panorama(Image[] images) throws IOException;

    Optional<Image[]> panorama() throws IOException;

    WorldFormat open(ScapesServer server) throws IOException;

    @Override
    void close() throws IOException;
}
