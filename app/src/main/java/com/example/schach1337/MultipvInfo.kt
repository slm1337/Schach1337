package com.example.schach1337

data class MultipvInfo(
    val fen : String,
    val depth: Int,
    val score: Any?,      // Float for cp, String for mate
    val pv: Any?,
    val bestMove: String,

)
