package com.example.schach1337

data class MoveAnalysis(
    val moveNumber: Int,
    val move: String,
    val fenBefore: String,
    val fenAfter: String,
    val evalBefore: Any?,
    val evalAfter: Any?,
    val bestMove : String,
    val category: MoveCategory
)
