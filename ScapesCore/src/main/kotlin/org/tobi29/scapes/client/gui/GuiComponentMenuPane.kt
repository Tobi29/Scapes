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

package org.tobi29.scapes.client.gui

import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.utils.math.vector.Vector2d

class GuiComponentMenuPane(parent: GuiLayoutData,
                           private val compactHeight: Double) : GuiComponentPane(
        parent) {

    fun <T : GuiComponent> addControl(marginX: Double,
                                      marginY: Double,
                                      width: Double,
                                      height: Double,
                                      marginHoriX: Double,
                                      marginHoriY: Double,
                                      widthHori: Double,
                                      heightHori: Double,
                                      child: (GuiLayoutDataMenuControl) -> T): T {
        return addControl(marginX, marginY, marginX, marginY, width, height,
                marginHoriX, marginHoriY, marginHoriX, marginHoriY, widthHori,
                heightHori, child)
    }

    fun <T : GuiComponent> addControl(marginStartX: Double,
                                      marginStartY: Double,
                                      marginEndX: Double,
                                      marginEndY: Double,
                                      width: Double,
                                      height: Double,
                                      marginHoriStartX: Double,
                                      marginHoriStartY: Double,
                                      marginHoriEndX: Double,
                                      marginHoriEndY: Double,
                                      widthHori: Double,
                                      heightHori: Double,
                                      child: (GuiLayoutDataMenuControl) -> T): T {
        return addControl(marginStartX, marginStartY, marginEndX,
                marginEndY, width, height, marginHoriStartX, marginHoriStartY,
                marginHoriEndX, marginHoriEndY, widthHori, heightHori, 0, child)
    }

    fun <T : GuiComponent> addControl(marginStartX: Double,
                                      marginStartY: Double,
                                      marginEndX: Double,
                                      marginEndY: Double,
                                      width: Double,
                                      height: Double,
                                      marginHoriStartX: Double,
                                      marginHoriStartY: Double,
                                      marginHoriEndX: Double,
                                      marginHoriEndY: Double,
                                      widthHori: Double,
                                      heightHori: Double,
                                      priority: Long,
                                      child: (GuiLayoutDataMenuControl) -> T): T {
        return addControl(Vector2d(marginStartX, marginStartY),
                Vector2d(marginEndX, marginEndY),
                Vector2d(width, height),
                Vector2d(marginHoriStartX, marginHoriStartY),
                Vector2d(marginHoriEndX, marginHoriEndY),
                Vector2d(widthHori, heightHori), priority, child)
    }

    fun <T : GuiComponent> addControl(marginStart: Vector2d,
                                      marginEnd: Vector2d,
                                      size: Vector2d,
                                      marginHoriStart: Vector2d,
                                      marginHoriEnd: Vector2d,
                                      sizeHori: Vector2d,
                                      priority: Long,
                                      child: (GuiLayoutDataMenuControl) -> T): T {
        val layoutData = GuiLayoutDataMenuControl(this, marginStart, marginEnd,
                size, marginHoriStart, marginHoriEnd, sizeHori, priority)
        val component = child(layoutData)
        append(component)
        return component
    }

    override fun updateMesh(renderer: GuiRenderer,
                            size: Vector2d) {
        gui.style.pane(renderer, size)
    }

    override fun newLayoutManager(size: Vector2d): GuiLayoutManager {
        return GuiLayoutManagerMenu(Vector2d.ZERO, size, components,
                size.y >= compactHeight)
    }
}
