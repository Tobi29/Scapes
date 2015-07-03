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
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.io.IOException;

public class PacketSoundEffect extends Packet implements PacketClient {
    private String audio;
    private Vector3 position, velocity;
    private float pitch, gain, range;

    public PacketSoundEffect() {
    }

    public PacketSoundEffect(String audio, Vector3 position, Vector3 velocity,
            float pitch, float gain, float range) {
        super(position, range);
        this.position = position;
        this.velocity = velocity;
        this.audio = audio;
        this.pitch = pitch;
        this.gain = gain;
        this.range = range;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putString(audio);
        stream.putDouble(position.doubleX());
        stream.putDouble(position.doubleY());
        stream.putDouble(position.doubleZ());
        stream.putDouble(velocity.doubleX());
        stream.putDouble(velocity.doubleY());
        stream.putDouble(velocity.doubleZ());
        stream.putFloat(pitch);
        stream.putFloat(gain);
        stream.putFloat(range);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        audio = stream.getString();
        position = new Vector3d(stream.getDouble(), stream.getDouble(),
                stream.getDouble());
        velocity = new Vector3d(stream.getDouble(), stream.getDouble(),
                stream.getDouble());
        pitch = stream.getFloat();
        gain = stream.getFloat();
        range = stream.getFloat();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        world.playSound(audio, position, velocity, pitch, gain, range);
    }
}
