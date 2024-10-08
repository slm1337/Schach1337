package com.example.schach1337.logic

import com.example.schach1337.logic.pieces.*

class Board {
    private val pieces = Array(8) { Array<Piece?>(8) { null } }

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

    companion object{
        fun initial() : Board{
            val board = Board()
            board.addStartPieces()
            return board
        }

        fun isInside(pos : Position) : Boolean{
            return pos.row in 0..7 && pos.column in 0..7
        }
    }

    private fun addStartPieces(){
//        this[0, 0] = Rook(Player.Black)
//        this[0, 1] = Knight(Player.Black)
//        this[0, 2] = Bishop(Player.Black)
//        this[0, 3] = Queen(Player.Black)
//        this[0, 4] = King(Player.Black)
//        this[0, 5] = Bishop(Player.Black)
//        this[0, 6] = Knight(Player.Black)
//        this[0, 7] = Rook(Player.Black)
//
//        this[7, 0] = Rook(Player.White)
//        this[7, 1] = Knight(Player.White)
//        this[7, 2] = Bishop(Player.White)
//        this[7, 3] = Queen(Player.White)
//        this[7, 4] = King(Player.White)
//        this[7, 5] = Bishop(Player.White)
//        this[7, 6] = Knight(Player.White)
//        this[7, 7] = Rook(Player.White)
//
//        for(c in 0..7){
//            this[1, c] = Pawn(Player.Black)
//            this[6, c] = Pawn(Player.White)
//        }


        this[4, 4] = King(Player.Black)
        this[7, 4] = King(Player.White)
        this[5, 3] = Queen(Player.Black)


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

}