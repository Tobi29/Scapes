package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.vanilla.basics.material.CropType;

class VanillaBasicsCrops {
    static GameRegistry.Registry<CropType> registerCropTypes(
            GameRegistry registry) {
        GameRegistry.Registry<CropType> r =
                registry.get("VanillaBasics", "CropType");
        r.reg(CropType.WHEAT, "vanilla.basics.crop.Wheat");
        return r;
    }
}
