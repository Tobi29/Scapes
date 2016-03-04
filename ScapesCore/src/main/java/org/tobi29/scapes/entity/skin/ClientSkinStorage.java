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

import java8.util.stream.Collectors;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.opengl.texture.Texture;
import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.packets.PacketSkin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientSkinStorage {
    private final Map<Checksum, ClientSkin> skins = new ConcurrentHashMap<>();
    private final Queue<Checksum> skinRequests = new ConcurrentLinkedQueue<>();
    private final ByteBuffer defaultSkin;

    public ClientSkinStorage(Texture defaultTexture) {
        defaultSkin = defaultTexture.buffer(0);
    }

    public void update(ClientConnection connection) {
        List<ClientSkin> oldSkins = Streams.of(skins.values())
                .filter(skin -> skin.increaseTicks() > 1200)
                .collect(Collectors.toList());
        Streams.of(oldSkins).forEach(skin -> {
            skins.remove(skin.checksum());
            skin.markDisposed();
        });
        while (!skinRequests.isEmpty()) {
            connection.send(new PacketSkin(skinRequests.poll().array()));
        }
    }

    public void dispose() {
        Streams.of(skins.values()).forEach(Texture::markDisposed);
    }

    public void addSkin(Checksum checksum, Image image) {
        ByteBuffer imageBuffer = image.buffer();
        ByteBuffer buffer = BufferCreator.bytes(imageBuffer.remaining());
        buffer.put(imageBuffer);
        buffer.rewind();
        ClientSkin skin = skins.get(checksum);
        if (skin != null) {
            skin.setImage(buffer);
        }
    }

    public Texture get(Checksum checksum) {
        ClientSkin skin = skins.get(checksum);
        if (skin == null) {
            skin = new ClientSkin(defaultSkin, checksum);
            skins.put(checksum, skin);
            skinRequests.add(checksum);
        }
        return skin;
    }
}
