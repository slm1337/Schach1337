package com.example.schach1337.logic.moves

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.Position

class EnPassant : Move {
    override var type: MoveType = MoveType.EnPassant

    override var fromPos: Position

    override var toPos: Position

    private val capturePos : Position

    constructor(from : Position, to : Position){
        fromPos = from
        toPos = to
        capturePos = Position(from.row, to.column)
    }

    override fun execute(board : Board) : Boolean {
        NormalMove(fromPos, toPos).execute(board)
        board[capturePos] = null
        return true
    }
}