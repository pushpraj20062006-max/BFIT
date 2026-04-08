package com.example.bfit.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * A premium animated ring/donut chart for visualizing macro breakdown.
 * Displays calorie progress as a thick arc with animated sweep,
 * with the numeric value in the center.
 */
class MacroRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Segment(
        val label: String,
        val value: Float,
        val color: Int
    )

    private var segments: List<Segment> = emptyList()
    private var centerText: String = ""
    private var subText: String = ""
    private var animProgress: Float = 0f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#10000000")
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#333333")
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#888888")
    }

    fun setData(segs: List<Segment>, center: String, sub: String) {
        segments = segs
        centerText = center
        subText = sub
        animProgress = 0f
        startAnimation()
        invalidate()
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 900
        animator.interpolator = DecelerateInterpolator(2f)
        animator.addUpdateListener {
            animProgress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = (180 * resources.displayMetrics.density).toInt()
        val w = resolveSize(desiredSize, widthMeasureSpec)
        val h = resolveSize(desiredSize, heightMeasureSpec)
        val size = minOf(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density
        val strokeWidth = 18f * density
        val padding = strokeWidth / 2f + 8f * density

        trackPaint.strokeWidth = strokeWidth
        arcPaint.strokeWidth = strokeWidth

        val rect = RectF(padding, padding, width - padding, height - padding)

        // Draw background track
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        // Draw segments
        val total = segments.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0) return

        var startAngle = -90f
        for (seg in segments) {
            val sweepAngle = (seg.value / total) * 360f * animProgress
            arcPaint.color = seg.color
            canvas.drawArc(rect, startAngle, sweepAngle, false, arcPaint)
            startAngle += sweepAngle
        }

        // Center text
        centerTextPaint.textSize = 20f * density
        centerTextPaint.alpha = (255 * animProgress).toInt()
        canvas.drawText(centerText, width / 2f, height / 2f + 4f * density, centerTextPaint)

        subTextPaint.textSize = 11f * density
        subTextPaint.alpha = (255 * animProgress).toInt()
        canvas.drawText(subText, width / 2f, height / 2f + 20f * density, subTextPaint)
    }
}
