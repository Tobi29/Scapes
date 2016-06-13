/*
 * Copyright 2012-2015 Tobi29
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
package org.tobi29.scapes.plugins;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.server.ScapesServer;

/**
 * Basic interface for generic plugins
 */
public interface Plugin {
    void initEarly(GameRegistry registry);

    void init(GameRegistry registry);

    void initEnd(GameRegistry registry);

    void initServer(ScapesServer server);

    void initServerEnd(ScapesServer server);

    void initClient(GameStateGameMP game);

    void initClientEnd(GameStateGameMP game);

    void worldInit(WorldServer world);

    void worldInit(WorldClient world);

    String id();

    String assetRoot();
}
