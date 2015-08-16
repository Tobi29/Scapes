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

package org.tobi29.scapes.vanilla.basics.material.block.device;

import org.tobi29.scapes.block.BlockExplosive;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.particle.ParticleManager;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.entity.server.MobBombServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleExplosion;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimple;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BlockHyperBomb extends BlockSimple implements BlockExplosive {
    public BlockHyperBomb(VanillaMaterial materials) {
        super(materials, "vanilla.basics.block.HyperBomb");
    }

    @Override
    public void explode(TerrainServer terrain, int x, int y, int z) {
        terrain.world()
                .explosionBlockPush(x + 0.5, y + 0.5, z + 0.5, 8.0, 0.2, 0.1,
                        64.0, 48.0);
    }

    @Override
    public void explodeClient(WorldClient world, Vector3 pos, Vector3 speed) {
        Random random = new Random();
        ParticleManager particleManager = world.particleManager();
        for (int i = 0; i < 40; i++) {
            double dirZ = random.nextDouble() * FastMath.TWO_PI;
            double dirX = random.nextDouble() * FastMath.PI - FastMath.HALF_PI;
            double dirSpeed = random.nextDouble() * 0.5 + 2.0;
            double dirSpeedX =
                    FastMath.cosTable(dirZ) * FastMath.cosTable(dirX) *
                            dirSpeed;
            double dirSpeedY =
                    FastMath.sinTable(dirZ) * FastMath.cosTable(dirX) *
                            dirSpeed;
            double dirSpeedZ = FastMath.sinTable(dirX) * dirSpeed;
            particleManager.add(new ParticleExplosion(particleManager,
                    pos.plus(new Vector3d(dirSpeedX, dirSpeedY, dirSpeedZ)),
                    speed.plus(new Vector3d(dirSpeedX, dirSpeedY, dirSpeedZ)),
                    random.nextDouble() * 0.25 + 0.25));
        }
    }

    @Override
    public void igniteByExplosion(TerrainServer terrain, int x, int y, int z) {
        Random random = ThreadLocalRandom.current();
        EntityServer entity = new MobBombServer(terrain.world(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5),
                new Vector3d(random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.2 + 0.2), this, (short) 0,
                random.nextDouble() * 2.0);
        entity.onSpawn();
        terrain.world().addEntity(entity);
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        Random random = ThreadLocalRandom.current();
        terrain.world().addEntity(new MobBombServer(terrain.world(),
                new Vector3d(x + 0.5, y + 0.5, z + 0.5),
                new Vector3d(random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.2 + 0.2), this, (short) 0,
                6.0));
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return 0;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        return Collections.emptyList();
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Wood.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "";
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/HyperBomb.png");
    }

    @Override
    public String name(ItemStack item) {
        return "HYPER-Bomb";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 256;
    }
}
