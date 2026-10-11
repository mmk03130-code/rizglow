package com.rsstudio.rizglow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/** A simple vertical slider with reliable touch. Drag up = increase.
 *  Replaces the hacky rotated-SeekBar which had dead touch zones. */
class VerticalSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var max: Int = 100
    var progress: Int = 0
        set(v) {
            field = v.coerceIn(0, max)
            invalidate()
        }
    var onChange: ((Int) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x408A7AB8.toInt()
        style = Paint.Style.FILL
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9B6BD3.toInt()
        style = Paint.Style.FILL
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7A4FB5.toInt()
        style = Paint.Style.FILL
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)
                val h = height.toFloat()
                // Top = max, bottom = 0.
                val p = ((h - e.y) / h * max).toInt().coerceIn(0, max)
                if (p != progress) {
                    progress = p
                    onChange?.invoke(p)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(e)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val trackW = dp(8).toFloat()
        val radius = dp(12).toFloat()

        // Track (full height).
        canvas.drawRoundRect(
            cx - trackW / 2, 0f, cx + trackW / 2, h,
            radius, radius, trackPaint
        )
        // Fill (from bottom up to progress).
        val fillTop = h - (progress.toFloat() / max) * h
        if (fillTop < h) {
            canvas.drawRoundRect(
                cx - trackW / 2, fillTop, cx + trackW / 2, h,
                radius, radius, fillPaint
            )
        }
        // Thumb at progress position.
        val thumbR = dp(14).toFloat()
        canvas.drawCircle(cx, fillTop.coerceIn(thumbR, h - thumbR), thumbR, thumbPaint)
    }
}
