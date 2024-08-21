package com.example.schach1337.logic

enum class Player {
    None,
    White,
    Black;
}

class PlayerExtensions{
    companion object {
        fun opponent(player : Player) {
            when(player) {
                Player.White -> Player.Black
                Player.Black -> Player.White
                else -> Player.None
            }
        }
    }
}