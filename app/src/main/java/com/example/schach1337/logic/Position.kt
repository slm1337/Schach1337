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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Position

        if (row != other.row) return false
        if (column != other.column) return false

        return true
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + column
        return result
    }

    operator fun plus(dir : Direction) : Position{
        return Position(row + dir.rowDelta, column + dir.columnDelta)
    }

}