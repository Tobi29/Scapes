/*
 * Copyright 2012-2017 Tobi29
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
package org.tobi29.scapes.vanilla.basics.material.block.device

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.block.TerrainTextureRegistry
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.terrain.TerrainServer
import org.tobi29.scapes.engine.utils.math.Face
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.server.MobPlayerServer
import org.tobi29.scapes.vanilla.basics.entity.server.MobBombServer
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial
import org.tobi29.scapes.vanilla.basics.material.BlockExplosive
import org.tobi29.scapes.vanilla.basics.material.block.BlockSimple
import org.tobi29.scapes.vanilla.basics.util.explosion
import org.tobi29.scapes.vanilla.basics.util.explosionBlockPush
import java.util.concurrent.ThreadLocalRandom

class BlockHyperBomb(materials: VanillaMaterial) : BlockSimple(materials,
        "vanilla.basics.block.HyperBomb"), BlockExplosive {

    override fun explode(terrain: TerrainServer,
                         x: Int,
                         y: Int,
                         z: Int) {
        terrain.queue { handle ->
            handle.explosionBlockPush(x + 0.5, y + 0.5, z + 0.5, 8.0, 0.2,
                    0.1, 64.0, 48.0)
        }
    }

    override fun explodeClient(world: WorldClient,
                               pos: Vector3d,
                               speed: Vector3d) {
        world.explosion(pos, speed, 1.0)
    }

    override fun igniteByExplosion(terrain: TerrainServer,
                                   x: Int,
                                   y: Int,
                                   z: Int,
                                   data: Int) {
        val random = ThreadLocalRandom.current()
        val entity = MobBombServer(terrain.world,
                Vector3d(x + 0.5, y + 0.5, z + 0.5),
                Vector3d(random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.2 + 0.2), this,
                0.toShort().toInt(),
                random.nextDouble() * 2.0)
        terrain.world.addEntityNew(entity)
    }

    override fun destroy(terrain: TerrainServer.TerrainMutable,
                         x: Int,
                         y: Int,
                         z: Int,
                         data: Int,
                         face: Face,
                         player: MobPlayerServer,
                         item: ItemStack): Boolean {
        val random = ThreadLocalRandom.current()
        terrain.world.addEntity(MobBombServer(terrain.world,
                Vector3d(x + 0.5, y + 0.5, z + 0.5),
                Vector3d(random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.1 - 0.05,
                        random.nextDouble() * 0.2 + 0.2), this,
                0.toShort().toInt(),
                6.0))
        return true
    }

    override fun resistance(item: ItemStack,
                            data: Int): Double {
        return 0.0
    }

    override fun drops(item: ItemStack,
                       data: Int): List<ItemStack> {
        return emptyList()
    }

    override fun footStepSound(data: Int): String {
        return "VanillaBasics:sound/footsteps/Wood.ogg"
    }

    override fun breakSound(item: ItemStack,
                            data: Int) = null

    override fun registerTextures(registry: TerrainTextureRegistry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/HyperBomb.png")
    }

    override fun name(item: ItemStack): String {
        return "HYPER-Bomb"
    }

    override fun maxStackSize(item: ItemStack): Int {
        return 256
    }
}
