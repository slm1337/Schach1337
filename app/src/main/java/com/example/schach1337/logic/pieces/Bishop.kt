package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player

class Bishop : Piece {
    override var type: PieceType = PieceType.Bishop
    override var color: Player = Player.None

    constructor(color : Player){
        this.color = color
    }

    override fun copy(): Piece {
        val copy = Bishop(color)
        copy.hasMoved = hasMoved
        return copy
    }
}