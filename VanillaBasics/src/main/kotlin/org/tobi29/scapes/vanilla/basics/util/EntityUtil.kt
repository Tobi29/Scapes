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

package org.tobi29.scapes.vanilla.basics.util

import org.tobi29.math.vector.Vector3d
import org.tobi29.stdex.math.toRad
import org.tobi29.scapes.entity.server.MobServer
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.vanilla.basics.VanillaBasics
import kotlin.math.cos
import kotlin.math.sin

fun MobServer.dropItem(item: Item) {
    val plugin = world.plugins.plugin<VanillaBasics>()
    val pos = getCurrentPos()
    val rot = rot()
    val entity = plugin.entityTypes.item.createServer(world).apply {
        setPos(pos)
        setSpeed(Vector3d(cos(rot.z.toRad()) * 10.0 * cos(rot.x.toRad()),
                sin(rot.z.toRad()) * 10.0 * cos(rot.x.toRad()),
                sin(rot.x.toRad()) * 0.3 + 0.3))
        this.item = item
    }
    world.addEntityNew(entity)
}
