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
package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleSmoke;
import org.tobi29.scapes.vanilla.basics.gui.GuiForgeInventory;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class EntityForgeClient extends EntityAbstractFurnaceClient {
    public EntityForgeClient(WorldClient world) {
        this(world, Vector3d.ZERO);
    }

    public EntityForgeClient(WorldClient world, Vector3 pos) {
        super(world, pos, new Inventory(world.registry(), 9), 4, 3,
                Float.POSITIVE_INFINITY, 1.006f, 10, 50);
    }

    @Override
    public Optional<Gui> gui(MobPlayerClientMain player) {
        if (player instanceof MobPlayerClientMainVB) {
            return Optional.of(new GuiForgeInventory(this,
                    (MobPlayerClientMainVB) player));
        }
        return Optional.empty();
    }

    @Override
    public void update(double delta) {
        super.update(delta);
        if (temperature > 10) {
            Random random = ThreadLocalRandom.current();
            ParticleManager particleManager = world.particleManager();
            particleManager.add(new ParticleSmoke(particleManager, pos.now(),
                    new Vector3d(random.nextDouble() * 0.1 - 0.05,
                            random.nextDouble() * 0.1 - 0.05, 0.0),
                    random.nextFloat() * 360.0f, 6.0));
        }
    }
}
