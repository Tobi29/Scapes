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

package org.tobi29.scapes.block;

import org.tobi29.scapes.engine.utils.math.AABB;

public class AABBElement {
    public final AABB aabb;
    public Collision collision;

    public AABBElement() {
        this(new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                BlockType.STANDARD_COLLISION);
    }

    public AABBElement(AABB aabb, Collision collision) {
        this.aabb = aabb;
        this.collision = collision;
    }

    public void set(double minX, double minY, double minZ, double maxX,
            double maxY, double maxZ, Collision collision) {
        aabb.minX = minX;
        aabb.minY = minY;
        aabb.minZ = minZ;
        aabb.maxX = maxX;
        aabb.maxY = maxY;
        aabb.maxZ = maxZ;
        this.collision = collision;
    }

    public boolean isSolid() {
        return collision.isSolid();
    }

    public AABB aabb() {
        return aabb;
    }
}
