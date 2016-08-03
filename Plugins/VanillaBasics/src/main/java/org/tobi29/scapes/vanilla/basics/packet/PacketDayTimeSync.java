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
package org.tobi29.scapes.vanilla.basics.packet;

import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.engine.utils.io.ReadableByteStream;
import org.tobi29.scapes.engine.utils.io.WritableByteStream;
import org.tobi29.scapes.packets.PacketAbstract;
import org.tobi29.scapes.packets.PacketClient;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentClimate;

import java.io.IOException;

public class PacketDayTimeSync extends PacketAbstract implements PacketClient {
    private long day;
    private double dayTime;

    public PacketDayTimeSync() {
    }

    public PacketDayTimeSync(double dayTime, long day) {
        this.dayTime = dayTime;
        this.day = day;
    }

    @Override
    public void sendClient(PlayerConnection player, WritableByteStream stream)
            throws IOException {
        stream.putFloat((float) dayTime);
        stream.putLong(day);
    }

    @Override
    public void parseClient(ClientConnection client, ReadableByteStream stream)
            throws IOException {
        dayTime = stream.getFloat();
        day = stream.getLong();
    }

    @Override
    public void runClient(ClientConnection client, WorldClient world) {
        if (world == null) {
            return;
        }
        EnvironmentClient environment = world.environment();
        if (environment instanceof EnvironmentClimate) {
            EnvironmentClimate environmentClimate =
                    (EnvironmentClimate) environment;
            ClimateGenerator climateGenerator = environmentClimate.climate();
            climateGenerator.setDayTime(dayTime);
            climateGenerator.setDay(day);
        }
    }
}
