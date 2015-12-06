/*
 * Copyright 2012-2015 Tobi29
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
package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.entity.WieldMode;

public interface EntityEquippedClient {
    ItemStack leftWeapon();

    ItemStack rightWeapon();

    default WieldMode wieldMode() {
        return WieldMode.DUAL;
    }

    default float leftCharge() {
        return 0.0f;
    }

    default float rightCharge() {
        return 0.0f;
    }
}
