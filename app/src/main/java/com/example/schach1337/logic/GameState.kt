package com.example.schach1337.logic

import com.example.schach1337.logic.moves.Move

class GameState {
    var board : Board
    private var currentPlayer : Player
    var result : Result? = null

    constructor(player : Player, board : Board){
        currentPlayer = player
        this.board = board
    }

    fun legalMovesForPiece(pos : Position): Sequence<Move>? {
        if(board.isEmpty(pos) || board[pos]?.color != currentPlayer){
            return emptySequence()
        }

        val piece = board[pos]
        val moveCandidates : Sequence<Move>? = piece?.getMoves(pos, board)
        return moveCandidates?.filter {move -> move.isLegal(board)}
    }

    fun makeMove(move : Move){
        move.execute(board)
        currentPlayer = Player.opponent(currentPlayer)
        checkForGameOver()
    }

    fun allLegalMovesFor(player : Player) : Sequence<Move>{
        val moveCandidates : Sequence<Move> = board.piecePositionsFor(player).flatMap { pos ->
            val piece = board[pos]
            piece?.getMoves(pos, board) ?: emptySequence()
        }

        return moveCandidates.filter {move -> move.isLegal(board)}
    }

    private fun checkForGameOver(){
        if(!allLegalMovesFor(currentPlayer).any()){
            if(board.isInCheck(currentPlayer)){
                result = Result.win(Player.opponent(currentPlayer))
            } else{
                result = Result.draw(EndReason.Stalemate)
            }
        }
    }

    fun isGameOver () : Boolean{
        return result != null
    }

}