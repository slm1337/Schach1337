package com.example.schach1337.logic

class Direction {
    var rowDelta : Int = 0
        private set(value){
            field = value
        }

    var columnDelta : Int = 0
        private set(value) {
            field = value
        }

    constructor(rowDelta : Int, columnDelta : Int){
        this.rowDelta = rowDelta
        this.columnDelta = columnDelta
    }

    companion object{
        val North : Direction = Direction(-1, 0)
        val South : Direction = Direction(1, 0)
        val East : Direction = Direction(0, 1)
        val West : Direction = Direction(0, -1)
        val NorthEast : Direction = North + East
        val NorthWest : Direction = North + West
        val SouthEast : Direction = South + East
        val SouthWest : Direction = South + West
    }

    operator fun plus(dir1 : Direction) : Direction{
        return Direction(this.rowDelta + dir1.rowDelta, this.columnDelta + dir1.columnDelta)
    }

    operator fun times(scalar : Int) : Direction{
        return Direction(rowDelta * scalar, columnDelta * scalar)
    }
}