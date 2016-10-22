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

package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.packets.PacketAbstract
import org.tobi29.scapes.vanilla.basics.packet.*

internal fun registerPackets(registry: GameRegistry) {
    registry.getSupplier<GameRegistry, PacketAbstract>("Core", "Packet").run {
        reg({ PacketDayTimeSync() }, PacketDayTimeSync::class.java,
                "vanilla.basics.packet.DayTimeSync")
        reg({ PacketCrafting() }, PacketCrafting::class.java,
                "vanilla.basics.packet.Crafting")
        reg({ PacketOpenCrafting() }, PacketOpenCrafting::class.java,
                "vanilla.basics.packet.OpenCrafting")
        reg({ PacketAnvil() }, PacketAnvil::class.java,
                "vanilla.basics.packet.Anvil")
        reg({ PacketLightning() }, PacketLightning::class.java,
                "vanilla.basics.packet.Lightning")
        reg({ PacketNotification() }, PacketNotification::class.java,
                "vanilla.basics.packet.Notification")
        reg({ PacketResearch() }, PacketResearch::class.java,
                "vanilla.basics.packet.Research")
        reg({ PacketQuern() }, PacketQuern::class.java,
                "vanilla.basics.packet.Quern")
    }
}