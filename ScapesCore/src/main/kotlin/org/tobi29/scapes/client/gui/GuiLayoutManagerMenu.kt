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

import org.tobi29.scapes.engine.gui.GuiComponent
import org.tobi29.scapes.engine.gui.GuiLayoutDataAbsolute
import org.tobi29.scapes.engine.gui.GuiLayoutDataFlow
import org.tobi29.scapes.engine.gui.GuiLayoutManager
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2d
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.minus
import org.tobi29.scapes.engine.utils.math.vector.plus
import java.util.*

class GuiLayoutManagerMenu(start: Vector2d,
                           maxSize: Vector2d,
                           components: Set<GuiComponent>,
                           private val vertical: Boolean = false) : GuiLayoutManager(
        start, maxSize, components) {


    override fun layout(output: MutableList<Triple<GuiComponent, Vector2d, Vector2d>>) {
        var unsized = 0.0
        var usedHeight = 0.0
        var unsizedWidth = 0.0
        var usedWidth = 0.0
        var horizontal = false
        var maxHeight = Double.MIN_VALUE
        val widths = ArrayList<Width>()
        for (component in components) {
            if (!component.visible) {
                continue
            }
            val data = component.parent
            if (!vertical && data is GuiLayoutDataMenuControl) {
                val marginStart = data.marginHorizontalStart
                val marginEnd = data.marginHorizontalEnd
                if (data.sizeHorizontal.x < 0.0) {
                    unsizedWidth -= data.sizeHorizontal.x
                } else {
                    usedWidth += data.sizeHorizontal.x + marginStart.x + marginEnd.x
                }
                if (data.sizeHorizontal.y >= 0.0) {
                    maxHeight = max(maxHeight,
                            data.sizeHorizontal.y + marginStart.y + marginEnd.y)
                }
                horizontal = true
            } else {
                if (horizontal) {
                    if (maxHeight < 0.0) {
                        unsized -= maxHeight
                    } else {
                        usedHeight += maxHeight
                    }
                    widths.add(Width(unsizedWidth, usedWidth))
                    maxHeight = Double.MIN_VALUE
                    horizontal = false
                }
                if (data is GuiLayoutDataFlow) {
                    if (data.height() < 0.0) {
                        unsized -= data.height()
                    } else {
                        val marginStart = data.marginStart
                        val marginEnd = data.marginEnd
                        usedHeight += data.height() + marginStart.y + marginEnd.y
                    }
                }
            }
        }
        if (horizontal) {
            if (maxHeight < 0.0) {
                unsized -= maxHeight
            } else {
                usedHeight += maxHeight
            }
            widths.add(Width(unsizedWidth, usedWidth))
            horizontal = false
        }
        assert(!horizontal)
        val pos = MutableVector2d()
        val size = MutableVector2d()
        val offset = MutableVector2d(start)
        val outSize = MutableVector2d()
        val preferredSize = Vector2d(maxSize.x,
                (maxSize.y - usedHeight) / unsized)
        val widthIterator = widths.iterator()
        var preferredSizeHorizontal = Vector2d.ZERO
        for (component in components) {
            if (!component.visible) {
                continue
            }
            val data = component.parent
            pos.set(offset.now())
            size.set(data.width(), data.height())
            if (!vertical && data is GuiLayoutDataMenuControl) {
                size.set(data.sizeHorizontal)
                if (!horizontal) {
                    widthIterator.next().let {
                        preferredSizeHorizontal = Vector2d(
                                (maxSize.x - it.used) / it.unsized,
                                maxSize.y)
                        maxHeight = it.used
                    }
                    horizontal = true
                }
                val marginStart = data.marginHorizontalStart
                val marginEnd = data.marginHorizontalEnd
                size(size, preferredSizeHorizontal.minus(marginStart).minus(
                        marginEnd),
                        maxSize.minus(marginStart).minus(marginEnd))
                pos.plus(marginStart)
                offset.plusX(size.doubleX() + marginStart.x +
                        marginEnd.x)
                setSize(pos.now().plus(size.now()).plus(marginEnd), outSize)
            } else {
                if (horizontal) {
                    pos.plusY(maxHeight)
                    horizontal = false
                }
                if (data is GuiLayoutDataFlow) {
                    val marginStart = data.marginStart
                    val marginEnd = data.marginEnd
                    if (size.doubleX() >= 0.0) {
                        pos.plusX((maxSize.x - size.doubleX() -
                                marginStart.x - marginEnd.x) * 0.5)
                    }
                    size(size,
                            preferredSize.minus(marginStart).minus(marginEnd),
                            maxSize.minus(marginStart).minus(marginEnd))
                    pos.plus(marginStart)
                    offset.plusY(size.doubleY() + marginStart.y +
                            marginEnd.y)
                    setSize(pos.now().plus(size.now()).plus(marginEnd), outSize)
                } else if (data is GuiLayoutDataAbsolute) {
                    pos.set(data.pos())
                    size(size, maxSize, maxSize)
                    setSize(pos.now().plus(size.now()), outSize)
                } else {
                    throw IllegalStateException(
                            "Invalid layout node: " + data::class.java)
                }
            }
            output.add(Triple(component, pos.now(), size.now()))
        }
        this.size = outSize.now()
    }

    private class Width(val unsized: Double,
                        val used: Double)
}
