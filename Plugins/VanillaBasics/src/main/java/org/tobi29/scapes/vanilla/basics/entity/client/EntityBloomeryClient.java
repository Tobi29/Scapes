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
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleSmoke;
import org.tobi29.scapes.vanilla.basics.gui.GuiBloomeryInventory;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class EntityBloomeryClient extends EntityAbstractFurnaceClient {
    private boolean hasBellows;

    public EntityBloomeryClient(WorldClient world) {
        this(world, Vector3d.ZERO);
    }

    public EntityBloomeryClient(WorldClient world, Vector3 pos) {
        super(world, pos, new Inventory(world.registry(), 14), 4, 9, 800.0f,
                1.004f, 4, 50);
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        hasBellows = tagStructure.getBoolean("Bellows");
        maximumTemperature = hasBellows ? Float.POSITIVE_INFINITY : 600.0f;
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

    @Override
    public Optional<Gui> gui(MobPlayerClientMain player) {
        if (player instanceof MobPlayerClientMainVB) {
            return Optional.of(new GuiBloomeryInventory(this,
                    (MobPlayerClientMainVB) player,
                    player.game().engine().globalGUI().style()));
        }
        return Optional.empty();
    }

    public boolean hasBellows() {
        return hasBellows;
    }
}
