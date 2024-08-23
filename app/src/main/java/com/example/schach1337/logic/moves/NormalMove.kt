package com.example.schach1337.logic.moves

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.pieces.Piece

class NormalMove : Move {
    override var type: MoveType = MoveType.Normal
    override var fromPos: Position
    override var toPos: Position

    constructor(from : Position, to : Position){
        fromPos = from
        toPos = to
    }

    override fun execute(board: Board) {
        val piece : Piece = board[fromPos] ?: return
        board[toPos] = piece
        board[fromPos] = null
        piece.hasMoved = true
    }

}