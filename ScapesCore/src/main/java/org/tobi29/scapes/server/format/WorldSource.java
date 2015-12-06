package org.tobi29.scapes.server.format;

import java8.util.Optional;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;
import java.util.List;

public interface WorldSource extends AutoCloseable {
    void init(long seed, List<FilePath> plugins) throws IOException;

    void panorama(Image[] images) throws IOException;

    Optional<Image[]> panorama() throws IOException;

    WorldFormat open(ScapesServer server) throws IOException;

    @Override
    void close() throws IOException;
}
