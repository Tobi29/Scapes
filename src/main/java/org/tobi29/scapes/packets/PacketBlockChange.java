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

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketBlockChange extends Packet implements PacketClient {
    protected int x, y, z, id, data;

    public PacketBlockChange() {
    }

    public PacketBlockChange(int x, int y, int z, int id, int data) {
        super(new Vector3d(x + 0.5, y + 0.5, z + 0.5), true);
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
        this.data = data;
    }

    @Override
    public void sendClient(PlayerConnection player, DataOutputStream streamOut)
            throws IOException {
        streamOut.writeInt(x);
        streamOut.writeInt(y);
        streamOut.writeInt(z);
        streamOut.writeShort(id);
        streamOut.writeShort(data);
    }

    @Override
    public void parseClient(ClientConnection client, DataInputStream streamIn)
            throws IOException {
        x = streamIn.readInt();
        y = streamIn.readInt();
        z = streamIn.readInt();
        id = streamIn.readShort();
        data = streamIn.readShort();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        world.getTerrain().processPacket(this);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getBlockId() {
        return id;
    }

    public int getBlockData() {
        return data;
    }
}
