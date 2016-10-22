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

package org.tobi29.scapes.vanilla.basics

internal fun VanillaBasics.registerResearch() {
    research("Food",
            "Using a Quern, you can\ncreate grain out of this.",
            "vanilla.basics.item.Crop")
    research("Food", "Try making dough out of this?",
            "vanilla.basics.item.Grain")
    research("Metal",
            "You could try heating this on\na forge and let it melt\ninto a ceramic mold.\nMaybe you can find a way\nto shape it, to create\nhandy tools?",
            "vanilla.basics.item.OreChunk.Chalcocite")
    research("Iron",
            "Maybe you can figure out\na way to create a strong\nmetal out of this ore...",
            "vanilla.basics.item.OreChunk.Magnetite")
}
