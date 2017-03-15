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

package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.packets.PacketType
import org.tobi29.scapes.vanilla.basics.packet.*

internal fun registerPackets(registry: Registries) {
    registry.get<PacketType>("Core", "Packet").run {
        reg("vanilla.basics.packet.DayTimeSync") {
            PacketType(it, ::PacketDayTimeSync)
        }
        reg("vanilla.basics.packet.Crafting") {
            PacketType(it, ::PacketCrafting)
        }
        reg("vanilla.basics.packet.OpenCrafting") {
            PacketType(it, ::PacketOpenCrafting)
        }
        reg("vanilla.basics.packet.Anvil") {
            PacketType(it, ::PacketAnvil)
        }
        reg("vanilla.basics.packet.Lightning") {
            PacketType(it, ::PacketLightning)
        }
        reg("vanilla.basics.packet.Notification") {
            PacketType(it, ::PacketNotification)
        }
        reg("vanilla.basics.packet.Research") {
            PacketType(it, ::PacketResearch)
        }
        reg("vanilla.basics.packet.Quern") {
            PacketType(it, ::PacketQuern)
        }
    }
}