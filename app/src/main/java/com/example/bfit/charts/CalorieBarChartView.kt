package com.example.bfit.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * A premium animated bar chart view for displaying daily calorie intake.
 * Features: gradient bars, rounded corners, target line, value labels, day labels, animated entry.
 */
class CalorieBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class BarData(
        val label: String,
        val value: Int,
        val isToday: Boolean = false,
        val isComplete: Boolean = false
    )

    private var data: List<BarData> = emptyList()
    private var targetValue: Int = 2000
    private var animProgress: Float = 0f

    // Paints
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#888888")
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#666666")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val todayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A000000")
        strokeWidth = 1f
    }
    private val targetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.parseColor("#FF5252")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Colors
    private val colorOnTarget = intArrayOf(Color.parseColor("#00E676"), Color.parseColor("#00C853"))
    private val colorOver = intArrayOf(Color.parseColor("#FFB74D"), Color.parseColor("#FF9800"))
    private val colorUnder = intArrayOf(Color.parseColor("#FF8A80"), Color.parseColor("#FF5252"))
    private val colorEmpty = intArrayOf(Color.parseColor("#E0E0E0"), Color.parseColor("#BDBDBD"))
    private val colorToday = Color.parseColor("#6C3CE0")

    fun setData(bars: List<BarData>, target: Int) {
        data = bars
        targetValue = target
        animProgress = 0f
        startAnimation()
        invalidate()
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 800
        animator.interpolator = DecelerateInterpolator(2f)
        animator.addUpdateListener {
            animProgress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (280 * resources.displayMetrics.density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val density = resources.displayMetrics.density
        val paddingLeft = 16f * density
        val paddingRight = 16f * density
        val paddingTop = 36f * density
        val paddingBottom = 56f * density

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val maxVal = maxOf(data.maxOfOrNull { it.value } ?: 1, targetValue, 1)
        val barWidth = chartWidth / data.size
        val barInnerWidth = barWidth * 0.55f
        val cornerRadius = 10f * density

        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = paddingTop + (chartHeight * i / 4f)
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
        }

        // Draw target dashed line
        val targetY = paddingTop + chartHeight * (1f - targetValue.toFloat() / maxVal)
        canvas.drawLine(paddingLeft, targetY, width - paddingRight, targetY, targetLinePaint)
        canvas.drawText("Target", width - paddingRight - 10f * density, targetY - 6f * density, targetTextPaint)

        // Draw bars
        for (i in data.indices) {
            val bar = data[i]
            val cx = paddingLeft + barWidth * i + barWidth / 2f
            val barLeft = cx - barInnerWidth / 2f
            val barRight = cx + barInnerWidth / 2f

            val ratio = if (maxVal > 0) bar.value.toFloat() / maxVal else 0f
            val animatedRatio = ratio * animProgress
            val barTop = paddingTop + chartHeight * (1f - animatedRatio)
            val barBottom = paddingTop + chartHeight

            // Choose gradient colors
            val gradColors = when {
                bar.value == 0 -> colorEmpty
                bar.value > targetValue * 1.1 -> colorOver
                bar.value >= targetValue * 0.8 -> colorOnTarget
                else -> colorUnder
            }

            // Create gradient for each bar
            barPaint.shader = LinearGradient(
                barLeft, barTop, barLeft, barBottom,
                gradColors[0], gradColors[1],
                Shader.TileMode.CLAMP
            )

            // Draw rounded rect bar
            val rect = RectF(barLeft, barTop, barRight, barBottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)

            // Draw value label above bar
            if (bar.value > 0) {
                valuePaint.color = if (bar.isToday) colorToday else Color.parseColor("#555555")
                canvas.drawText("${bar.value}", cx, barTop - 8f * density, valuePaint)
            }

            // Draw day label
            if (bar.isToday) {
                todayLabelPaint.color = colorToday
                canvas.drawText(bar.label, cx, height - 12f * density, todayLabelPaint)
            } else {
                canvas.drawText(bar.label, cx, height - 12f * density, labelPaint)
            }

            // Draw completion dot
            if (bar.isComplete) {
                dotPaint.color = colorToday
                canvas.drawCircle(cx, height - 36f * density, 4f * density, dotPaint)
            }
        }
    }
}
