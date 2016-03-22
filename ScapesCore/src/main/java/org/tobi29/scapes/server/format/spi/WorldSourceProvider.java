package org.tobi29.scapes.server.format.spi;

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;

public interface WorldSourceProvider {
    boolean available();

    String configID();

    WorldSource get(FilePath path, TagStructure config,
            TaskExecutor taskExecutor) throws IOException;
}
