package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.NormalMove

class Pawn : Piece {
    override var type: PieceType = PieceType.Pawn
    override var color: Player = Player.None

    private val forward : Direction

    constructor(color : Player){
        this.color = color

        forward = if(color == Player.White){
            Direction.North
        } else {
            Direction.South
        }
    }

    companion object{
        private fun canMoveTo(pos : Position, board : Board) : Boolean{
            return Board.isInside(pos) && board.isEmpty(pos)
        }
    }

    override fun copy(): Piece {
        val copy = Pawn(color)
        copy.hasMoved = hasMoved
        return copy
    }

    override fun getMoves(from: Position, board: Board): Sequence<Move> {
        return forwardMoves(from, board).plus(diagonalMoves(from, board))
    }

    private fun forwardMoves(from: Position, board: Board): Sequence<Move> = sequence {
        val oneMovePos = from + forward

        if (canMoveTo(oneMovePos, board)) {
            yield(NormalMove(from, oneMovePos))

            val twoMovesPos = oneMovePos + forward

            if (!hasMoved && canMoveTo(twoMovesPos, board)) {
                yield(NormalMove(from, twoMovesPos))
            }
        }
    }

    private fun diagonalMoves(from: Position, board: Board): Sequence<Move> = sequence {
        for (dir in listOf(Direction.West, Direction.East)) {
            val to = from + forward + dir

            if (canCaptureAt(to, board)) {
                yield(NormalMove(from, to))
            }
        }
    }




    private fun canCaptureAt(pos : Position, board : Board) : Boolean{
        if(!Board.isInside(pos) || board.isEmpty(pos)){
            return false
        }

        return board[pos]!!.color != color
    }




}