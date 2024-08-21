package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player


abstract class Piece {
    abstract var type : PieceType
    abstract var color : Player
    var hasMoved : Boolean = false

    abstract fun copy() : Piece
}