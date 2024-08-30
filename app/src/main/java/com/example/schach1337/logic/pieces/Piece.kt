package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move


abstract class Piece {
    abstract var type : PieceType
    abstract var color : Player
    var hasMoved : Boolean = false

    abstract fun copy() : Piece

    abstract fun getMoves(from : Position, board : Board) : Sequence<Move>

    protected fun movePositionsInDir(from: Position, board: Board, dir: Direction): Sequence<Position> = sequence {
        var pos = from + dir
        while (Board.isInside(pos)) {
            if (board.isEmpty(pos)) {
                yield(pos)
                pos += dir
                continue
            }

            val piece = board[pos]

            if (piece != null && piece.color != color) {
                yield(pos)
            }

            break
        }
    }

    protected fun movePositionInDirs(from: Position, board: Board, dirs: Array<Direction>): Sequence<Position> {
        return dirs.asSequence().flatMap { dir -> movePositionsInDir(from, board, dir) }
    }

    open fun canCaptureOpponentKing(from : Position, board : Board) : Boolean{
        return getMoves(from, board).any{move ->
            val piece = board[move.toPos]
            piece != null && piece.type == PieceType.King
        }
    }

}