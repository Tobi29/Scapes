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

package org.tobi29.scapes.entity

import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d
import org.tobi29.scapes.engine.utils.math.vector.Vector3d

class MobPositionReceiver(pos: Vector3d,
                          private val positionListener: (Vector3d) -> Unit,
                          private val speedListener: (Vector3d) -> Unit,
                          private val rotationListener: (Vector3d) -> Unit,
                          private val stateListener: (Boolean, Boolean, Boolean, Boolean) -> Unit) {
    private val sentPosRelative: MutableVector3d

    init {
        sentPosRelative = MutableVector3d(pos)
    }

    constructor(positionListener: (Vector3d) -> Unit,
                speedListener: (Vector3d) -> Unit,
                rotationListener: (Vector3d) -> Unit,
                stateListener: (Boolean, Boolean, Boolean, Boolean) -> Unit
    ) : this(Vector3d.ZERO, positionListener, speedListener, rotationListener,
            stateListener)

    fun receiveMoveRelative(x: Byte,
                            y: Byte,
                            z: Byte) {
        val xx = x / 500.0
        val yy = y / 500.0
        val zz = z / 500.0
        sentPosRelative.plusX(xx)
        sentPosRelative.plusY(yy)
        sentPosRelative.plusZ(zz)
        positionListener(sentPosRelative.now())
    }

    fun receiveMoveAbsolute(x: Double,
                            y: Double,
                            z: Double) {
        sentPosRelative.setX(x)
        sentPosRelative.setY(y)
        sentPosRelative.setZ(z)
        positionListener(sentPosRelative.now())
    }

    fun receiveRotation(xRot: Double,
                        yRot: Double,
                        zRot: Double) {
        rotationListener(Vector3d(xRot, yRot, zRot))
    }

    fun receiveSpeed(xSpeed: Double,
                     ySpeed: Double,
                     zSpeed: Double) {
        speedListener(Vector3d(xSpeed, ySpeed, zSpeed))
    }

    fun receiveState(ground: Boolean,
                     slidingWall: Boolean,
                     inWater: Boolean,
                     swimming: Boolean) {
        stateListener(ground, slidingWall, inWater, swimming)
    }
}
