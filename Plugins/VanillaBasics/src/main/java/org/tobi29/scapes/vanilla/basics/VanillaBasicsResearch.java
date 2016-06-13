package org.tobi29.scapes.vanilla.basics;

class VanillaBasicsResearch {
    static void registerResearch(VanillaBasics plugin) {
        plugin.c.research("Food",
                "Using a Quern, you can\ncreate grain out of this.",
                "vanilla.basics.item.Crop");
        plugin.c.research("Food", "Try making dough out of this?",
                "vanilla.basics.item.Grain");
        plugin.c.research("Metal",
                "You could try heating this on\na forge and let it melt\ninto a ceramic mold.\nMaybe you can find a way\nto shape it, to create\nhandy tools?",
                "vanilla.basics.item.OreChunk.Chalcocite");
        plugin.c.research("Iron",
                "Maybe you can figure out\na way to create a strong\nmetal out of this ore...",
                "vanilla.basics.item.OreChunk.Magnetite");
    }
}
