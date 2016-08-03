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

import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.server.format.WorldSource;
import org.tobi29.scapes.server.format.spi.WorldSourceProvider;

import java.io.IOException;

public class MariaDBWorldSourceProvider implements WorldSourceProvider {
    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String configID() {
        return "MariaDB";
    }

    @Override
    public WorldSource get(FilePath path, TagStructure config,
            TaskExecutor taskExecutor) throws IOException {
        String url = config.getString("URL");
        String user = config.getString("User");
        String password = config.getString("Password");
        return new MariaDBWorldSource(path, url, user, password);
    }
}
