package com.example.schach1337.logic

class Position{
    var row : Int = 0
        private set (value) {
            field = value
        }

    var column = 0
        private set (value) {
            field = value
        }

    constructor(row : Int, column : Int) {
        this.row = row
        this.column = column
    }

    fun squareColor() : Player {
        if((row + column) % 2 == 0){
            return Player.White
        }
        return Player.Black
    }


}