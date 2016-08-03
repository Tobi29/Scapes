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

package org.tobi29.scapes.server.format.sqlite;

import java8.util.Optional;
import org.tobi29.scapes.engine.sql.sqlite.SQLiteConfig;
import org.tobi29.scapes.engine.sql.sqlite.SQLiteDatabase;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.WorldFormat;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.sql.SQLWorldFormat;

import java.io.IOException;
import java.util.List;

public class SQLiteWorldSource implements WorldSource {
    private final FilePath path;
    private final SQLiteDatabase database;

    public SQLiteWorldSource(FilePath path, TaskExecutor taskExecutor)
            throws IOException {
        this.path = path;
        FileUtil.createDirectories(path);
        SQLiteConfig config = new SQLiteConfig();
        config.secureDelete = false;
        config.journalMode = SQLiteConfig.JournalMode.WAL;
        config.synchronous = SQLiteConfig.Synchronous.NORMAL;
        database = new SQLiteDatabase(path.resolve("Data.db"), taskExecutor,
                config);
    }

    @Override
    public void init(long seed, List<FilePath> plugins) throws IOException {
        SQLWorldFormat.initDatabase(database, seed);
        FilePath pluginsDir = path.resolve("plugins");
        FileUtil.createDirectories(pluginsDir);
        for (FilePath plugin : plugins) {
            FileUtil.copy(plugin, pluginsDir.resolve(plugin.getFileName()));
        }
    }

    @Override
    public void panorama(Image[] images) throws IOException {
        if (images.length != 6) {
            throw new IllegalArgumentException("6 panorama images required");
        }
        for (int i = 0; i < 6; i++) {
            int j = i;
            FileUtil.write(path.resolve("Panorama" + i + ".png"),
                    streamOut -> PNG.encode(images[j], streamOut, 9, false));
        }
    }

    @Override
    public Optional<Image[]> panorama() throws IOException {
        Image[] array = new Image[6];
        for (int i = 0; i < 6; i++) {
            FilePath background = path.resolve("Panorama" + i + ".png");
            if (FileUtil.exists(background)) {
                array[i] = FileUtil.readReturn(background,
                        stream -> PNG.decode(stream, BufferCreator::bytes));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(array);
    }

    @Override
    public WorldFormat open(ScapesServer server) throws IOException {
        return new SQLWorldFormat(path, database);
    }

    @Override
    public void close() throws IOException {
        database.close();
    }
}
