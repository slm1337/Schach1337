package com.example.schach1337.logic.moves

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position

abstract class Move {
    abstract var type : MoveType
    abstract var fromPos : Position
    abstract var toPos : Position

    abstract fun execute(board : Board)

    open fun isLegal(board : Board): Boolean {
        val player : Player? = board[fromPos]?.color
        val boardCopy = board.copy()
        execute(boardCopy)
        return !boardCopy.isInCheck(player!!)
    }
}