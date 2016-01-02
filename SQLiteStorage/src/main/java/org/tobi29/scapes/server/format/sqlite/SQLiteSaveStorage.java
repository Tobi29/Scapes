package org.tobi29.scapes.server.format.sqlite;

import java8.util.stream.Stream;
import org.tobi29.scapes.client.SaveStorage;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;

public class SQLiteSaveStorage implements SaveStorage {
    private final FilePath path;
    private final TaskExecutor taskExecutor;

    public SQLiteSaveStorage(FilePath path, TaskExecutor taskExecutor) {
        this.path = path;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public Stream<String> list() throws IOException {
        if (!FileUtil.exists(path)) {
            return Streams.of();
        }
        return Streams.of(FileUtil
                .list(path, FileUtil::isDirectory, FileUtil::isNotHidden))
                .map(FilePath::getFileName).map(FilePath::toString);
    }

    @Override
    public boolean exists(String name) throws IOException {
        return FileUtil.exists(path.resolve(name));
    }

    @Override
    public WorldSource get(String name) throws IOException {
        return new SQLiteWorldSource(path.resolve(name), taskExecutor);
    }

    @Override
    public boolean delete(String name) throws IOException {
        FileUtil.deleteDir(path.resolve(name));
        return true;
    }

    @Override
    public boolean loadClasses() {
        return true;
    }
}
