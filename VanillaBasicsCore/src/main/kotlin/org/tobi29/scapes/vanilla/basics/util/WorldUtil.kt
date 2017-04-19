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

import org.tobi29.scapes.block.ItemStack
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.threadLocalRandom
import org.tobi29.scapes.vanilla.basics.VanillaBasics

fun WorldServer.dropItems(items: List<ItemStack>,
                          x: Int,
                          y: Int,
                          z: Int,
                          despawntime: Double = 600.0) {
    val pos = Vector3d(x + 0.5, y + 0.5, z + 0.5)
    for (item in items) {
        dropItem(item, pos, despawntime)
    }
}

fun WorldServer.dropItem(item: ItemStack,
                         x: Int,
                         y: Int,
                         z: Int,
                         despawntime: Double = 600.0) {
    dropItem(item, Vector3d(x + 0.5, y + 0.5, z + 0.5), despawntime)
}

fun WorldServer.dropItem(item: ItemStack,
                         pos: Vector3d,
                         despawntime: Double = 600.0) {
    val plugin = plugins.plugin("VanillaBasics") as VanillaBasics
    val random = threadLocalRandom()
    val entity = plugin.entityTypes.item.createServer(this).apply {
        setPos(pos)
        setSpeed(Vector3d(-2.0 + random.nextDouble() * 4.0,
                -2.0 + random.nextDouble() * 4.0,
                random.nextDouble() * 1.0 + 0.5))
        this.item.set(item)
        this.despawntime = despawntime
    }
    addEntityNew(entity)
}