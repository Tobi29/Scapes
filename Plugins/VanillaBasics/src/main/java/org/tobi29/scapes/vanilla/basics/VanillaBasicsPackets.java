package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.vanilla.basics.packet.*;

class VanillaBasicsPackets {
    static void registerPackets(GameRegistry registry) {
        GameRegistry.SupplierRegistry<GameRegistry, Packet> r =
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
