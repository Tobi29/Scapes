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

import org.tobi29.scapes.block.inventories
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.math.threadLocalRandom
import org.tobi29.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.client.GUI_COMPONENT
import org.tobi29.scapes.entity.particle.ParticleEmitterTransparent
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import org.tobi29.scapes.vanilla.basics.gui.GuiBloomeryInventory
import org.tobi29.io.tag.TagMap
import org.tobi29.io.tag.toBoolean
import org.tobi29.stdex.math.TWO_PI
import kotlin.math.max

class EntityBloomeryClient(
        type: EntityType<*, *>,
        world: WorldClient
) : EntityAbstractFurnaceClient(type, world, Vector3d.ZERO, 4, 9) {
    private var particleWait = 0.1
    var hasBellows = false
        private set

    init {
        inventories.add("Container", 14)
        registerComponent(GUI_COMPONENT) { player ->
            if (player is MobPlayerClientMainVB) {
                GuiBloomeryInventory(this, player, player.game.engine.guiStyle)
            } else null
        }
    }

    override fun read(map: TagMap) {
        super.read(map)
        hasBellows = map["Bellows"]?.toBoolean() ?: false
    }

    override fun update(delta: Double) {
        super.update(delta)
        if (temperature > 80.0) {
            val plugin = world.plugins.plugin<VanillaBasics>()
            particleWait += delta
            val particleTime = max(500.0 / temperature, 0.05)
            while (particleWait >= particleTime) {
                particleWait -= particleTime
                val emitter = world.scene.particles().emitter(
                        ParticleEmitterTransparent::class.java)
                emitter.add { instance ->
                    val random = threadLocalRandom()
                    instance.pos.set(pos.now())
                    instance.speed.set(
                            Vector3d(random.nextDouble() * 0.4 - 0.2,
                                    random.nextDouble() * 0.4 - 0.2, 0.0))
                    instance.time = 12.0f
                    instance.setPhysics(-0.2f)
                    instance.setTexture(emitter, plugin.particles.smoke)
                    instance.rStart = 1.0f
                    instance.gStart = 1.0f
                    instance.bStart = 1.0f
                    instance.aStart = 0.8f
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
