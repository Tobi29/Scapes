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

package org.tobi29.scapes.vanilla.basics.packet;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3f;
import org.tobi29.scapes.packets.PacketAbstract;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.entity.particle.ParticleEmitterLightning;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class PacketLightning extends PacketAbstract implements PacketClient {
    private static final String[] SOUNDS =
            {"VanillaBasics:sound/entity/particle/thunder/Close1.ogg",
                    "VanillaBasics:sound/entity/particle/thunder/Close2.ogg",
                    "VanillaBasics:sound/entity/particle/thunder/Close3.ogg"};
    private double x, y, z;

    public PacketLightning() {
    }

    public PacketLightning(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putDouble(x);
        stream.putDouble(y);
        stream.putDouble(z);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        x = stream.getDouble();
        y = stream.getDouble();
        z = stream.getDouble();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        Vector3 pos = new Vector3d(x, y, z);
        ParticleEmitterLightning emitter = world.scene().particles()
                .emitter(ParticleEmitterLightning.class);
        emitter.add(instance -> {
            Random random = ThreadLocalRandom.current();
            instance.pos.set(pos);
            instance.speed.set(Vector3f.ZERO);
            instance.time = 0.5f;
            instance.vao = random.nextInt(emitter.maxVAO());
        });
        Random random = ThreadLocalRandom.current();
        world.playSound(SOUNDS[random.nextInt(3)], pos, Vector3d.ZERO, 1.0f,
                64.0f);
    }
}
