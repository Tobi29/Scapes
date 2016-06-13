package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.vanilla.basics.material.StoneType;

class VanillaBasicsStones {
    static GameRegistry.Registry<StoneType> registerStoneTypes(
            GameRegistry registry) {
        GameRegistry.Registry<StoneType> r =
                registry.get("VanillaBasics", "StoneType");
        r.reg(StoneType.DIRT_STONE, "vanilla.basics.stone.DirtStone");
        r.reg(StoneType.FLINT, "vanilla.basics.stone.Flint");
        r.reg(StoneType.CHALK, "vanilla.basics.stone.Chalk");
        r.reg(StoneType.CHERT, "vanilla.basics.stone.Chert");
        r.reg(StoneType.CLAYSTONE, "vanilla.basics.stone.Claystone");
        r.reg(StoneType.CONGLOMERATE, "vanilla.basics.stone.Conglomerate");
        r.reg(StoneType.MARBLE, "vanilla.basics.stone.Marble");
        r.reg(StoneType.ANDESITE, "vanilla.basics.stone.Andesite");
        r.reg(StoneType.BASALT, "vanilla.basics.stone.Basalt");
        r.reg(StoneType.DACITE, "vanilla.basics.stone.Dacite");
        r.reg(StoneType.RHYOLITE, "vanilla.basics.stone.Rhyolite");
        r.reg(StoneType.DIORITE, "vanilla.basics.stone.Diorite");
        r.reg(StoneType.GABBRO, "vanilla.basics.stone.Gabbro");
        r.reg(StoneType.GRANITE, "vanilla.basics.stone.Granite");
        return r;
    }
}
