package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.NormalMove

class King : Piece {
    override var type: PieceType = PieceType.King
    override var color: Player = Player.None

    constructor(color : Player){
        this.color = color
    }

    companion object{
        private val dirs = arrayOf(
            Direction.North,
            Direction.South,
            Direction.East,
            Direction.West,
            Direction.NorthWest,
            Direction.NorthEast,
            Direction.SouthWest,
            Direction.SouthEast
        )
    }

    override fun copy(): Piece {
        val copy = King(color)
        copy.hasMoved = hasMoved
        return copy
    }

    override fun getMoves(from: Position, board: Board): Sequence<Move> = sequence{
        for(to in movePositions(from, board)){
            yield(NormalMove(from, to))
        }
    }

    private fun movePositions(from : Position, board : Board) : Sequence<Position> = sequence{
        for(dir in dirs){
            val to : Position = from + dir

            if(!Board.isInside(to)){
                continue
            }

            if(board.isEmpty(to) || board[to]?.color != color){
                yield(to)
            }
        }
    }

    override fun canCaptureOpponentKing(from: Position, board: Board): Boolean {
        return movePositions(from, board).any { to ->
            val piece = board[to]
            piece != null && piece.type == PieceType.King
        }
    }

}