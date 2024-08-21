package com.example.schach1337.logic

class GameState {
    var board : Board
    var currentPlayer : Player
        private set

    constructor(player : Player, board : Board){
        currentPlayer = player
        this.board = board
    }
}