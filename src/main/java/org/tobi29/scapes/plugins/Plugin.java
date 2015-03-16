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
import org.tobi29.scapes.server.ScapesServer;

/**
 * Basic interface for generic plugins
 */
public interface Plugin {
    /**
     * Called when the game is starting (Do your registering and loading here)
     *
     * @param registry Used to register blocks, entities etc.
     */
    void init(GameRegistry registry);

    /**
     * Called after all plugins initialized
     *
     * @param registry Used to register blocks, entities etc.
     */
    void initEnd(GameRegistry registry);

    /**
     * Called after all common initialization methods got called
     *
     * @param server Server that this plugin is running on
     */
    void initServer(ScapesServer server);

    /**
     * Called when a world initializes
     *
     * @param world The {@code World} that is initializing
     */
    void worldInit(WorldServer world);

    void worldInit(WorldClient world);

    /**
     * Called when a world ticks
     *
     * @param world The {@code World} that is ticking
     */
    void worldTick(WorldServer world);

    /**
     * Called when the game is shutting down (Textures, sounds and VAOs are
     * disposed automatically! (Textures that are not loaded through the {@code
     * TextureManager} are not disposed automatically! So you have to dispose
     * these yourself!))
     *
     * @param registry Used to register blocks, entities etc.
     */
    void dispose(GameRegistry registry);

    /**
     * Name of the plugin
     *
     * @return Name of the plugin, excluding version
     */
    String getName();

    String getID();

    String getAssetRoot();

    /**
     * Version of the plugin
     *
     * @return Version of the plugin (Format: x.x.x)
     */
    String getVersion();
}
