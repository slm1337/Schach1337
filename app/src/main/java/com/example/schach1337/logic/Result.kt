package com.example.schach1337.logic

class Result {
    var winner: Player
    var reason : EndReason

    constructor(winner : Player, reason : EndReason){
        this.winner = winner
        this.reason = reason
    }

    companion object{
        fun win(winner : Player) : Result{
            return Result(winner, EndReason.Checkmate)
        }

        fun draw(reason : EndReason) : Result{
            return Result(Player.None, reason)
        }
    }
}