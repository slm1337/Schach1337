package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.NormalMove

class Knight : Piece {
    override var type: PieceType = PieceType.Knight
    override var color: Player = Player.None

    constructor(color : Player){
        this.color = color
    }

    companion object {
        private fun potentialToPositions(from : Position) : Sequence<Position> = sequence {
            for(vDir in listOf(Direction.North, Direction.South)){
                for(hDir in listOf(Direction.West, Direction.East)){
                    yield(from + (vDir * 2) + hDir)
                    yield(from + (hDir * 2) + vDir)
                }
            }
        }
    }

    override fun copy(): Piece {
        val copy = Knight(color)
        copy.hasMoved = hasMoved
        return copy
    }

    private fun movePositions(from: Position, board: Board): Sequence<Position> {
        return potentialToPositions(from).filter { pos ->
            Board.isInside(pos) && (board.isEmpty(pos) || board[pos]?.color != color)
        }
    }

    override fun getMoves(from: Position, board: Board): Sequence<Move> {
        return movePositions(from, board).map { to -> NormalMove(from, to) }
    }



}