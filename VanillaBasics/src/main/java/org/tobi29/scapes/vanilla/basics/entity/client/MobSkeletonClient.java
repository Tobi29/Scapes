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

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.client.MobLivingEquippedClient;
import org.tobi29.scapes.entity.model.MobLivingModelHuman;
import org.tobi29.scapes.entity.model.MobModel;
import org.tobi29.scapes.entity.particle.ParticleManager;

import java.util.Optional;

public class MobSkeletonClient extends MobLivingEquippedClient {
    public MobSkeletonClient(WorldClient world) {
        this(world, Vector3d.ZERO, Vector3d.ZERO, 0.0, 0.0);
    }

    public MobSkeletonClient(WorldClient world, Vector3 pos, Vector3 speed,
            double xRot, double zRot) {
        super(world, pos, speed, new AABB(-0.4, -0.4, -1, 0.4, 0.4, 0.9), 20.0,
                30.0, new Frustum(90, 1, 0.1, 24),
                new Frustum(20, 0.5, 0.1, 0.2));
        rot.setX(xRot);
        rot.setZ(zRot);
    }

    @Override
    public void onDeath() {
        Texture texture =
                world.getGame().getEngine().getGraphics().getTextureManager()
                        .getTexture("VanillaBasics:image/entity/mob/Skeleton");
        ParticleManager particleManager = world.getParticleManager();
        MobLivingModelHuman
                .getParticles(particleManager, pos.now(), speed.now(),
                        rot.now(), texture, true, false);
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTER;
    }

    @Override
    public Vector3 getViewOffset() {
        return new Vector3d(0.0, 0.0, 0.7);
    }

    @Override
    public Optional<MobModel> createModel() {
        Texture texture =
                world.getGame().getEngine().getGraphics().getTextureManager()
                        .getTexture("VanillaBasics:image/entity/mob/Skeleton");
        return Optional.of(new MobLivingModelHuman(this, texture, true, false));
    }

    @Override
    public ItemStack getLeftWeapon() {
        return new ItemStack(registry);
    }

    @Override
    public ItemStack getRightWeapon() {
        return new ItemStack(registry);
    }
}
