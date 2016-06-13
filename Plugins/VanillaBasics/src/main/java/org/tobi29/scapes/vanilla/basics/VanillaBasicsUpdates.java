package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.Update;
import org.tobi29.scapes.vanilla.basics.material.update.*;

class VanillaBasicsUpdates {
    static void registerUpdates(GameRegistry registry) {
        GameRegistry.SupplierRegistry<GameRegistry, Update> r =
                registry.getSupplier("Core", "Update");
        r.regS(UpdateWaterFlow::new, "vanilla.basics.update.WaterFlow");
        r.regS(UpdateLavaFlow::new, "vanilla.basics.update.LavaFlow");
        r.regS(UpdateGrassGrowth::new, "vanilla.basics.update.GrassGrowth");
        r.regS(UpdateFlowerGrowth::new, "vanilla.basics.update.FlowerGrowth");
        r.regS(UpdateSaplingGrowth::new, "vanilla.basics.update.SaplingGrowth");
        r.regS(UpdateStrawDry::new, "vanilla.basics.update.StrawDry");
    }
}
