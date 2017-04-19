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

package org.tobi29.scapes.vanilla.basics.entity.client

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.utils.math.TWO_PI
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.threadLocalRandom
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.particle.ParticleEmitterTransparent
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.gui.GuiForgeInventory

class EntityForgeClient(type: EntityType<*, *>,
                        world: WorldClient) : EntityAbstractFurnaceClient(
        type, world, Vector3d.ZERO,
        Inventory(world.plugins, 9), 4, 3) {
    private var particleWait = 0.1

    override fun gui(player: MobPlayerClientMain): Gui? {
        if (player is MobPlayerClientMainVB) {
            return GuiForgeInventory(this, player, player.game.engine.guiStyle)
        }
        return null
    }

    override fun update(delta: Double) {
        super.update(delta)
        if (temperature > 80.0) {
            val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
            particleWait += delta
            val particleTime = max(500.0 / temperature, 0.05)
            while (particleWait >= particleTime) {
                particleWait -= particleTime
                val emitter = world.scene.particles().emitter(
                        ParticleEmitterTransparent::class.java)
                emitter.add { instance ->
                    val random = threadLocalRandom()
                    instance.pos.set(pos.now())
                    instance.speed.set(Vector3d(random.nextDouble() * 0.8 - 0.4,
                            random.nextDouble() * 0.8 - 0.4,
                            random.nextDouble() * 0.2))
                    instance.time = 12.0f
                    instance.setPhysics(-0.2f)
                    instance.setTexture(plugin.particles.smoke)
                    instance.rStart = 1.0f
                    instance.gStart = 1.0f
                    instance.bStart = 1.0f
                    instance.aStart = 0.6f
                    instance.rEnd = 0.3f
                    instance.gEnd = 0.3f
                    instance.bEnd = 0.3f
                    instance.aEnd = 0.0f
                    instance.sizeStart = 0.125f
                    instance.sizeEnd = 8.0f
                    instance.dir = random.nextFloat() * TWO_PI.toFloat()
                }
            }
        }
    }
}
