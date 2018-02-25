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

import org.tobi29.scapes.engine.graphics.GL
import org.tobi29.scapes.engine.graphics.Shader
import org.tobi29.scapes.engine.gui.GuiComponentHeavy
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.math.vector.Vector2d
import org.tobi29.scapes.inventory.Item

class GuiComponentItem(
        parent: GuiLayoutData,
        var item: () -> Item?
) : GuiComponentHeavy(parent) {
    constructor(
            parent: GuiLayoutData,
            item: Item? = null
    ) : this(parent, { item })

    public override fun renderComponent(gl: GL,
                                        shader: Shader,
                                        size: Vector2d,
                                        pixelSize: Vector2d,
                                        delta: Double) {
        super.renderComponent(gl, shader, size, pixelSize, delta)
        GuiUtils.items(0.0f, 0.0f, size.x.toFloat(), size.y.toFloat(), item(),
                gl, shader, gui.style.font, pixelSize)
    }
}
