package org.tobi29.scapes.client;

import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.basic.BasicWorldSource;

import java.io.IOException;

public class BasicSaveStorage implements SaveStorage {
    private final FilePath path;

    public BasicSaveStorage(FilePath path) {
        this.path = path;
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
        return new BasicWorldSource(path.resolve(name));
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
