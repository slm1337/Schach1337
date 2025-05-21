package com.example.schach1337

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate


class ActivityAnalysisResults : AppCompatActivity() {
    private lateinit var evalChart: LineChart
    private val evalEntries = mutableListOf<Entry>()
    private var moveNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_results)

        evalChart = findViewById(R.id.evalChart)
        setupChart()

        @Suppress("UNCHECKED_CAST")
        val evaluations = intent.getSerializableExtra("evaluations") as? ArrayList<Float> ?: emptyList()
        val whiteBestCount = intent.getIntExtra("whiteBestCount", 0)
        val whiteGoodCount = intent.getIntExtra("whiteGoodCount", 0)
        val whiteInaccuracyCount = intent.getIntExtra("whiteInaccuracyCount", 0)
        val whiteMistakeCount = intent.getIntExtra("whiteMistakeCount", 0)
        val whiteBlunderCount = intent.getIntExtra("whiteBlunderCount", 0)
        val blackBestCount = intent.getIntExtra("blackBestCount", 0)
        val blackGoodCount = intent.getIntExtra("blackGoodCount", 0)
        val blackInaccuracyCount = intent.getIntExtra("blackInaccuracyCount", 0)
        val blackMistakeCount = intent.getIntExtra("blackMistakeCount", 0)
        val blackBlunderCount = intent.getIntExtra("blackBlunderCount", 0)
        val whiteAccuracy = intent.getFloatExtra("whiteAccuracy", 0f)
        val blackAccuracy = intent.getFloatExtra("blackAccuracy", 0f)


        evaluations.forEach { eval ->
            updateChartWithEval(eval)
        }

        findViewById<TextView>(R.id.whiteBestCount).text = whiteBestCount.toString()
        findViewById<TextView>(R.id.whiteGoodCount).text = whiteGoodCount.toString()
        findViewById<TextView>(R.id.whiteInaccuracyCount).text = whiteInaccuracyCount.toString()
        findViewById<TextView>(R.id.whiteMistakeCount).text = whiteMistakeCount.toString()
        findViewById<TextView>(R.id.whiteBlunderCount).text = whiteBlunderCount.toString()
        findViewById<TextView>(R.id.blackBestCount).text = blackBestCount.toString()
        findViewById<TextView>(R.id.blackGoodCount).text = blackGoodCount.toString()
        findViewById<TextView>(R.id.blackInaccuracyCount).text = blackInaccuracyCount.toString()
        findViewById<TextView>(R.id.blackMistakeCount).text = blackMistakeCount.toString()
        findViewById<TextView>(R.id.blackBlunderCount).text = blackBlunderCount.toString()
        findViewById<TextView>(R.id.whiteAccuracy).text = String.format("%.1f%%", whiteAccuracy)
        findViewById<TextView>(R.id.blackAccuracy).text = String.format("%.1f%%", blackAccuracy)
    }

    private fun setupChart() {
        evalChart.description.isEnabled = false
        evalChart.setTouchEnabled(true)
        evalChart.isDragEnabled = true
        evalChart.setScaleEnabled(true)
        evalChart.setPinchZoom(true)

        evalChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter()
            granularity = 1f
            setDrawGridLines(false)
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        }

        evalChart.axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = -10f
            axisMaximum = 10f
        }
        evalChart.axisRight.isEnabled = false

        val dataSet = LineDataSet(evalEntries, "Evaluation")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[0]
        dataSet.setDrawCircles(true)
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = ColorTemplate.MATERIAL_COLORS[0]
        dataSet.fillAlpha = 50

        evalChart.data = LineData(dataSet)
        evalChart.invalidate()
    }

    private fun updateChartWithEval(eval: Float) {
        moveNumber++
        evalEntries.add(Entry(moveNumber.toFloat(), eval))

        val dataSet = evalChart.data.getDataSetByIndex(0) as LineDataSet
        dataSet.values = evalEntries
        evalChart.data.notifyDataChanged()
        evalChart.notifyDataSetChanged()
        evalChart.invalidate()

        if (moveNumber > 10) {
            evalChart.moveViewToX(moveNumber.toFloat())
        }
    }
}