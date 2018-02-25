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

package org.tobi29.scapes.packets

import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.client.connection.ClientConnection
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.server.EntityServer
import org.tobi29.scapes.server.connection.PlayerConnection
import org.tobi29.uuid.Uuid

fun ClientConnection.getEntity(uuid: Uuid?,
                               consumer: (EntityClient) -> Unit) {
    if (uuid == null) {
        return
    }
    mob { it.world.getEntity(uuid, consumer) }
}

fun WorldClient.getEntity(uuid: Uuid?,
                          consumer: (EntityClient) -> Unit) {
    if (uuid == null) {
        return
    }
    val entity = getEntity(uuid)
    if (entity == null) {
        send(PacketRequestEntity(registry, uuid))
    } else {
        consumer(entity)
    }
}

fun PlayerConnection.getEntity(uuid: Uuid?,
                               consumer: (EntityServer) -> Unit) {
    if (uuid == null) {
        return
    }
    mob { it.world.getEntity(uuid, consumer) }
}

fun WorldServer.getEntity(uuid: Uuid?,
                          consumer: (EntityServer) -> Unit) {
    if (uuid == null) {
        return
    }
    getEntity(uuid)?.let { consumer(it) }
}
