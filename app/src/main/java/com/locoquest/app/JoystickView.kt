package com.locoquest.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var xPercent: Float = 0f
    var yPercent: Float = 0f

    private val outerCirclePaint: Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
    }

    private val innerCirclePaint: Paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private var outerCircleRadius: Float = 0f
    private var innerCircleRadius: Float = 0f
    private var outerCircleCenter: PointF = PointF()
    private var innerCircleCenter: PointF = PointF()

    private var onJoystickMoveListener: OnJoystickMoveListener? = null

    init {
        setWillNotDraw(false)
    }

    interface OnJoystickMoveListener {
        fun onJoystickMove(xPercent: Float, yPercent: Float)
        fun onJoystickRelease()
    }

    fun setOnJoystickMoveListener(listener: OnJoystickMoveListener?) {
        onJoystickMoveListener = listener
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val centerX = width / 2f
        val centerY = height / 2f
        outerCircleRadius = width.coerceAtMost(height) / 2f
        innerCircleRadius = outerCircleRadius / 2f
        outerCircleCenter.set(centerX, centerY)
        innerCircleCenter.set(centerX, centerY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(outerCircleCenter.x, outerCircleCenter.y, outerCircleRadius, outerCirclePaint)
        canvas.drawCircle(innerCircleCenter.x, innerCircleCenter.y, innerCircleRadius, innerCirclePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x
                val touchY = event.y
                val displacement = calculateDisplacement(touchX, touchY)

                if (displacement <= outerCircleRadius) {
                    innerCircleCenter.set(touchX, touchY)
                } else {
                    val dx = touchX - outerCircleCenter.x
                    val dy = touchY - outerCircleCenter.y
                    val angle = atan2(dy.toDouble(), dx.toDouble())
                    val constrainedX = (outerCircleCenter.x + outerCircleRadius * cos(angle)).toFloat()
                    val constrainedY = (outerCircleCenter.y + outerCircleRadius * sin(angle)).toFloat()
                    innerCircleCenter.set(constrainedX, constrainedY)
                }

                invalidate()

                val xPercent = (innerCircleCenter.x - outerCircleCenter.x) / outerCircleRadius
                val yPercent = ((innerCircleCenter.y - outerCircleCenter.y) / outerCircleRadius) * -1
                this.xPercent = xPercent
                this.yPercent = yPercent
                onJoystickMoveListener?.onJoystickMove(xPercent, yPercent)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                innerCircleCenter.set(outerCircleCenter.x, outerCircleCenter.y)
                invalidate()
                onJoystickMoveListener?.onJoystickRelease()
            }
        }
        return true
    }

    private fun calculateDisplacement(x: Float, y: Float): Float {
        val dx = x - outerCircleCenter.x
        val dy = y - outerCircleCenter.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}