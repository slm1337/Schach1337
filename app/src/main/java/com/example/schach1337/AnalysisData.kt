package com.example.schach1337

data class AnalysisData(
    val evaluations: List<Float>,
    val whiteCounts: MoveCounts,
    val blackCounts: MoveCounts,
    val whiteAccuracy: Float,
    val blackAccuracy: Float
)
