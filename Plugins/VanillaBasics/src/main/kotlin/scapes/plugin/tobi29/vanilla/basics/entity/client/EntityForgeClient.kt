/*
 * Copyright 2012-2016 Tobi29
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

package scapes.plugin.tobi29.vanilla.basics.entity.client

import org.tobi29.scapes.block.Inventory
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.utils.math.TWO_PI
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import org.tobi29.scapes.entity.particle.ParticleEmitterTransparent
import scapes.plugin.tobi29.vanilla.basics.VanillaBasics
import scapes.plugin.tobi29.vanilla.basics.gui.GuiForgeInventory
import java.util.concurrent.ThreadLocalRandom

class EntityForgeClient(world: WorldClient, pos: Vector3d = Vector3d.ZERO) : EntityAbstractFurnaceClient(
        world, pos, Inventory(world.registry, 9), 4, 3,
        Float.POSITIVE_INFINITY, 1.006f, 10, 50) {
    private var particleWait = 0.1

    override fun gui(player: MobPlayerClientMain): Gui? {
        if (player is MobPlayerClientMainVB) {
            return GuiForgeInventory(this, player, player.game.engine.guiStyle)
        }
        return null
    }

    override fun update(delta: Double) {
        super.update(delta)
        val plugin = world.plugins.plugin("VanillaBasics") as VanillaBasics
        if (temperature > 10) {
            particleWait -= delta
            while (particleWait < 0.0) {
                particleWait += 0.1
                val emitter = world.scene.particles().emitter(
                        ParticleEmitterTransparent::class.java)
                emitter.add { instance ->
                    val random = ThreadLocalRandom.current()
                    instance.pos.set(pos.now())
                    instance.speed.set(Vector3d(random.nextDouble() * 0.4 - 0.2,
                            random.nextDouble() * 0.4 - 0.2, 0.0))
                    instance.time = 12.0f
                    instance.setPhysics(-0.2f)
                    instance.setTexture(plugin.particles.smoke)
                    instance.rStart = 1.0f
                    instance.gStart = 1.0f
                    instance.bStart = 1.0f
                    instance.aStart = 1.0f
                    instance.rEnd = 0.3f
                    instance.gEnd = 0.3f
                    instance.bEnd = 0.3f
                    instance.aEnd = 0.0f
                    instance.sizeStart = 0.125f
                    instance.sizeEnd = 4.0f
                    instance.dir = random.nextFloat() * TWO_PI.toFloat()
                }
            }
        }
    }
}
