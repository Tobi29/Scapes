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

package org.tobi29.scapes.server.format;

import java8.util.function.Function;
import org.tobi29.scapes.chunk.EnvironmentServer;
import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.server.ScapesServer;

import java.io.IOException;

public interface WorldFormat {
    IDStorage idStorage();

    PlayerData playerData();

    long seed();

    Plugins plugins();

    WorldServer registerWorld(ScapesServer server,
            Function<WorldServer, EnvironmentServer> environmentSupplier,
            String name, long seed) throws IOException;

    void removeWorld(WorldServer world);

    boolean deleteWorld(String name);

    void dispose() throws IOException;
}
