/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.entity.ai;

import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.server.MobLivingServer;
import org.tobi29.scapes.entity.server.MobServer;

public class SimpleAI implements AI {
    private final MobLivingServer mob;
    private double timeout;
    private Vector3 target;
    private MobServer mobTarget;
    private double yaw;

    public SimpleAI(MobLivingServer mob) {
        this.mob = mob;
    }

    @Override
    public void update(double delta) {
        if (mobTarget != null) {
            target = mobTarget.pos();
        }
        if (target != null) {
            Vector3 pos = mob.pos();
            yaw = FastMath.pointDirection(pos, target);
            timeout -= delta;
            if (timeout <= 0.0) {
                target = null;
                mobTarget = null;
            } else if (FastMath.pointDistanceSqr(pos, target) < 4.0) {
                target = null;
            }
        }
    }

    @Override
    public void setMobTarget(MobServer target, double timeout) {
        mobTarget = target;
        this.timeout = timeout;
    }

    @Override
    public void setPositionTarget(Vector3 target, double timeout) {
        this.target = target;
        this.timeout = timeout;
    }

    @Override
    public boolean hasTarget() {
        return target != null || mobTarget != null;
    }

    @Override
    public boolean hasMobTarget() {
        return mobTarget != null;
    }

    @Override
    public double targetYaw() {
        return yaw;
    }
}
