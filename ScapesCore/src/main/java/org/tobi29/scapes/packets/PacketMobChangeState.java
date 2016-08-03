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
package org.tobi29.scapes.packets;

import java8.util.Optional;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.MobileEntity;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketMobChangeState extends PacketAbstract implements PacketBoth {
    private int entityID;
    private boolean ground, slidingWall, inWater, swimming;

    public PacketMobChangeState() {
    }

    public PacketMobChangeState(int entityID, Vector3 pos, boolean ground,
            boolean slidingWall, boolean inWater, boolean swimming) {
        super(pos, 32.0, false, false);
        this.entityID = entityID;
        this.ground = ground;
        this.slidingWall = slidingWall;
        this.inWater = inWater;
        this.swimming = swimming;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putInt(entityID);
        int b = (ground ? 1 : 0) | (slidingWall ? 2 : 0) | (inWater ? 4 : 0) |
                (swimming ? 8 : 0);
        stream.put(b);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        entityID = stream.getInt();
        byte value = stream.get();
        ground = (value & 1) == 1;
        slidingWall = (value & 2) == 2;
        inWater = (value & 4) == 4;
        swimming = (value & 8) == 8;
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        Optional<EntityClient> fetch = world.entity(entityID);
        if (fetch.isPresent()) {
            EntityClient entity = fetch.get();
            if (entity instanceof MobileEntity) {
                ((MobileEntity) entity).positionHandler()
                        .receiveState(ground, slidingWall, inWater, swimming);
            }
        } else {
            client.send(new PacketRequestEntity(entityID));
        }
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        int b = (ground ? 1 : 0) | (slidingWall ? 2 : 0) | (inWater ? 4 : 0) |
                (swimming ? 8 : 0);
        stream.put(b);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        byte value = stream.get();
        ground = (value & 1) == 1;
        slidingWall = (value & 2) == 2;
        inWater = (value & 4) == 4;
        swimming = (value & 8) == 8;
    }

    @Override
    public void runServer(PlayerConnection player) {
        player.mob(mob -> mob.positionHandler()
                .receiveState(ground, slidingWall, inWater, swimming));
    }
}
