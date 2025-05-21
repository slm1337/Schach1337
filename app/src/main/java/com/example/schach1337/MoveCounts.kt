package com.example.schach1337

data class MoveCounts(
    var best: Int,
    var good: Int,
    var inaccuracy: Int,
    var mistake: Int,
    var blunder: Int
)