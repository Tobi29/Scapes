package org.tobi29.scapes.server.format.basic;

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.spi.WorldSourceProvider;

import java.io.IOException;

public class BasicWorldSourceProvider implements WorldSourceProvider {
    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String configID() {
        return "Basic";
    }

    @Override
    public WorldSource get(FilePath path, TagStructure config)
            throws IOException {
        return new BasicWorldSource(path);
    }
}
