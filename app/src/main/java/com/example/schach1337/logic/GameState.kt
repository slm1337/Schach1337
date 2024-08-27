package com.example.schach1337.logic

import com.example.schach1337.logic.moves.Move

class GameState {
    var board : Board
    private var currentPlayer : Player

    constructor(player : Player, board : Board){
        currentPlayer = player
        this.board = board
    }

    fun legalMovesForPiece(pos : Position): Sequence<Move>? {
        if(board.isEmpty(pos) || board[pos]?.color != currentPlayer){
            return emptySequence()
        }

        val piece = board[pos]
        return piece?.getMoves(pos, board)
    }

    fun makeMove(move : Move){
        move.execute(board)
        currentPlayer = Player.opponent(currentPlayer)
    }
}