package org.tobi29.scapes.packets;

import org.tobi29.scapes.block.GameRegistry;

public interface Packet {
    default short id(GameRegistry registry) {
        return (short) registry.getSupplier("Core", "Packet").id(this);
    }
}
