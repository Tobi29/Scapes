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

package org.tobi29.scapes.vanilla.basics.entity.server

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.io.tag.ReadWriteTagMap
import org.tobi29.scapes.engine.utils.io.tag.TagMap
import org.tobi29.scapes.engine.utils.io.tag.set
import org.tobi29.scapes.engine.utils.io.tag.toDouble
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.entity.server.EntityAbstractServer
import org.tobi29.scapes.packets.PacketEntityChange

class EntityBlockBreakServer(type: EntityType<*, *>,
                             world: WorldServer) : EntityAbstractServer(
        type, world, Vector3d.ZERO) {
    private var progress = 0.0
    private var wait = 0.0

    override fun write(map: ReadWriteTagMap) {
        super.write(map)
        map["Progress"] = progress
        map["Wait"] = wait
    }

    override fun read(map: TagMap) {
        super.read(map)
        map["Progress"]?.toDouble()?.let { progress = it }
        map["Wait"]?.toDouble()?.let { wait = it }
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
        world.send(PacketEntityChange(registry, this))
        return progress >= 1.0
    }
}
