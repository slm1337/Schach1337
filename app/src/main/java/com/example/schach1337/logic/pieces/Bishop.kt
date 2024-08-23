package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.NormalMove

class Bishop : Piece {
    override var type: PieceType = PieceType.Bishop
    override var color: Player = Player.None

    companion object{
        private val dirs : Array<Direction> = arrayOf(
            Direction.NorthWest,
            Direction.NorthEast,
            Direction.SouthWest,
            Direction.SouthEast
        )
    }

    constructor(color : Player){
        this.color = color
    }

    override fun copy(): Piece {
        val copy = Bishop(color)
        copy.hasMoved = hasMoved
        return copy
    }

    override fun getMoves(from: Position, board: Board): Sequence<Move> {
        return movePositionInDirs(from, board, dirs).map { to -> NormalMove(from, to) }
    }
}