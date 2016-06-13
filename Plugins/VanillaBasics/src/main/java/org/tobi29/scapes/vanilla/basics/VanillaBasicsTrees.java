package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.vanilla.basics.material.TreeType;

class VanillaBasicsTrees {
    static GameRegistry.Registry<TreeType> registerTreeTypes(
            GameRegistry registry) {
        GameRegistry.Registry<TreeType> r =
                registry.get("VanillaBasics", "TreeType");
        r.reg(TreeType.OAK, "vanilla.basics.tree.Oak");
        r.reg(TreeType.BIRCH, "vanilla.basics.tree.Birch");
        r.reg(TreeType.SPRUCE, "vanilla.basics.tree.Spruce");
        r.reg(TreeType.PALM, "vanilla.basics.tree.Palm");
        r.reg(TreeType.MAPLE, "vanilla.basics.tree.Maple");
        r.reg(TreeType.SEQUOIA, "vanilla.basics.tree.Sequoia");
        r.reg(TreeType.WILLOW, "vanilla.basics.tree.Willow");
        return r;
    }
}
