package com.example.schach1337.logic

import com.example.schach1337.logic.pieces.Piece

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

    fun getMoveFromFENs(fen1: String, fen2: String): String? {
        if (isKingsideCastling(fen1, fen2, fen1.split(" ")[1])) return "O-O"
        if (isQueensideCastling(fen1, fen2, fen1.split(" ")[1])) return "O-O-O"

        val board1 = parseFENBoard(fen1)
        val board2 = parseFENBoard(fen2)

        val differences = mutableListOf<Triple<String, Char?, Char?>>()

        for (rank in 0 until 8) {
            for (file in 0 until 8) {
                val square = "${'a' + file}${8 - rank}"
                val piece1 = board1[rank][file]
                val piece2 = board2[rank][file]
                if (piece1 != piece2) {
                    differences.add(Triple(square, piece1, piece2))
                }
            }
        }

        val fromSquare = differences.find { it.second != null && it.third == null }?.first
        val toSquare = differences.find { it.third != null }?.first

        if (fromSquare != null && toSquare != null) {
            val isCapture = differences.any { it.second != null && it.third == null && it.first == toSquare }
            val move = if (isCapture) "$fromSquare x $toSquare" else "$fromSquare-$toSquare"
            return move
        }

        return null
    }

    fun parseFENBoard(fen: String): Array<Array<Char?>> {
        val board = Array(8) { Array<Char?>(8) { null } }
        val rows = fen.split(" ")[0].split("/")

        for ((rank, row) in rows.withIndex()) {
            var file = 0
            for (c in row) {
                if (c.isDigit()) {
                    file += c.digitToInt()
                } else {
                    board[rank][file] = c
                    file++
                }
            }
        }
        return board
    }

    fun isKingsideCastling(fen1: String, fen2: String, turn: String): Boolean {
        val board1 = parseFENBoard(fen1)
        val board2 = parseFENBoard(fen2)

        return when (turn) {
            "w" -> board1[7][4] == 'K' && board2[7][6] == 'K'
            "b" -> board1[0][4] == 'k' && board2[0][6] == 'k'
            else -> false
        }
    }

    fun isQueensideCastling(fen1: String, fen2: String, turn: String): Boolean {
        val board1 = parseFENBoard(fen1)
        val board2 = parseFENBoard(fen2)

        return when (turn) {
            "w" -> board1[7][4] == 'K' && board2[7][2] == 'K'
            "b" -> board1[0][4] == 'k' && board2[0][2] == 'k'
            else -> false
        }
    }
}