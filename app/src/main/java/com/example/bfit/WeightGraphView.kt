package com.example.bfit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.bfit.database.WeightLogEntry

class WeightGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D9488")
        strokeWidth = 7f
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F766E")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#374151")
        textSize = 30f
    }

    private var points: List<WeightLogEntry> = emptyList()

    fun setData(entries: List<WeightLogEntry>) {
        points = entries.sortedBy { it.date }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val leftPadding = 72f
        val rightPadding = 32f
        val topPadding = 40f
        val bottomPadding = 58f

        val chartLeft = leftPadding
        val chartTop = topPadding
        val chartRight = width - rightPadding
        val chartBottom = height - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

        if (points.isEmpty() || points.size == 1 || chartWidth <= 0f || chartHeight <= 0f) {
            canvas.drawText("Add at least 2 weight logs", chartLeft, chartTop + 40f, labelPaint)
            return
        }

        val minWeight = points.minOf { it.weightKg }
        val maxWeight = points.maxOf { it.weightKg }
        val range = (maxWeight - minWeight).takeIf { it > 0f } ?: 1f

        val stepX = chartWidth / (points.size - 1)

        for (i in 0 until points.lastIndex) {
            val current = points[i]
            val next = points[i + 1]

            val x1 = chartLeft + i * stepX
            val x2 = chartLeft + (i + 1) * stepX
            val y1 = chartBottom - ((current.weightKg - minWeight) / range) * chartHeight
            val y2 = chartBottom - ((next.weightKg - minWeight) / range) * chartHeight

            canvas.drawLine(x1, y1, x2, y2, linePaint)
            canvas.drawCircle(x1, y1, 7f, pointPaint)
            if (i == points.lastIndex - 1) {
                canvas.drawCircle(x2, y2, 7f, pointPaint)
            }
        }

        canvas.drawText("${"%.1f".format(maxWeight)} kg", chartLeft + 8f, chartTop + 26f, labelPaint)
        canvas.drawText("${"%.1f".format(minWeight)} kg", chartLeft + 8f, chartBottom - 8f, labelPaint)
    }
}
