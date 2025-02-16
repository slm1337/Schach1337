package com.example.schach1337.logic

import com.example.schach1337.logic.pieces.Piece

class Counting {
    private val whiteCount : MutableMap<PieceType, Int> = mutableMapOf()
    private val blackCount : MutableMap<PieceType, Int> = mutableMapOf()

    var totalCount : Int = 0

    constructor(){
        for (type in PieceType.values()) {
            whiteCount[type] = 0
            blackCount[type] = 0
        }
    }

    fun increment(color : Player, type : PieceType){
        if(color == Player.White){
            whiteCount[type] = (whiteCount[type] ?: 0) + 1
        } else if (color == Player.Black){
            blackCount[type] = (blackCount[type] ?: 0) + 1
        }
        totalCount++
    }

    fun white(type : PieceType) : Int?{
        return whiteCount[type]
    }

    fun black(type : PieceType) : Int?{
        return blackCount[type]
    }
}