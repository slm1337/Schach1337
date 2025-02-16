package com.example.schach1337.logic.moves

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.Position

class DoublePawn : Move {
    override var type: MoveType = MoveType.DoublePawn
    override var fromPos: Position
    override var toPos: Position

    private val skippedPos : Position

    constructor(from : Position, to : Position){
        fromPos = from
        toPos = to
        skippedPos = Position((from.row + to.row) / 2, from.column)
    }

    override fun execute(board: Board) {
        val player = board[fromPos]!!.color
        board.setPawnSkipPosition(player, skippedPos)
        NormalMove(fromPos, toPos).execute(board)
    }

}