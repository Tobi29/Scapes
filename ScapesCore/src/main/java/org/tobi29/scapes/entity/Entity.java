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
package org.tobi29.scapes.entity;

import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

import java.util.UUID;

public interface Entity {
    UUID uuid();

    Vector3 pos();

    double x();

    double y();

    double z();

    default AABB aabb() {
        AABB aabb = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Vector3 pos = pos();
        aabb.add(pos.doubleX(), pos.doubleY(), pos.doubleZ());
        return aabb;
    }
}
