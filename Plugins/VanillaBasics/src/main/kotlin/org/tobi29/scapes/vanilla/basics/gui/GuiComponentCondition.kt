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

package org.tobi29.scapes.vanilla.basics.gui

import org.tobi29.scapes.client.gui.GuiComponentBar
import org.tobi29.scapes.engine.gui.GuiComponentGroup
import org.tobi29.scapes.engine.gui.GuiComponentGroupSlab
import org.tobi29.scapes.engine.gui.GuiLayoutData
import org.tobi29.scapes.engine.utils.io.tag.getDouble
import org.tobi29.scapes.entity.client.MobPlayerClientMain

class GuiComponentCondition(parent: GuiLayoutData,
                            private val player: MobPlayerClientMain) : GuiComponentGroup(
        parent) {

    init {
        addVert(0.0, 0.0, -1.0, -1.0) {
            GuiComponentBar(it, 0.0f, 1.0f, 0.0f, 0.6f, 1.0) {
                value("Stamina")
            }
        }
        val bottom = addVert(0.0, 0.0, -1.0, -2.0,
                ::GuiComponentGroupSlab)
        bottom.addHori(0.0, 0.0, -1.0, -1.0) {
            GuiComponentBar(it, 1.0f, 0.0f, 0.0f, 0.6f,
                    1.0) { player.health() / player.maxHealth() }
        }
        val bottomRight = bottom.addHori(0.0, 0.0, -1.0,
                -1.0, ::GuiComponentGroup)
        bottomRight.addVert(0.0, 0.0, -1.0, -1.0) {
            GuiComponentBar(it, 1.0f, 0.5f, 0.0f, 0.6f, 1.0) { value("Hunger") }
        }
        bottomRight.addVert(0.0, 0.0, -1.0, -1.0) {
            GuiComponentBar(it, 0.0f, 0.2f, 1.0f, 0.6f, 1.0) { value("Thirst") }
        }
    }

    private fun value(name: String): Double {
        val conditionTag = player.metaData("Vanilla").structure("Condition")
        return conditionTag.getDouble(name) ?: 0.0
    }
}