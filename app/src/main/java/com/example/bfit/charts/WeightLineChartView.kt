package com.example.bfit.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * A premium animated line chart for displaying weight trend over time.
 * Features: smooth bezier curve, gradient area fill, data point dots with glow,
 * horizontal grid lines with values, date labels, animated drawing.
 */
class WeightLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class WeightPoint(
        val label: String,
        val weight: Float,
        val bmi: Float = 0f
    )

    private var points: List<WeightPoint> = emptyList()
    private var animProgress: Float = 0f

    private val lineColor = Color.parseColor("#6C3CE0")
    private val areaColorTop = Color.parseColor("#446C3CE0")
    private val areaColorBottom = Color.parseColor("#006C3CE0")
    private val dotColor = Color.parseColor("#6C3CE0")
    private val dotGlowColor = Color.parseColor("#336C3CE0")

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotColor
        style = Paint.Style.FILL
    }
    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotGlowColor
        style = Paint.Style.FILL
    }
    private val dotWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#888888")
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        textAlign = Paint.Align.CENTER
        color = lineColor
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#12000000")
        strokeWidth = 1f
    }
    private val gridValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        textAlign = Paint.Align.LEFT
        color = Color.parseColor("#AAAAAA")
    }
    private val dashLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20000000")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }

    fun setData(weightPoints: List<WeightPoint>) {
        points = weightPoints
        animProgress = 0f
        startAnimation()
        invalidate()
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator(1.5f)
        animator.addUpdateListener {
            animProgress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (220 * resources.displayMetrics.density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val density = resources.displayMetrics.density
        val paddingLeft = 42f * density
        val paddingRight = 24f * density
        val paddingTop = 32f * density
        val paddingBottom = 52f * density

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val weights = points.map { it.weight }
        val minW = weights.min() - 1f
        val maxW = weights.max() + 1f
        val range = maxW - minW

        // Draw horizontal grid lines and values
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + (chartHeight * i / gridLines)
            val gridValue = maxW - (range * i / gridLines)
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            canvas.drawText(String.format("%.0f", gridValue), 8f * density, y + 8f, gridValuePaint)
        }

        // Calculate point positions
        val pointXs = FloatArray(points.size)
        val pointYs = FloatArray(points.size)
        val step = chartWidth / (points.size - 1).coerceAtLeast(1)

        for (i in points.indices) {
            pointXs[i] = paddingLeft + step * i
            val normalized = (points[i].weight - minW) / range
            pointYs[i] = paddingTop + chartHeight * (1f - normalized)
        }

        // Animate Y positions (slide up from bottom)
        val animYs = FloatArray(points.size) { i ->
            val bottomY = paddingTop + chartHeight
            bottomY + (pointYs[i] - bottomY) * animProgress
        }

        // Draw area fill with gradient
        val areaPath = Path()
        areaPath.moveTo(pointXs[0], paddingTop + chartHeight)
        for (i in points.indices) {
            areaPath.lineTo(pointXs[i], animYs[i])
        }
        areaPath.lineTo(pointXs.last(), paddingTop + chartHeight)
        areaPath.close()

        areaPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + chartHeight,
            areaColorTop, areaColorBottom, Shader.TileMode.CLAMP
        )
        canvas.drawPath(areaPath, areaPaint)

        // Draw connecting lines
        val linePath = Path()
        linePath.moveTo(pointXs[0], animYs[0])
        for (i in 1 until points.size) {
            // Use bezier for smooth curve
            val prevX = pointXs[i - 1]
            val prevY = animYs[i - 1]
            val currX = pointXs[i]
            val currY = animYs[i]
            val midX = (prevX + currX) / 2f
            linePath.cubicTo(midX, prevY, midX, currY, currX, currY)
        }
        canvas.drawPath(linePath, linePaint)

        // Draw dots with glow
        for (i in points.indices) {
            // Vertical dashed guide
            canvas.drawLine(pointXs[i], animYs[i], pointXs[i], paddingTop + chartHeight, dashLinePaint)

            // Glow
            canvas.drawCircle(pointXs[i], animYs[i], 14f * density * animProgress, dotGlowPaint)
            // White outer
            canvas.drawCircle(pointXs[i], animYs[i], 8f * density * animProgress, dotWhitePaint)
            // Colored inner
            canvas.drawCircle(pointXs[i], animYs[i], 5f * density * animProgress, dotPaint)

            // Weight value above dot
            valuePaint.alpha = (255 * animProgress).toInt()
            canvas.drawText(
                String.format("%.1f", points[i].weight),
                pointXs[i],
                animYs[i] - 14f * density,
                valuePaint
            )

            // Date label below
            labelPaint.alpha = (255 * animProgress).toInt()
            canvas.drawText(points[i].label, pointXs[i], height - 12f * density, labelPaint)
        }
    }
}
