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

package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.packets.PacketAbstract;
import org.tobi29.scapes.vanilla.basics.packet.*;

class VanillaBasicsPackets {
    static void registerPackets(GameRegistry registry) {
        GameRegistry.SupplierRegistry<GameRegistry, PacketAbstract> r =
                registry.getSupplier("Core", "Packet");
        r.regS(PacketDayTimeSync::new, "vanilla.basics.packet.DayTimeSync");
        r.regS(PacketCrafting::new, "vanilla.basics.packet.Crafting");
        r.regS(PacketOpenCrafting::new, "vanilla.basics.packet.OpenCrafting");
        r.regS(PacketAnvil::new, "vanilla.basics.packet.Anvil");
        r.regS(PacketLightning::new, "vanilla.basics.packet.Lightning");
        r.regS(PacketNotification::new, "vanilla.basics.packet.Notification");
        r.regS(PacketResearch::new, "vanilla.basics.packet.Research");
        r.regS(PacketQuern::new, "vanilla.basics.packet.Quern");
    }
}
