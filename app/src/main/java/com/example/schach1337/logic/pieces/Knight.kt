package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player

class Knight : Piece {
    override var type: PieceType = PieceType.Knight
    override var color: Player = Player.None

    constructor(color : Player){
        this.color = color
    }

    override fun copy(): Piece {
        val copy = Knight(color)
        copy.hasMoved = hasMoved
        return copy
    }
}