/*
 * Copyright 2012-2016 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                .map(FilePath::getFileName).map(String::valueOf);
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
