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

package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

public abstract class MobLivingEquippedClient extends MobLivingClient
        implements EntityEquippedClient {
    protected MobLivingEquippedClient(WorldClient world, Vector3 pos,
            Vector3 speed, AABB aabb, double lives, double maxLives,
            Frustum viewField, Frustum hitField) {
        super(world, pos, speed, aabb, lives, maxLives, viewField, hitField);
    }
}