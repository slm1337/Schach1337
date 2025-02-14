package com.example.schach1337.logic.moves

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.pieces.Bishop
import com.example.schach1337.logic.pieces.Knight
import com.example.schach1337.logic.pieces.Piece
import com.example.schach1337.logic.pieces.Queen
import com.example.schach1337.logic.pieces.Rook

class PawnPromotion : Move{
    override var type: MoveType = MoveType.PawnPromotion
    override var fromPos: Position
    override var toPos: Position

    private val newType : PieceType

    constructor(from : Position, to : Position, newType : PieceType){
        fromPos = from
        toPos = to
        this.newType = newType
    }

    private fun createPromotionPiece(color : Player) : Piece{
        return when (newType){
            PieceType.Queen -> Queen(color)
            PieceType.Knight -> Knight(color)
            PieceType.Bishop -> Bishop(color)
            else -> Rook(color)
        }
    }

    override fun execute(board: Board) {
        val pawn : Piece = board[fromPos] ?: return
        board[fromPos] = null

        val promotionPiece = createPromotionPiece(pawn.color)
        promotionPiece.hasMoved = true
        board[toPos] = promotionPiece
    }

}