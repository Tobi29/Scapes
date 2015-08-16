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
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketSkin extends Packet implements PacketServer, PacketClient {
    private Image image;
    private byte[] checksum;

    public PacketSkin() {
    }

    public PacketSkin(byte[] checksum) {
        this.checksum = checksum;
    }

    public PacketSkin(Image image, byte[] checksum) {
        this.image = image;
        this.checksum = checksum;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.put(image.buffer());
        stream.putByteArray(checksum);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        ByteBuffer buffer = BufferCreator.bytes(64 * 64 * 4);
        stream.get(buffer);
        buffer.flip();
        image = new Image(64, 64, buffer);
        checksum = stream.getByteArray();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world)
            throws ConnectionCloseException {
        client.world().scene().skinStorage().addSkin(checksum, image);
    }

    @Override
    public void sendServer(ClientConnection client, WritableByteStream stream)
            throws IOException {
        stream.putByteArray(checksum);
    }

    @Override
    public void parseServer(PlayerConnection player, ReadableByteStream stream)
            throws IOException {
        checksum = stream.getByteArray(1 << 10);
    }

    @Override
    public void runServer(PlayerConnection player, WorldServer world) {
        player.server().skin(checksum).ifPresent(
                skin -> player.send(new PacketSkin(skin.image(), checksum)));
    }
}
