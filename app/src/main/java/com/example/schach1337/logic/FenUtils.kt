package com.example.schach1337.logic

object FenUtils {
    fun isValidFEN(fen: String): Boolean {
        val parts = fen.trim().split(" ")
        if (parts.size != 6) return false

        val board = parts[0]
        val activeColor = parts[1]
        val castling = parts[2]
        val enPassant = parts[3]
        val halfmove = parts[4]
        val fullmove = parts[5]

        val rows = board.split("/")
        if (rows.size != 8) return false
        for (row in rows) {
            var count = 0
            for (c in row) {
                count += if (c.isDigit()) c.digitToInt() else 1
            }
            if (count != 8) return false
        }

        if (activeColor != "w" && activeColor != "b") return false

        if (!castling.matches(Regex("[-KQkq]+"))) return false

        if (enPassant != "-" && !enPassant.matches(Regex("^[a-h][36]$"))) return false

        if (!halfmove.matches(Regex("^\\d+$"))) return false
        if (!fullmove.matches(Regex("^\\d+$"))) return false

        return true
    }

    fun currentPlayer(fen: String): Player? {
        val parts = fen.trim().split(" ")
        if (parts.size < 2) return null
        return when (parts[1]) {
            "w" -> Player.White
            "b" -> Player.Black
            else -> null
        }
    }
}