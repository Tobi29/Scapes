package org.tobi29.scapes.server.format.sqlite;

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.spi.WorldSourceProvider;

import java.io.IOException;

public class SQLiteWorldSourceProvider implements WorldSourceProvider {
    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String configID() {
        return "SQLite";
    }

    @Override
    public WorldSource get(FilePath path, TagStructure config,
            TaskExecutor taskExecutor) throws IOException {
        return new SQLiteWorldSource(path, taskExecutor);
    }
}
