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
package org.tobi29.scapes.tools.controlpanel.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label

class ControlPanelGraph(parent: Composite,
                        max: Double,
                        scale: Double,
                        private val borderNameFilter: (Double) -> String) : Composite(
        parent, SWT.NONE) {
    val canvas: Canvas
    private val label: Label
    private var i: Int = 0
    private var data = DoubleArray(0)

    init {
        layout = GridLayout(1, false)

        label = Label(this, SWT.NONE)
        label.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1)

        canvas = Canvas(this, SWT.BORDER)
        canvas.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)
        canvas.addListener(SWT.Resize) { event ->
            val size = canvas.size
            if (data.size != size.x) {
                i = 0
                data = DoubleArray(size.x)
            }
        }
        val display = display
        val colorGreen = display.getSystemColor(SWT.COLOR_GREEN)
        val colorYellow = display.getSystemColor(SWT.COLOR_YELLOW)
        val colorRed = display.getSystemColor(SWT.COLOR_RED)
        canvas.addPaintListener { event ->
            val size = canvas.size
            for (i in 1 until data.size) {
                var x = i - this.i
                if (x < 1) {
                    x += data.size
                }
                val percentageLow = data[i - 1] / max
                val percentageHigh = data[i] / max
                if (percentageLow < 1.0 && percentageHigh < 1.0) {
                    event.gc.foreground = colorGreen
                } else if (percentageLow < 1.1 && percentageHigh < 1.1) {
                    event.gc.foreground = colorYellow
                } else {
                    event.gc.foreground = colorRed
                }
                val heightLow = (percentageLow * scale * size.y.toDouble()).toInt()
                val heightHigh = (percentageHigh * scale * size.y.toDouble()).toInt()
                event.gc.drawLine(x, size.y - heightLow, x + 1,
                        size.y - heightHigh)
            }
        }
        val size = canvas.size
        data = DoubleArray(size.x)
        label.text = "Loading..."
    }

    fun addStamp(value: Double) {
        if (i < data.size) {
            data[i++] = value
        }
        if (i >= data.size) {
            i = 0
        }
        canvas.redraw()
        label.text = borderNameFilter(value)
    }

    override fun checkSubclass() {
    }
}
