package org.tobi29.scapes.engine.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import org.tobi29.scapes.engine.gui.GlyphRenderer
import org.tobi29.scapes.engine.utils.math.round
import java.nio.ByteBuffer

class AndroidGlyphRenderer(private val typeface: Typeface, size: Int) : GlyphRenderer {
    private val tiles: Int
    private val pageTiles: Int
    private val pageTileBits: Int
    private val pageTileMask: Int
    private val glyphSize: Int
    private val imageSize: Int
    private val renderX: Int
    private val renderY: Int
    private val tileSize: Float
    private val size: Float

    init {
        this.size = size.toFloat()
        val tileBits = 2
        tiles = 1 shl tileBits
        pageTileBits = tileBits shl 1
        pageTileMask = (1 shl pageTileBits) - 1
        pageTiles = 1 shl pageTileBits
        tileSize = 1.0f / tiles
        glyphSize = size shl 1
        imageSize = glyphSize shl tileBits
        renderX = round(size * 0.25)
        renderY = round(size * 1.5)
    }

    override fun page(id: Int,
                      bufferSupplier: (Int) -> ByteBuffer): GlyphRenderer.GlyphPage {
        val width = FloatArray(pageTiles)
        val bitmap = Bitmap.createBitmap(imageSize, imageSize,
                Bitmap.Config.ARGB_8888)
        bitmap.density = 96
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFFFFFFFF.toInt()
        paint.typeface = typeface
        paint.textSize = size * 1.4f
        var i = 0
        val offset = id shl pageTileBits
        val singleWidth = FloatArray(1)
        for (y in 0..tiles - 1) {
            val yy = y * glyphSize + renderY
            for (x in 0..tiles - 1) {
                val xx = x * glyphSize + renderX
                val c = (i + offset).toChar()
                val str = String(charArrayOf(c))
                canvas.drawText(str, xx.toFloat(), yy.toFloat(), paint)
                paint.getTextWidths(str, singleWidth)
                width[i++] = singleWidth[0] * 0.75f / size
            }
        }
        val buffer = bufferSupplier(imageSize * imageSize shl 2)
        val pixels = IntArray(imageSize * imageSize)
        bitmap.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
        i = 0
        for (y in 0..imageSize - 1) {
            for (x in 0..imageSize - 1) {
                buffer.put(WHITE)
                buffer.put((pixels[i++] ushr 24).toByte())
            }
        }
        buffer.rewind()
        return GlyphRenderer.GlyphPage(buffer, width, imageSize, tiles,
                tileSize)
    }

    override fun pageID(character: Char): Int {
        return character.toInt() shr pageTileBits
    }

    override fun pageCode(character: Char): Int {
        return character.toInt() and pageTileMask
    }

    companion object {
        private val WHITE = byteArrayOf(0xFF.toByte(), 0xFF.toByte(),
                0xFF.toByte())
    }
}
