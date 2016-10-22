package org.tobi29.scapes.engine.android

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import org.tobi29.scapes.engine.input.ControllerTouch
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import java.util.concurrent.ConcurrentHashMap

class ScapesEngineView(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(
        context, attrs) {
    val fingers: MutableMap<Int, ControllerTouch.Tracker> = ConcurrentHashMap()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setEGLContextClientVersion(3)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        super.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = e.actionIndex
                val tracker = ControllerTouch.Tracker()
                tracker.pos.set(e.getX(index), e.getY(index))
                val id = e.getPointerId(index)
                fingers.put(id, tracker)
            }
            MotionEvent.ACTION_MOVE -> for ((key, value1) in fingers) {
                val index = e.findPointerIndex(key)
                val value = Vector2d(e.getX(index).toDouble(),
                        e.getY(index).toDouble())
                value1.pos.set(value)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id = e.getPointerId(e.actionIndex)
                fingers.remove(id)
            }
        }
        return true
    }
}
