package org.tobi29.scapes.server.ssl.spi;

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import javax.net.ssl.KeyManager;
import java.io.IOException;

public interface KeyManagerProvider {
    boolean available();

    String configID();

    KeyManager[] get(FilePath path, TagStructure config) throws IOException;
}
