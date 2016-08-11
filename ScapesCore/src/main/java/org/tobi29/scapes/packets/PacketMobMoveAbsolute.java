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
package org.tobi29.scapes.packets;

import java8.util.Optional;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.MobileEntity;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;
import java.util.UUID;

public class PacketMobMoveAbsolute extends PacketAbstract
        implements PacketBoth {
    private UUID uuid;
    private double x, y, z;

    public PacketMobMoveAbsolute() {
    }

    public PacketMobMoveAbsolute(UUID uuid, Vector3 pos, double x, double y,
            double z) {
        super(pos);
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putLong(uuid.getMostSignificantBits());
        stream.putLong(uuid.getLeastSignificantBits());
        stream.putDouble(x);
        stream.putDouble(y);
        stream.putDouble(z);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        uuid = new UUID(stream.getLong(), stream.getLong());
        x = stream.getDouble();
        y = stream.getDouble();
        z = stream.getDouble();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        Optional<EntityClient> fetch = world.entity(uuid);
        if (fetch.isPresent()) {
            EntityClient entity = fetch.get();
            if (entity instanceof MobileEntity) {
                ((MobileEntity) entity).positionHandler()
                        .receiveMoveAbsolute(x, y, z);
            }
        } else if (world.terrain()
                .isBlockTicking(FastMath.floor(x), FastMath.floor(y),
                        FastMath.floor(z))) {
            client.send(new PacketRequestEntity(uuid));
        }
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putDouble(x);
        stream.putDouble(y);
        stream.putDouble(z);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        x = stream.getDouble();
        y = stream.getDouble();
        z = stream.getDouble();
    }

    @Override
    public void runServer(PlayerConnection player) {
        player.mob(mob -> mob.positionHandler().receiveMoveAbsolute(x, y, z));
    }
}
