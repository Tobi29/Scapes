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

package org.tobi29.scapes.entity.ai

import org.tobi29.scapes.engine.utils.math.toDeg
import org.tobi29.scapes.engine.math.vector.Vector3d
import org.tobi29.scapes.engine.math.vector.direction
import org.tobi29.scapes.engine.math.vector.distanceSqr
import org.tobi29.scapes.entity.server.MobLivingServer
import org.tobi29.scapes.entity.server.MobServer

class SimpleAI(private val mob: MobLivingServer) : AI {
    private var timeout = 0.0
    private var target: Vector3d? = null
    private var mobTarget: MobServer? = null
    private var yaw = 0.0

    override fun update(delta: Double) {
        mobTarget?.let { target = it.getCurrentPos() }
        target?.let {
            val pos = mob.getCurrentPos()
            yaw = pos.direction(it).toDeg()
            timeout -= delta
            if (timeout <= 0.0) {
                target = null
                mobTarget = null
            } else if (pos.distanceSqr(it) < 4.0) {
                target = null
            }
        }
    }

    override fun setMobTarget(target: MobServer,
                              timeout: Double) {
        mobTarget = target
        this.timeout = timeout
    }

    override fun setPositionTarget(target: Vector3d,
                                   timeout: Double) {
        this.target = target
        this.timeout = timeout
    }

    override fun hasTarget(): Boolean {
        return target != null || mobTarget != null
    }

    override fun hasMobTarget(): Boolean {
        return mobTarget != null
    }

    override fun targetYaw(): Double {
        return yaw
    }
}
