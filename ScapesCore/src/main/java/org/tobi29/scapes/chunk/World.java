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
package org.tobi29.scapes.chunk;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.plugins.Plugins;

public abstract class World {
    protected final BlockType air;
    protected final Plugins plugins;
    protected final TaskExecutor taskExecutor;
    protected final GameRegistry registry;
    protected final long seed;
    protected Thread thread;
    protected Vector3 spawn = new Vector3i(0, 0, 0);
    protected long tick;
    @SuppressWarnings("CanBeFinal")
    protected double gravity = 9.8;

    protected World(Plugins plugins, TaskExecutor taskExecutor,
            GameRegistry registry, long seed) {
        this.plugins = plugins;
        this.taskExecutor = taskExecutor;
        this.registry = registry;
        this.seed = seed;
        air = registry.air();
    }

    public Plugins plugins() {
        return plugins;
    }

    public BlockType air() {
        return air;
    }

    public long seed() {
        return seed;
    }

    public Vector3 spawn() {
        return spawn;
    }

    public TaskExecutor taskExecutor() {
        return taskExecutor;
    }

    public GameRegistry registry() {
        return registry;
    }

    public long tick() {
        return tick;
    }

    public double gravity() {
        return gravity;
    }

    public boolean checkThread() {
        return Thread.currentThread() == thread;
    }
}
