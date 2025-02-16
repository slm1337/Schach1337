package com.example.schach1337.logic

import com.example.schach1337.logic.moves.Move

class GameState {
    var board : Board
    var currentPlayer : Player
    var result : Result? = null
    private var noCaptureOrPawnMove : Int = 0

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
        board.setPawnSkipPosition(currentPlayer, null)
        val captureOrPawn : Boolean = move.execute(board)

        if(captureOrPawn){
            noCaptureOrPawnMove = 0
        } else {
            noCaptureOrPawnMove++;
        }

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
        } else if(board.insifficientMaterial()){
            result = Result.draw(EndReason.InsufficientMaterial)
        } else if(FiftyMoveRule()){
            result = Result.draw(EndReason.FiftyMoveRule)
        }
    }

    fun isGameOver () : Boolean{
        return result != null
    }

    private fun FiftyMoveRule() : Boolean{
        val fullMoves : Int = noCaptureOrPawnMove / 2
        return fullMoves == 50;
    }
}