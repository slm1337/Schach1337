package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player

class Pawn : Piece {
    override var type: PieceType = PieceType.Pawn
    override var color: Player = Player.None

    constructor(color : Player){
        this.color = color
    }

    override fun copy(): Piece {
        val copy = Pawn(color)
        copy.hasMoved = hasMoved
        return copy
    }
}