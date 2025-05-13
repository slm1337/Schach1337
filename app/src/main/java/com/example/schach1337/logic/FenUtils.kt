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

    fun getMoveFromFENs(fen1: String, fen2: String): String? {
        val board1 = parseFENBoard(fen1)
        val board2 = parseFENBoard(fen2)
        val turn = fen1.split(" ")[1] // чей ход: 'w' или 'b'

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

        if (isKingsideCastling(fen1, fen2, turn)) return "O-O"
        if (isQueensideCastling(fen1, fen2, turn)) return "O-O-O"

        val fromSquare = differences.find { it.second != null && (it.third == null || it.second != it.third) }?.first
        val toSquare = differences.find { it.third != null && (it.second == null || it.second != it.third) }?.first

        if (fromSquare != null && toSquare != null) {
            val toRank = toSquare[1].digitToInt()
            val promotionRank = if (turn == "w") 8 else 1
            val promotedPiece = differences.find { it.first == toSquare }?.third
            if ((turn == "w" && toRank == 8 || turn == "b" && toRank == 1) &&
                (promotedPiece == 'q' || promotedPiece == 'r' || promotedPiece == 'b' || promotedPiece == 'n'
                        || promotedPiece == 'Q' || promotedPiece == 'R' || promotedPiece == 'B' || promotedPiece == 'N')
            ) {
                return "$fromSquare$toSquare${promotedPiece.lowercaseChar()}"
            }

            val isPawn = differences.find { it.first == fromSquare }?.second?.lowercaseChar() == 'p'
            val fromRank = fromSquare[1].digitToInt()
            val toFile = toSquare[0]
            val fromFile = fromSquare[0]
            if (isPawn && fromFile != toFile && differences.size == 1) {
                return "$fromSquare$toSquare" // Взятие на проходе выглядит как обычное взятие
            }

            return "$fromSquare$toSquare"
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
            "w" -> {
                board1[7][4] == 'K' && board1[7][7] == 'R' && // E1, H1
                        board2[7][6] == 'K' && board2[7][5] == 'R' && // G1, F1
                        board2[7][4] == null && board2[7][7] == null  // E1, H1 пусты
            }
            "b" -> {
                board1[0][4] == 'k' && board1[0][7] == 'r' && // E8, H8
                        board2[0][6] == 'k' && board2[0][5] == 'r' && // G8, F8
                        board2[0][4] == null && board2[0][7] == null  // E8, H8 пусты
            }
            else -> false
        }
    }

    fun isQueensideCastling(fen1: String, fen2: String, turn: String): Boolean {
        val board1 = parseFENBoard(fen1)
        val board2 = parseFENBoard(fen2)

        return when (turn) {
            "w" -> {
                board1[7][4] == 'K' && board1[7][0] == 'R' && // E1, A1
                        board2[7][2] == 'K' && board2[7][3] == 'R' && // C1, D1
                        board2[7][4] == null && board2[7][0] == null  // E1, A1 пусты
            }
            "b" -> {
                board1[0][4] == 'k' && board1[0][0] == 'r' && // E8, A8
                        board2[0][2] == 'k' && board2[0][3] == 'r' && // C8, D8
                        board2[0][4] == null && board2[0][0] == null  // E8, A8 пусты
            }
            else -> false
        }
    }

}