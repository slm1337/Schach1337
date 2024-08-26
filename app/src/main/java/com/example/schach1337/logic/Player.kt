package com.example.schach1337.logic

enum class Player {
    None,
    White,
    Black;

    companion object {
        fun opponent(player : Player) : Player {
            return when(player) {
                White -> Black
                Black -> White
                else -> None
            }
        }
    }
}