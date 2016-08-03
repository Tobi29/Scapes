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

package org.tobi29.scapes.vanilla.basics.util;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.item.ItemMetal;

public class IngotUtil {
    public static void createIngot(VanillaBasics plugin, ItemStack item,
            String metalType, float temperature) {
        createIngot(item, plugin.metalType(metalType), temperature);
    }

    public static void createIngot(ItemStack item, MetalType metalType,
            float temperature) {
        MetalUtil.Alloy alloy = new MetalUtil.Alloy();
        alloy.add(metalType, 1.0);
        ((ItemMetal) item.material()).setAlloy(item, alloy);
        item.metaData("Vanilla").setFloat("Temperature", temperature);
    }
}
