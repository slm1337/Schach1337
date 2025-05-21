package com.example.schach1337.logic

import com.example.schach1337.logic.moves.Move

class GameState {
    var board : Board
    var currentPlayer : Player
    var result : Result? = null
    private var noCaptureOrPawnMove : Int = 0
    lateinit var stateString : String
    private val stateHistory : MutableMap<String, Int> = mutableMapOf()

    var gameHistoryFENs: MutableList<String> = mutableListOf()
    var gameHistory: MutableList<String> = mutableListOf()

    constructor(player : Player, board : Board){
        currentPlayer = player
        this.board = board

        stateString = StateString(currentPlayer, board).toString()
        stateHistory[stateString] = 1
        clearHistory()
        gameHistoryFENs.add(stateString)
        gameHistory.add(" ")
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
            stateHistory.clear()
        } else {
            noCaptureOrPawnMove++;
        }

        currentPlayer = Player.opponent(currentPlayer)
        updateStateString()
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
        else if(threefoldRepetition()){
            result = Result.draw(EndReason.ThreefoldRepetition)
        }
    }

    fun isGameOver () : Boolean{
        return result != null
    }

    private fun FiftyMoveRule() : Boolean{
        val fullMoves : Int = noCaptureOrPawnMove / 2
        return fullMoves == 50;
    }

    private fun updateStateString() {
        stateString = StateString(currentPlayer, board).toString()


        gameHistoryFENs.add(stateString)
        val move = FenUtils.getMoveFromFENs(gameHistoryFENs[gameHistoryFENs.size-2], gameHistoryFENs[gameHistoryFENs.size-1])!!
        gameHistory.add(move)

        stateHistory[stateString] = stateHistory.getOrDefault(stateString, 0) + 1
    }


    private fun threefoldRepetition() : Boolean{
        return stateHistory[stateString] == 3
    }

    fun updateMoveHistoryFromFENs(){
        for (i in 1 until gameHistoryFENs.size) {
            val move = FenUtils.getMoveFromFENs(gameHistoryFENs[i - 1], gameHistoryFENs[i])!!
            gameHistory.add(move)
        }
    }

    fun clearHistory(){
        gameHistoryFENs.clear()
        gameHistory.clear()
    }

}