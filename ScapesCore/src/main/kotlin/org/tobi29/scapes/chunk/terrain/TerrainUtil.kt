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

package org.tobi29.scapes.chunk.terrain

import org.tobi29.scapes.block.AABBElement
import org.tobi29.scapes.block.BlockType
import org.tobi29.scapes.engine.utils.Pool
import org.tobi29.scapes.engine.utils.ThreadLocal
import org.tobi29.scapes.engine.utils.math.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d
import org.tobi29.scapes.engine.utils.math.vector.distance
import org.tobi29.scapes.engine.utils.math.vector.plus

fun Terrain.selectBlock(pos: Vector3d,
                        distance: Double,
                        direction: Vector2d): PointerPane? {
    val pointerPanes = POINTER_PANES.get()
    pointerPanes(pos.intX(), pos.intY(), pos.intZ(), ceil(distance),
            pointerPanes)
    val factor = cos(direction.x.toRad()) * distance
    val lookX = cos(direction.y.toRad()) * factor
    val lookY = sin(direction.y.toRad()) * factor
    val lookZ = sin(direction.x.toRad()) * distance
    val t = pos + Vector3d(lookX, lookY, lookZ)
    var distanceSqr = sqr(distance)
    var closest: PointerPane? = null
    for (pane in pointerPanes) {
        val intersection = Intersection.intersectPointerPane(pos, t, pane)
        if (intersection != null) {
            val check = pos distance intersection
            if (check < distanceSqr) {
                closest = pane
                distanceSqr = check
            }
        }
    }
    pointerPanes.reset()
    return closest
}

fun Terrain.collisions(minX: Int,
                       minY: Int,
                       minZ: Int,
                       maxX: Int,
                       maxY: Int,
                       maxZ: Int,
                       pool: Pool<AABBElement>) {
    for (x in minX..maxX) {
        for (y in minY..maxY) {
            for (z in minZ..maxZ) {
                block(x, y, z) { addCollision(pool, this@collisions, x, y, z) }
            }
        }
    }
}

fun Terrain.pointerPanes(x: Int,
                         y: Int,
                         z: Int,
                         range: Int,
                         pool: Pool<PointerPane>) {
    for (xx in x - range..x + range) {
        for (yy in y - range..y + range) {
            for (zz in z - range..z + range) {
                block(xx, yy, zz) {
                    addPointerCollision(it, pool, xx, yy, zz)
                }
            }
        }
    }
}

fun Terrain.isSolid(x: Int,
                    y: Int,
                    z: Int) = block(x, y, z, BlockType::isSolid)

fun Terrain.isTransparent(x: Int,
                          y: Int,
                          z: Int) = block(x, y, z, BlockType::isTransparent)

fun Terrain.lightEmit(x: Int,
                      y: Int,
                      z: Int) = block(x, y, z, BlockType::lightEmit)

fun Terrain.lightTrough(x: Int,
                        y: Int,
                        z: Int) = block(x, y, z, BlockType::lightTrough)

inline fun <R> Terrain.block(x: Int,
                             y: Int,
                             z: Int,
                             block: BlockType.(Int) -> R) =
        block(x, y, z).let { block(type(it), data(it)) }

private val POINTER_PANES = ThreadLocal { Pool { PointerPane() } }
