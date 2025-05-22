package com.example.schach1337

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class ArrowOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val analysisPaint = Paint().apply {
        color = Color.GREEN
        alpha = 100
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val movePaint = Paint().apply {
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

    private val hintPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 10f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }
    private var hintArrow: Path? = null

    private val analysisArrowPaths = mutableListOf<Path>()
    private val arrowNumbers = mutableListOf<Pair<Float, Float>>()
    private val numbers = mutableListOf<String>()
    private var moveArrowPath: Path? = null
    private var moveArrowPaint: Paint = movePaint

    fun addHintArrow(startX: Float, startY: Float, endX: Float, endY: Float) {
        hintArrow = createArrowPath(startX, startY, endX, endY)
        invalidate()
    }

    fun clearHintArrow() {
        hintArrow = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (path in analysisArrowPaths) {
            canvas.drawPath(path, analysisPaint)
        }

        moveArrowPath?.let { canvas.drawPath(it, moveArrowPaint) }

        for (i in arrowNumbers.indices) {
            val (x, y) = arrowNumbers[i]
            canvas.drawText(numbers[i], x, y, textPaint)
        }

        hintArrow?.let { canvas.drawPath(it, hintPaint) }
    }

    fun addAnalysisArrow(fromX: Float, fromY: Float, toX: Float, toY: Float, number: Int) {
        val path = createArrowPath(fromX, fromY, toX, toY)
        analysisArrowPaths.add(path)
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

    fun addMoveArrow(fromX: Float, fromY: Float, toX: Float, toY: Float, category: MoveCategory?) {
        moveArrowPath = createArrowPath(fromX, fromY, toX, toY)
        moveArrowPaint = Paint().apply {
            alpha = 100
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = when (category) {
                MoveCategory.BEST -> Color.rgb(0, 255, 0)
                MoveCategory.GOOD -> Color.rgb(50, 205, 50)
                MoveCategory.INACCURACY -> Color.BLUE
                MoveCategory.MISTAKE -> Color.YELLOW
                MoveCategory.BLUNDER -> Color.RED
                else -> Color.BLACK
            }
        }
        invalidate()
    }

    private fun createArrowPath(fromX: Float, fromY: Float, toX: Float, toY: Float): Path {
        return Path().apply {
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
    }

    fun clearAnalysisArrows() {
        analysisArrowPaths.clear()
        arrowNumbers.clear()
        numbers.clear()
        invalidate()
    }

    fun clearMoveArrow() {
        moveArrowPath = null
        invalidate()
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
    }
}