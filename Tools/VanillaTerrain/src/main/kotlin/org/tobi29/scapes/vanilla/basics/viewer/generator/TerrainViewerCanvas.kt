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

package org.tobi29.scapes.vanilla.basics.viewer.generator

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.PaletteData
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Composite
import org.tobi29.scapes.engine.swt.util.framework.Application
import org.tobi29.scapes.engine.utils.graphics.hsvToRGB
import org.tobi29.scapes.engine.utils.math.clamp
import org.tobi29.scapes.engine.utils.math.max
import org.tobi29.scapes.engine.utils.math.min
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.math.vector.Vector2i
import org.tobi29.scapes.engine.utils.math.vector.distanceSqr
import org.tobi29.scapes.engine.utils.task.TaskExecutor
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class TerrainViewerCanvas(parent: Composite,
                          style: Int,
                          private val application: Application,
                          private val colorSupplier: () -> TerrainViewerCanvas.ColorSupplier,
                          scale: Int) : Canvas(parent, style) {
    private val taskExecutor: TaskExecutor
    private val chunks = ConcurrentHashMap<Vector2i, Image>()
    private val emptyImage: Image
    private val cache = AtomicLong()
    private val renders = AtomicInteger()
    private val drawQueued = AtomicBoolean()
    private var scale = 0.0
    private var cx = 0
    private var cy = 0
    private var dragX = 0
    private var dragY = 0
    private var tx = 0
    private var ty = 0
    private var dragging = false

    init {
        this.scale = scale.toDouble()
        taskExecutor = application.taskExecutor
        val data = ByteArray(3 shl CHUNK_BITS shl CHUNK_BITS)
        Arrays.fill(data, 0x44.toByte())
        val palette = PaletteData(0xFF0000, 0xFF00, 0xFF)
        val imageData = ImageData(CHUNK_SIZE, CHUNK_SIZE, 24, palette, 1, data)
        emptyImage = Image(display, imageData)
        listeners()
        addDisposeListener {
            wipeCache()
            emptyImage.dispose()
        }
    }

    private fun chunk(pos: Vector2i): Image {
        checkWidget()
        chunks[pos]?.let { return it }
        chunks.put(pos, emptyImage)
        render(pos)
        return emptyImage
    }

    private fun render(pos: Vector2i) {
        val scale = this.scale
        val cache = this.cache.get()
        renders.incrementAndGet()
        taskExecutor.runThread({
            if (cache == this.cache.get()) {
                val data = ByteArray(3 shl CHUNK_BITS shl CHUNK_BITS)
                val output = Output()
                val supplier = colorSupplier()
                val minx = pos.x shl CHUNK_BITS
                val miny = pos.y shl CHUNK_BITS
                val maxx = minx + CHUNK_SIZE
                val maxy = miny + CHUNK_SIZE
                var i = 0
                for (y in miny..maxy - 1) {
                    val yy = y * scale
                    for (x in minx..maxx - 1) {
                        val xx = x * scale
                        supplier.color(xx, yy, output)
                        val h = clamp(output.h, 0.0, 1.0)
                        val s = clamp(output.s, 0.0, 1.0)
                        val v = clamp(output.v, 0.0, 1.0)
                        val rgb = hsvToRGB(h, s, v)
                        data[i++] = (rgb.x * 255.0).toByte()
                        data[i++] = (rgb.y * 255.0).toByte()
                        data[i++] = (rgb.z * 255.0).toByte()
                    }
                }
                application.accessAsync {
                    if (cache == this.cache.get()) {
                        putImage(pos, image(data))
                    }
                }
            }
            renders.decrementAndGet()
        }, "Render-Tile")
    }

    private fun image(data: ByteArray): Image {
        val palette = PaletteData(0xFF0000, 0xFF00, 0xFF)
        val imageData = ImageData(CHUNK_SIZE, CHUNK_SIZE, 24, palette, 1, data)
        return Image(display, imageData)
    }

    private fun putImage(pos: Vector2i,
                         image: Image) {
        val oldImage = chunks.remove(pos)
        oldImage?.let {
            if (it != emptyImage) {
                it.dispose()
            }
        }
        chunks[pos] = image
        queueDraw()
    }

    fun render() {
        checkWidget()
        cache.incrementAndGet()
        chunks.keys.forEach { render(it) }
    }

    fun wipeCache() {
        checkWidget()
        cache.incrementAndGet()
        chunks.values.asSequence().filter { it != emptyImage }.forEach(
                Image::dispose)
        chunks.clear()
    }

    val isRendering: Boolean
        get() = renders.get() > 0

    private fun queueDraw() {
        if (!drawQueued.getAndSet(true)) {
            taskExecutor.addTaskOnce({ redraw() }, "Viewer-Redraw", 100)
        }
    }

    private fun listeners() {
        val colorSupplier = this.colorSupplier()
        addPaintListener { event ->
            drawQueued.set(false)
            val size = size
            val cx = round(this.cx / scale)
            val cy = round(this.cy / scale)
            val ccx = cx shr CHUNK_BITS
            val ccy = cy shr CHUNK_BITS
            val width = (size.x - 1 shr CHUNK_BITS) + ccx + 1
            val height = (size.y - 1 shr CHUNK_BITS) + ccy + 1
            for (y in ccy..height) {
                val yy = y * CHUNK_SIZE
                for (x in ccx..width) {
                    val xx = x * CHUNK_SIZE
                    val image = chunk(Vector2i(x, y))
                    event.gc.drawImage(image, xx - cx, yy - cy)
                }
            }
            val iterator = chunks.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val image = entry.value
                if (image == emptyImage) {
                    // Wait for it to finish rendering and remove on a later
                    // pass as it will otherwise be added back anyways
                    continue
                }
                val pos = entry.key
                val x = pos.x
                val y = pos.y
                if (x < ccx || y < ccy || x > width || y > height) {
                    if (image != emptyImage) {
                        image.dispose()
                    }
                    iterator.remove()
                }
            }
        }
        addListener(SWT.MouseDown) { e ->
            dragX = e.x
            dragY = e.y
            dragging = true
        }
        addListener(SWT.MouseUp) { e -> dragging = false }
        addListener(SWT.MouseMove) { e ->
            if (dragging) {
                cx += ((dragX.toDouble() - e.x) * scale).toInt()
                cy += ((dragY.toDouble() - e.y) * scale).toInt()
                dragX = e.x
                dragY = e.y
                redraw()
            } else {
                colorSupplier.tooltip(cx + e.x * scale,
                        cy + e.y * scale)?.let {
                    if (toolTipText == null) {
                        toolTipText = it
                        tx = e.x
                        ty = e.y
                    }
                    if (distanceSqr(tx.toDouble(), ty.toDouble(),
                            e.x.toDouble(), e.y.toDouble()) > 32.0) {
                        toolTipText = null
                    }
                }
            }
        }
        addListener(SWT.MouseWheel) { e ->
            if (e.count > 0) {
                scale = max(scale * 0.8, 1.0)
            } else {
                scale = min(scale * 1.25, 1024.0)
            }
            wipeCache()
            redraw()
        }
    }

    override fun checkSubclass() {
    }

    interface ColorSupplier {
        fun color(x: Double,
                  y: Double,
                  o: Output)

        fun tooltip(x: Double,
                    y: Double): String? {
            return null
        }
    }

    class Output {
        var h = 0.0
        var s = 0.0
        var v = 0.0
    }

    companion object {
        private val CHUNK_BITS = 8
        private val CHUNK_SIZE = 1 shl CHUNK_BITS
    }
}
