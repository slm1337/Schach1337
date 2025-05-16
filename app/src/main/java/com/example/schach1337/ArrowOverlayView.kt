package com.example.schach1337

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class ArrowOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.BLACK
        alpha = 100
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 36f
        isAntiAlias = true
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val arrowPaths = mutableListOf<Path>()
    private val arrowNumbers = mutableListOf<Pair<Float, Float>>()
    private val numbers = mutableListOf<String>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (path in arrowPaths) {
            canvas.drawPath(path, paint)
        }

        for (i in arrowNumbers.indices) {
            val (x, y) = arrowNumbers[i]
            canvas.drawText(numbers[i], x, y, textPaint)
        }
    }

    fun addArrow(fromX: Float, fromY: Float, toX: Float, toY: Float, number: Int) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)

            val angle = Math.atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
            val arrowLength = 30
            val arrowAngle = Math.toRadians(30.0)

            val x1 = toX - arrowLength * Math.cos(angle - arrowAngle).toFloat()
            val y1 = toY - arrowLength * Math.sin(angle - arrowAngle).toFloat()
            val x2 = toX - arrowLength * Math.cos(angle + arrowAngle).toFloat()
            val y2 = toY - arrowLength * Math.sin(angle + arrowAngle).toFloat()

            moveTo(toX, toY)
            lineTo(x1, y1)
            moveTo(toX, toY)
            lineTo(x2, y2)
        }

        arrowPaths.add(path)
        var textX = toX
        var textY = toY - 30f

        val offsetStep = 40f
        var offsetMultiplier = 0
        while (arrowNumbers.any { (x, y) -> distance(x, y, textX, textY) < 25f }) {
            offsetMultiplier++
            textX = toX + offsetMultiplier * offsetStep
            textY = toY - 30f + offsetMultiplier * 10f
        }

        arrowNumbers.add(Pair(textX, textY))
        numbers.add(number.toString())
        invalidate()
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
    }

    fun clearArrows() {
        arrowPaths.clear()
        arrowNumbers.clear()
        numbers.clear()
        invalidate()
    }
}