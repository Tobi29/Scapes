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

package org.tobi29.scapes.entity.skin;

import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.packets.PacketSkin;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class ClientSkinStorage {
    private final Map<Checksum, ClientSkin> skins = new ConcurrentHashMap<>();
    private final Queue<byte[]> skinRequests = new ConcurrentLinkedQueue<>();
    private final ByteBuffer defaultSkin;

    public ClientSkinStorage(Texture defaultTexture) {
        defaultSkin = defaultTexture.getBuffer();
    }

    public void update(GraphicsSystem graphics, ClientConnection connection) {
        List<ClientSkin> oldSkins = skins.values().stream()
                .filter(skin -> skin.increaseTicks() > 1200)
                .collect(Collectors.toList());
        oldSkins.forEach(skin -> {
            skins.remove(new Checksum(skin.checksum()));
            skin.dispose(graphics);
        });
        while (!skinRequests.isEmpty()) {
            connection.send(new PacketSkin(skinRequests.poll()));
        }
    }

    public void dispose(GraphicsSystem graphics) {
        skins.values().forEach(skin -> skin.dispose(graphics));
    }

    public void addSkin(byte[] checksum, Image image) {
        ByteBuffer imageBuffer = image.getBuffer();
        ByteBuffer buffer =
                BufferCreatorDirect.byteBuffer(imageBuffer.remaining());
        buffer.put(imageBuffer);
        buffer.rewind();
        ClientSkin skin = skins.get(new Checksum(checksum));
        if (skin != null) {
            skin.setImage(buffer);
        }
    }

    public Texture getSkin(byte[] checksum) {
        ClientSkin skin = skins.get(checksum);
        if (skin == null) {
            skin = new ClientSkin(defaultSkin, checksum);
            skins.put(new Checksum(checksum), skin);
            skinRequests.add(checksum);
        }
        return skin;
    }

    private static class Checksum {
        protected final byte[] array;

        private Checksum(byte[] array) {
            this.array = array;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Checksum &&
                    Arrays.equals(array, ((Checksum) obj).array);
        }
    }
}
