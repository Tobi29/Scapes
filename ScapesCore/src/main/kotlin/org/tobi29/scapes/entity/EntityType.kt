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

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.server.EntityServer

class EntityType<out C : EntityClient, out S : EntityServer>(
        val id: Int,
        private val client: (EntityType<C, S>, WorldClient) -> C,
        private val server: (EntityType<C, S>, WorldServer) -> S
) {
    fun createClient(world: WorldClient) = client(this, world)
    fun createServer(world: WorldServer) = server(this, world)
}
