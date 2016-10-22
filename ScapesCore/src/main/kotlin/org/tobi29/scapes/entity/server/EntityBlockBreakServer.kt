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
package org.tobi29.scapes.entity.server

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.TagStructure
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.engine.utils.io.tag.setDouble
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.packets.PacketEntityChange

class EntityBlockBreakServer(world: WorldServer, pos: Vector3d = Vector3d.ZERO) : EntityServer(
        world, pos) {
    private var progress = 0.0
    private var wait = 0.0

    override fun write(): TagStructure {
        val tag = super.write()
        tag.setDouble("Progress", progress)
        tag.setDouble("Wait", wait)
        return tag
    }

    override fun read(tagStructure: TagStructure) {
        super.read(tagStructure)
        tagStructure.getDouble("Progress")?.let { progress = it }
        tagStructure.getDouble("Wait")?.let { wait = it }
    }

    override fun update(delta: Double) {
        wait += delta
        if (wait >= 6.0 || progress >= 1.0) {
            world.removeEntity(this)
        }
    }

    fun punch(world: WorldServer,
              strength: Double): Boolean {
        wait = 0.0
        progress = clamp(progress + strength, 0.0, 1.0)
        world.send(PacketEntityChange(this))
        return progress >= 1.0
    }
}
