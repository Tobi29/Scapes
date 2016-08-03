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

package org.tobi29.scapes.server.format.mariadb;

import java8.util.Optional;
import org.mariadb.jdbc.MariaDbDataSource;
import org.tobi29.scapes.engine.sql.mysql.MySQLDatabase;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.format.WorldFormat;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.sql.SQLWorldFormat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class MariaDBWorldSource implements WorldSource {
    private final FilePath path;
    private final Connection connection;
    private final MySQLDatabase database;

    public MariaDBWorldSource(FilePath path, String url, String user,
            String password) throws IOException {
        this(path, openDatabase(url, user, password));
    }

    public MariaDBWorldSource(FilePath path, Connection connection) {
        this.path = path;
        this.connection = connection;
        database = new MySQLDatabase(connection);
    }

    public static Connection openDatabase(String url, String user,
            String password) throws IOException {
        try {
            MariaDbDataSource source = new MariaDbDataSource(url);
            return source.getConnection(user, password);
        } catch (SQLException e) {
            throw new IOException(e);
        }
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
    }

    @Override
    public Optional<Image[]> panorama() throws IOException {
        return Optional.empty();
    }

    @Override
    public WorldFormat open(ScapesServer server) throws IOException {
        return new SQLWorldFormat(path, database);
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
