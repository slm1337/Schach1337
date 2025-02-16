package com.example.schach1337.logic

import com.example.schach1337.logic.pieces.*

class Board {
    private val pieces = Array(8) { Array<Piece?>(8) { null } }
    private val pawnSkipPositions: MutableMap<Player, Position?> = mutableMapOf(
        Player.White to null,
        Player.Black to null
    )

    operator fun get(row: Int, col: Int): Piece? {
        return pieces[row][col]
    }

    operator fun set(row: Int, col: Int, value: Piece?) {
        pieces[row][col] = value
    }

    operator fun get(pos: Position): Piece? {
        return this[pos.row, pos.column]
    }

    operator fun set(pos: Position, value: Piece?) {
        this[pos.row, pos.column] = value
    }

    fun getPawnSkipPosition(player : Player) : Position? {
        return pawnSkipPositions[player]
    }

    fun setPawnSkipPosition(player : Player, pos : Position?) {
        pawnSkipPositions[player] = pos
    }

    companion object{
        fun initial() : Board{
            val board = Board()
            board.addStartPieces()
            return board
        }

        fun isInside(pos : Position) : Boolean{
            return pos.row in 0..7 && pos.column in 0..7
        }

        private fun isKingVKing(counting: Counting) : Boolean{
            return counting.totalCount == 2
        }

        private fun isKingBishopVKing(counting: Counting) : Boolean{
            return counting.totalCount == 3 && (counting.white(PieceType.Bishop) == 1 || counting.black(PieceType.Bishop) == 1)
        }

        private fun isKingKnightVKing(counting: Counting) : Boolean{
            return counting.totalCount == 3 && (counting.white(PieceType.Knight) == 1 || counting.black(PieceType.Knight) == 1)
        }
    }

    private fun addStartPieces(){
//        this[0, 0] = Rook(Player.Black)
        this[0, 1] = Knight(Player.Black)
        this[0, 2] = Bishop(Player.Black)
//        this[0, 3] = Queen(Player.Black)
        this[0, 4] = King(Player.Black)
//        this[0, 5] = Bishop(Player.Black)
//        this[0, 6] = Knight(Player.Black)
//        this[0, 7] = Rook(Player.Black)
//
//        this[7, 0] = Rook(Player.White)
//        this[7, 1] = Knight(Player.White)
//        this[7, 2] = Bishop(Player.White)
//        this[7, 3] = Queen(Player.White)
        this[7, 4] = King(Player.White)
//        this[7, 5] = Bishop(Player.White)
//        this[7, 6] = Knight(Player.White)
//        this[7, 7] = Rook(Player.White)

//        for(c in 0..7){
//            this[1, c] = Pawn(Player.Black)
//            this[6, c] = Pawn(Player.White)
//        }

    }

    fun isEmpty(pos : Position): Boolean {
        return this[pos] == null
    }

    fun piecePositions() : Sequence<Position> = sequence{
        for(r in 0..7){
            for(c in 0..7){
                val pos = Position(r, c)

                if(!isEmpty(pos)){
                    yield(pos)
                }
            }
        }
    }

    fun piecePositionsFor(player : Player) : Sequence<Position>{
        return piecePositions().filter { pos -> this[pos]?.color == player }
    }

    fun isInCheck(player: Player): Boolean {
        return piecePositionsFor(Player.opponent(player)).any { pos ->
            val piece = this[pos]
            piece?.canCaptureOpponentKing(pos, this) == true
        }
    }

    fun copy() : Board {
        val copy = Board()

        for(pos in piecePositions()){
            copy[pos] = this[pos]?.copy()
        }

        return copy
    }

    fun countPieces() : Counting{
        val counting = Counting()

        for(pos : Position in piecePositions()){
            val piece = this[pos]!!
            counting.increment(piece.color, piece.type)
        }

        return counting
    }

    fun insifficientMaterial() : Boolean{
        val counting = countPieces()

        return isKingVKing(counting) || isKingBishopVKing(counting) ||
                isKingKnightVKing(counting) || isKingBishopVKingBishop(counting)
    }

    private fun findPiece(color : Player, type : PieceType) : Position{
        return piecePositionsFor(color).first{ pos ->
            this[pos]?.type == type
        }
    }

    private fun isKingBishopVKingBishop(counting: Counting) : Boolean{
        if (counting.totalCount != 4){
            return false
        }

        if(counting.white(PieceType.Bishop) != 1 || counting.black(PieceType.Bishop) != 1){
            return false
        }

        val wBishopPos : Position = findPiece(Player.White, PieceType.Bishop)
        val bBishopPos : Position = findPiece(Player.Black, PieceType.Bishop)

        return wBishopPos.squareColor() == bBishopPos.squareColor()
    }

}