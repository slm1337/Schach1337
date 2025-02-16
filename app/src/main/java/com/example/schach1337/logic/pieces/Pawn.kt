package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.DoublePawn
import com.example.schach1337.logic.moves.EnPassant
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.NormalMove
import com.example.schach1337.logic.moves.PawnPromotion

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

    private fun promotionMoves(from: Position, to : Position): Sequence<Move> = sequence {
        yield(PawnPromotion(from, to, PieceType.Queen))
        yield(PawnPromotion(from, to, PieceType.Knight))
        yield(PawnPromotion(from, to, PieceType.Bishop))
        yield(PawnPromotion(from, to, PieceType.Rook))
    }

    private fun forwardMoves(from: Position, board: Board): Sequence<Move> = sequence {
        val oneMovePos = from + forward

        if (canMoveTo(oneMovePos, board)) {

            if(oneMovePos.row == 0 || oneMovePos.row == 7){
                for(promMove : Move in promotionMoves(from, oneMovePos)){
                    yield(promMove)
                }
            }
            else
            {
                yield(NormalMove(from, oneMovePos))
            }

            val twoMovesPos = oneMovePos + forward

            if (!hasMoved && canMoveTo(twoMovesPos, board)) {
                yield(DoublePawn(from, twoMovesPos))
            }
        }
    }

    private fun diagonalMoves(from: Position, board: Board): Sequence<Move> = sequence {
        for (dir in listOf(Direction.West, Direction.East)) {
            val to = from + forward + dir

            val opponent : Player = Player.opponent(color)
            if(to == board.getPawnSkipPosition(opponent)){
                yield(EnPassant(from, to))
            } else if (canCaptureAt(to, board)) {
                if(to.row == 0 || to.row == 7){
                    for(promMove : Move in promotionMoves(from, to)){
                        yield(promMove)
                    }
                }
                else
                {
                    yield(NormalMove(from, to))
                }
            }
        }
    }

    private fun canCaptureAt(pos : Position, board : Board) : Boolean{
        if(!Board.isInside(pos) || board.isEmpty(pos)){
            return false
        }

        return board[pos]!!.color != color
    }

    override fun canCaptureOpponentKing(from: Position, board: Board): Boolean {
        return diagonalMoves(from, board).any { move ->
            val piece = board[move.toPos]
            piece != null && piece.type == PieceType.King
        }
    }



}