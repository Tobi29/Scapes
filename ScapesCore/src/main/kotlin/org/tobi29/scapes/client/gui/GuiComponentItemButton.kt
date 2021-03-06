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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.engine.gui.GuiComponentButtonHeavy
import org.tobi29.scapes.engine.gui.GuiComponentText
import org.tobi29.scapes.engine.gui.GuiContainerRow
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.inventory.Item
import org.tobi29.scapes.inventory.ItemTypeNamed
import org.tobi29.scapes.inventory.kind
import org.tobi29.scapes.inventory.name

class GuiComponentItemButton(
        parent: GuiLayoutData,
        private val item: () -> Item?
) : GuiComponentButtonHeavy(parent) {
    init {
        addSubHori(0.0, 0.0, -1.0, -1.0) {
            GuiComponentItem(it, item)
        }
    }

    constructor(parent: GuiLayoutData,
                item: Item?) : this(parent, { item })

    override fun tooltip(p: GuiContainerRow): (() -> Unit)? {
        val text = p.addVert(15.0, 15.0, -1.0, 16.0) {
            GuiComponentText(it, "")
        }
        return {
            text.text = item().kind<ItemTypeNamed>()?.name ?: ""
        }
    }
}
