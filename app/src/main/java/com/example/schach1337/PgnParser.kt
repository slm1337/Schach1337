package com.example.schach1337

import android.util.Log
import com.example.schach1337.logic.Board
import com.example.schach1337.logic.GameState
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Castle
import com.example.schach1337.logic.moves.EnPassant
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.NormalMove
import com.example.schach1337.logic.moves.PawnPromotion

class PgnParser {
    companion object {
        private const val TAG = "PgnParser"

        private val pieceTypeToNotation = mapOf(
            PieceType.Knight to "N",
            PieceType.Bishop to "B",
            PieceType.Rook to "R",
            PieceType.Queen to "Q",
            PieceType.King to "K"
        )

        fun parsePgnToFens(pgn: String, initialFen: String? = null): List<String> {
            val moveText = extractMoveText(pgn)
            Log.d(TAG, "Extracted move text: $moveText")

            val board = if (initialFen != null) Board.initial(initialFen) else Board.initial()
            val gameState = GameState(Player.White, board)

            gameState.clearHistory()
            gameState.gameHistoryFENs.add(gameState.stateString)

            val cleanedMoveText = moveText.replace("\n", " ").replace("\\s+".toRegex(), " ")
            val movePairs = cleanedMoveText.split(" ")
                .filter { it.isNotBlank() && !it.matches("\\d+\\.{1,3}".toRegex()) && !it.matches("1-0|0-1|1/2-1/2|\\*".toRegex()) }
                .filter { isValidMoveNotation(it) }
            Log.d(TAG, "Move pairs: $movePairs")

            for ((index, moveNotation) in movePairs.withIndex()) {
                val player = if (index % 2 == 0) Player.White else Player.Black
                if (gameState.currentPlayer != player) {
                    throw IllegalStateException("Move $moveNotation does not match current player $player")
                }

                Log.d(TAG, "Processing move $moveNotation for $player")
                Log.d(TAG, "Board state before move: ${gameState.stateString}")
                val move = parseMove(moveNotation, gameState, player)
                gameState.makeMove(move)
            }

            return gameState.gameHistoryFENs
        }

        fun pgnToBoard(pgn: String, moveIndex: Int = -1, initialFen: String? = null): Board {
            val fens = parsePgnToFens(pgn, initialFen)
            val index = when {
                moveIndex < 0 || moveIndex >= fens.size -> fens.size - 1
                else -> moveIndex
            }
            val board = Board()
            board.setFromFen(fens[index])
            return board
        }

        private fun isValidMoveNotation(notation: String): Boolean {
            return notation.matches("^(?:[RNBQK][a-h]?[1-8]?x?[a-h][1-8]|[a-h](?:[1-8]?x?[a-h][1-8]|[2-7]))(=[QRBN])?[+#]?$".toRegex()) ||
                    notation == "O-O" || notation == "O-O-O"
        }

        private fun extractMoveText(pgn: String): String {
            val lines = pgn.split("\n").map { it.trim() }
            val nonMetadataLines = lines.filterNot { it.startsWith("[") && it.endsWith("]") }

            var moveText = nonMetadataLines.joinToString(" ")
                .replace("\\{.*?\\}".toRegex(), "")
                .replace("\\(.*?\\)".toRegex(), "")
                .replace("(1-0|0-1|1/2-1/2|\\*)\\s*$".toRegex(), "")
                .trim()

            moveText = moveText.replace("\\d+\\.{1,3}".toRegex(), "").trim()

            return moveText
        }

        private fun parseMove(notation: String, gameState: GameState, player: Player): Move {
            val board = gameState.board
            Log.d(TAG, "Parsing move: $notation")

            if (notation == "O-O") {
                val kingPos = board.piecePositionsFor(player).first { board[it]?.type == PieceType.King }
                return Castle(MoveType.CastleKS, kingPos)
            } else if (notation == "O-O-O") {
                val kingPos = board.piecePositionsFor(player).first { board[it]?.type == PieceType.King }
                return Castle(MoveType.CastleQS, kingPos)
            }

            val promotion = if (notation.contains("=")) {
                when (notation.last()) {
                    'Q' -> PieceType.Queen
                    'R' -> PieceType.Rook
                    'B' -> PieceType.Bishop
                    'N' -> PieceType.Knight
                    else -> throw IllegalArgumentException("Invalid promotion piece in $notation")
                }
            } else null

            var cleanedNotation = notation.replace("[+#]".toRegex(), "")
            if (promotion != null) {
                cleanedNotation = cleanedNotation.replace("=[QRBN]".toRegex(), "")
            }

            // Parse the move
            val toSquare = cleanedNotation.takeLast(2)
            val toPos = algebraicToPosition(toSquare)
            val pieceType = when {
                cleanedNotation[0].isUpperCase() && cleanedNotation[0] in "RNBQK" -> when (cleanedNotation[0]) {
                    'N' -> PieceType.Knight
                    'B' -> PieceType.Bishop
                    'R' -> PieceType.Rook
                    'Q' -> PieceType.Queen
                    'K' -> PieceType.King
                    else -> throw IllegalArgumentException("Invalid piece in $notation")
                }
                else -> PieceType.Pawn
            }

            val pieceNotation = pieceTypeToNotation[pieceType] ?: ""
            val fromHint = if (pieceType == PieceType.Pawn) {
                cleanedNotation.dropLast(2).replace("x", "")
            } else {
                val prefix = cleanedNotation.dropLast(2).replace("x", "")
                Log.d(TAG, "PieceType: $pieceType, toString: ${pieceType.toString()}, Notation: $pieceNotation, Prefix: $prefix")
                prefix.removePrefix(pieceNotation)
            }
            Log.d(TAG, "Piece: $pieceType, To: $toSquare, FromHint: $fromHint")

            val fromPositions = board.piecePositionsFor(player)
                .filter { pos ->
                    val piece = board[pos]
                    piece?.type == pieceType && piece.color == player
                }
                .filter { pos ->
                    val moves = gameState.legalMovesForPiece(pos)
                    val isLegal = moves?.any { it.toPos == toPos } == true
                    Log.d(TAG, "Checking position $pos for $pieceType: Legal moves = ${moves?.toList()}, IsLegal to $toPos = $isLegal")
                    isLegal
                }

            if (fromPositions.count() == 1) {
                val fromPos = fromPositions.first()
                Log.d(TAG, "Selected motions selected: $pieceType from $fromPos to $toPos")
                return createMove(fromPos, toPos, pieceType, promotion, board, player)
            } else if (fromPositions.count() > 1) {
                val disambiguator = fromHint.trim()
                Log.d(TAG, "Multiple positions found: $fromPositions, Disambiguator: $disambiguator")
                if (disambiguator.isEmpty()) {
                    throw IllegalArgumentException("Ambiguous move $notation: multiple pieces can move to $toSquare. Positions: $fromPositions")
                }
                val fromPos = fromPositions.firstOrNull { pos ->
                    when {
                        disambiguator.length == 1 && disambiguator[0].isLetter() ->
                            pos.column == (disambiguator[0].lowercaseChar() - 'a')
                        disambiguator.length == 1 && disambiguator[0].isDigit() ->
                            pos.row == 8 - disambiguator[0].digitToInt()
                        disambiguator.length == 2 && disambiguator[0].isLetter() && disambiguator[1].isDigit() ->
                            pos == algebraicToPosition(disambiguator)
                        else -> throw IllegalArgumentException("Invalid disambiguator $disambiguator in move $notation")
                    }
                } ?: throw IllegalArgumentException("No valid piece found for move $notation with disambiguator $disambiguator. Positions: $fromPositions")
                return createMove(fromPos, toPos, pieceType, promotion, board, player)
            } else {
                throw IllegalArgumentException("No valid piece found for move $notation. Positions checked: ${board.piecePositionsFor(player).toList()}")
            }
        }

        private fun createMove(fromPos: Position, toPos: Position, pieceType: PieceType, promotion: PieceType?, board: Board, player: Player): Move {
            if (pieceType == PieceType.Pawn && board.isEmpty(toPos) && fromPos.column != toPos.column) {
                val skipPos = board.getPawnSkipPosition(Player.opponent(player))
                if (skipPos != null && toPos == skipPos) {
                    return EnPassant(fromPos, toPos)
                }
            }

            if (promotion != null && pieceType == PieceType.Pawn) {
                return PawnPromotion(fromPos, toPos, promotion)
            }

            return NormalMove(fromPos, toPos)
        }

        private fun algebraicToPosition(algebraic: String): Position {
            if (algebraic.length != 2 || !algebraic[0].isLetter() || !algebraic[1].isDigit()) {
                throw IllegalArgumentException("Invalid algebraic notation: $algebraic")
            }
            val col = algebraic[0].lowercaseChar() - 'a'
            val row = 8 - algebraic[1].digitToInt()
            return Position(row, col)
        }
    }
}