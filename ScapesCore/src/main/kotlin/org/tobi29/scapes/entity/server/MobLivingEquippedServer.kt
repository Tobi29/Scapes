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

package org.tobi29.scapes.entity.server

import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.engine.utils.math.AABB
import org.tobi29.scapes.engine.utils.math.Frustum
import org.tobi29.scapes.engine.utils.math.vector.Vector3d

abstract class MobLivingEquippedServer(world: WorldServer, pos: Vector3d,
                                       speed: Vector3d, aabb: AABB, lives: Double, maxLives: Double,
                                       viewField: Frustum, hitField: Frustum) : MobLivingServer(
        world, pos, speed, aabb, lives, maxLives, viewField,
        hitField), EntityEquippedServer
