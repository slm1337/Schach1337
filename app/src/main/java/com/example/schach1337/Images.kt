package com.example.schach1337

import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.pieces.Piece

class Images {
    companion object{
        val whiteSources = mapOf(
            PieceType.Pawn to R.drawable.ic_pawn_w,
            PieceType.Bishop to R.drawable.ic_bishop_w,
            PieceType.Knight to R.drawable.ic_knight_w,
            PieceType.Rook to R.drawable.ic_rook_w,
            PieceType.Queen to R.drawable.ic_queen_w,
            PieceType.King to R.drawable.ic_king_w,
        )

        val blackSources = mapOf(
            PieceType.Pawn to R.drawable.ic_pawn_b,
            PieceType.Bishop to R.drawable.ic_bishop_b,
            PieceType.Knight to R.drawable.ic_knight_b,
            PieceType.Rook to R.drawable.ic_rook_b,
            PieceType.Queen to R.drawable.ic_queen_b,
            PieceType.King to R.drawable.ic_king_b,
        )

        fun getImage(color : Player, type : PieceType): Int? {
            return when(color) {
                Player.White -> whiteSources[type]
                Player.Black -> blackSources[type]
                else -> null
            }
        }

        fun getImage(piece : Piece?): Int? {
            if(piece == null) {
                return null
            }

            return getImage(piece.color, piece.type)
        }
    }
}