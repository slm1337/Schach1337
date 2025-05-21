package com.example.schach1337.logic

import com.example.schach1337.logic.moves.EnPassant
import com.example.schach1337.logic.pieces.*

class Board {
    private val pieces = Array(8) { Array<Piece?>(8) { null } }
    private val pawnSkipPositions: MutableMap<Player, Position?> = mutableMapOf(
        Player.White to null,
        Player.Black to null
    )

    operator fun get(row: Int, col: Int): Piece? {
        return pieces[row][col]
    }

    operator fun set(row: Int, col: Int, value: Piece?) {
        pieces[row][col] = value
    }

    operator fun get(pos: Position): Piece? {
        return this[pos.row, pos.column]
    }

    operator fun set(pos: Position, value: Piece?) {
        this[pos.row, pos.column] = value
    }

    fun getPawnSkipPosition(player: Player): Position? {
        return pawnSkipPositions[player]
    }

    fun setPawnSkipPosition(player: Player, pos: Position?) {
        pawnSkipPositions[player] = pos
    }

    companion object {
        fun initial(): Board {
            val board = Board()
            board.addStartPieces()
            return board
        }

        fun initial(fen: String): Board {
            val board = Board()
            board.addStartPieces(fen)
            return board
        }

        fun isInside(pos: Position): Boolean {
            return pos.row in 0..7 && pos.column in 0..7
        }

        private fun isKingVKing(counting: Counting): Boolean {
            return counting.totalCount == 2
        }

        private fun isKingBishopVKing(counting: Counting): Boolean {
            return counting.totalCount == 3 && (counting.white(PieceType.Bishop) == 1 || counting.black(PieceType.Bishop) == 1)
        }

        private fun isKingKnightVKing(counting: Counting): Boolean {
            return counting.totalCount == 3 && (counting.white(PieceType.Knight) == 1 || counting.black(PieceType.Knight) == 1)
        }
    }

    private fun addStartPieces() {
        this[0, 0] = Rook(Player.Black)
        this[0, 1] = Knight(Player.Black)
        this[0, 2] = Bishop(Player.Black)
        this[0, 3] = Queen(Player.Black)
        this[0, 4] = King(Player.Black)
        this[0, 5] = Bishop(Player.Black)
        this[0, 6] = Knight(Player.Black)
        this[0, 7] = Rook(Player.Black)

        this[7, 0] = Rook(Player.White)
        this[7, 1] = Knight(Player.White)
        this[7, 2] = Bishop(Player.White)
        this[7, 3] = Queen(Player.White)
        this[7, 4] = King(Player.White)
        this[7, 5] = Bishop(Player.White)
        this[7, 6] = Knight(Player.White)
        this[7, 7] = Rook(Player.White)

        for (c in 0..7) {
            this[1, c] = Pawn(Player.Black)
            this[6, c] = Pawn(Player.White)
        }
    }

    private fun addStartPieces(fen: String) {
        if (fen.isEmpty()) {
            addStartPieces()
            return
        }

        clearBoard()

        val pieceMap = mapOf(
            'p' to { Pawn(Player.Black) }, 'r' to { Rook(Player.Black) },
            'n' to { Knight(Player.Black) }, 'b' to { Bishop(Player.Black) },
            'q' to { Queen(Player.Black) }, 'k' to { King(Player.Black) },
            'P' to { Pawn(Player.White) }, 'R' to { Rook(Player.White) },
            'N' to { Knight(Player.White) }, 'B' to { Bishop(Player.White) },
            'Q' to { Queen(Player.White) }, 'K' to { King(Player.White) }
        )

        val parts = fen.trim().split(" ")
        val rows = parts[0].split("/")
        for ((rowIndex, row) in rows.withIndex()) {
            var col = 0
            for (char in row) {
                if (char.isDigit()) {
                    col += char.digitToInt()
                } else {
                    this[rowIndex, col] = pieceMap[char]?.invoke()
                    col++
                }
            }
        }

        // Set en passant (pawn skip) position
        val enPassant = parts[3]
        if (enPassant != "-") {
            val col = enPassant[0].lowercaseChar() - 'a'
            val row = 8 - enPassant[1].digitToInt()
            val player = if (row == 2) Player.White else Player.Black // White pawn moved to rank 6, Black to rank 3
            setPawnSkipPosition(Player.opponent(player), Position(row, col))
        } else {
            setPawnSkipPosition(Player.White, null)
            setPawnSkipPosition(Player.Black, null)
        }
    }

    fun setFromFen(fen: String) {
//        if (!FenUtils.isValidFEN(fen)) {
//            throw IllegalArgumentException("Invalid FEN string: $fen")
//        }
        addStartPieces(fen)
    }

    private fun clearBoard() {
        for (r in 0..7) {
            for (c in 0..7) {
                this[r, c] = null
            }
        }
    }

    fun isEmpty(pos: Position): Boolean {
        return this[pos] == null
    }

    fun piecePositions(): Sequence<Position> = sequence {
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                if (!isEmpty(pos)) {
                    yield(pos)
                }
            }
        }
    }

    fun piecePositionsFor(player: Player): Sequence<Position> {
        return piecePositions().filter { pos -> this[pos]?.color == player }
    }

    fun isInCheck(player: Player): Boolean {
        return piecePositionsFor(Player.opponent(player)).any { pos ->
            val piece = this[pos]
            piece?.canCaptureOpponentKing(pos, this) == true
        }
    }

    fun copy(): Board {
        val copy = Board()
        for (pos in piecePositions()) {
            copy[pos] = this[pos]?.copy()
        }
        copy.setPawnSkipPosition(Player.White, getPawnSkipPosition(Player.White))
        copy.setPawnSkipPosition(Player.Black, getPawnSkipPosition(Player.Black))
        return copy
    }

    fun countPieces(): Counting {
        val counting = Counting()
        for (pos: Position in piecePositions()) {
            val piece = this[pos]!!
            counting.increment(piece.color, piece.type)
        }
        return counting
    }

    fun insifficientMaterial(): Boolean {
        val counting = countPieces()
        return isKingVKing(counting) || isKingBishopVKing(counting) ||
                isKingKnightVKing(counting) || isKingBishopVKingBishop(counting)
    }

    private fun findPiece(color: Player, type: PieceType): Position {
        return piecePositionsFor(color).first { pos ->
            this[pos]?.type == type
        }
    }

    private fun isKingBishopVKingBishop(counting: Counting): Boolean {
        if (counting.totalCount != 4) {
            return false
        }
        if (counting.white(PieceType.Bishop) != 1 || counting.black(PieceType.Bishop) != 1) {
            return false
        }
        val wBishopPos: Position = findPiece(Player.White, PieceType.Bishop)
        val bBishopPos: Position = findPiece(Player.Black, PieceType.Bishop)
        return wBishopPos.squareColor() == bBishopPos.squareColor()
    }

    private fun isUnmovedKingAndRook(kingPos: Position, rookPos: Position): Boolean {
        if (isEmpty(kingPos) || isEmpty(rookPos)) {
            return false
        }
        val king = this[kingPos]!!
        val rook = this[rookPos]!!
        return king.type == PieceType.King && rook.type == PieceType.Rook &&
                !king.hasMoved && !rook.hasMoved
    }

    fun castleRightKS(player: Player): Boolean {
        return when (player) {
            Player.White -> isUnmovedKingAndRook(Position(7, 4), Position(7, 7))
            Player.Black -> isUnmovedKingAndRook(Position(0, 4), Position(0, 7))
            else -> false
        }
    }

    fun castleRightQS(player: Player): Boolean {
        return when (player) {
            Player.White -> isUnmovedKingAndRook(Position(7, 4), Position(7, 0))
            Player.Black -> isUnmovedKingAndRook(Position(0, 4), Position(0, 0))
            else -> false
        }
    }

    private fun hasPawnInPosition(player: Player, pawnPositions: Array<Position>, skipPos: Position): Boolean {
        for (pos in pawnPositions.filter { isInside(it) }) {
            val piece = this[pos] ?: continue
            if (piece.color != player || piece.type != PieceType.Pawn) {
                continue
            }
            val move = EnPassant(pos, skipPos)
            if (move.isLegal(this)) {
                return true
            }
        }
        return false
    }

    fun canCaptureEnPassant(player: Player): Boolean {
        val skipPos = getPawnSkipPosition(Player.opponent(player)) ?: return false
        val pawnPositions = when (player) {
            Player.White -> arrayOf(skipPos + Direction.SouthWest, skipPos + Direction.SouthEast)
            Player.Black -> arrayOf(skipPos + Direction.NorthWest, skipPos + Direction.NorthEast)
            else -> emptyArray()
        }
        return hasPawnInPosition(player, pawnPositions, skipPos)
    }
}