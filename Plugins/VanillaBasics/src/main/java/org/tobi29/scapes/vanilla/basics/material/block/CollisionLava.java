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
package org.tobi29.scapes.vanilla.basics.material.block;

import org.tobi29.scapes.block.Collision;
import org.tobi29.scapes.entity.particle.ParticleInstance;
import org.tobi29.scapes.entity.server.MobLivingServer;
import org.tobi29.scapes.entity.server.MobServer;

public class CollisionLava extends Collision {
    public static final Collision INSTANCE = new CollisionLava();

    @Override
    public void inside(MobServer mob, double delta) {
        mob.setSpeed(mob.speed().div(1.0 + 1.2 * delta));
        if (mob instanceof MobLivingServer) {
            ((MobLivingServer) mob)
                    .damage(((MobLivingServer) mob).maxHealth() / 5.0);
        }
    }

    @Override
    public void inside(ParticleInstance particle, double delta) {
        particle.speed.div(1.0 + 1.2 * delta);
    }

    @Override
    public boolean isLiquid() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }
}
