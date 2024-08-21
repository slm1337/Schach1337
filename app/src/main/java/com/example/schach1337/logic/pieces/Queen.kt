package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player

class Queen : Piece {
    override var type: PieceType = PieceType.Queen
    override var color: Player = Player.None

    constructor(color : Player){
        this.color = color
    }

    override fun copy(): Piece {
        val copy = Queen(color)
        copy.hasMoved = hasMoved
        return copy
    }
}