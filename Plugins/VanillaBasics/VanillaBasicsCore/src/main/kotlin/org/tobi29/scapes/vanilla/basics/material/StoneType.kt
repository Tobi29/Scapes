/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.vanilla.basics.material

import org.tobi29.scapes.block.Registries

class StoneType(val id: Int,
                val name: String,
                val textureRoot: String,
                val resistance: Double) {
    val texture = name.replace(" ", "")

    companion object {
        operator fun get(registry: Registries,
                         data: Int): StoneType {
            return registry.get<StoneType>("VanillaBasics", "StoneType")[data]
        }
    }
}
