package com.example.schach1337.logic

import com.example.schach1337.logic.pieces.Piece

class StateString {
    private var sb = StringBuilder()

    constructor(currentPlayer : Player, board : Board){
        addPiecePlacement(board)
        sb.append(' ')
        addCurrentPlayer(currentPlayer)
        sb.append(' ')
        addCastlingRights(board)
        sb.append(' ')
        addEnPassant(board, currentPlayer)
    }

    override fun toString(): String {
        return sb.toString()
    }

    companion object{
        private fun pieceChar(piece : Piece) : Char{
            val c = when(piece.type){
                PieceType.Pawn -> 'p'
                PieceType.Knight -> 'n'
                PieceType.Rook -> 'r'
                PieceType.Bishop -> 'b'
                PieceType.Queen -> 'q'
                PieceType.King -> 'k'
                else -> ' '
            }

            if(piece.color == Player.White){
                return c.uppercaseChar()
            }

            return c
        }
    }

    private fun addRowData(board : Board, row : Int){
        var empty  : Int = 0

        for(c in 0..7){
            if(board[row, c] == null){
                empty++;
                continue
            }

            if(empty > 0){
                sb.append(empty)
                empty = 0
            }

            sb.append(pieceChar(board[row, c]!!))
        }

        if(empty > 0){
            sb.append(empty)
        }
    }

    private fun addPiecePlacement(board : Board){
        for(r in 0..7){
            if(r != 0){
                sb.append('/')
            }

            addRowData(board, r)
        }
    }

    private fun addCurrentPlayer(currentPlayer : Player){
        if(currentPlayer == Player.White){
            sb.append('w')
        } else{
            sb.append('b')
        }
    }

    private fun addCastlingRights(board : Board){
        val castleWKS = board.castleRightKS(Player.White)
        val castleWQS = board.castleRightQS(Player.White)
        val castleBKS = board.castleRightKS(Player.Black)
        val castleBQS = board.castleRightQS(Player.Black)

        if(!(castleWKS || castleWQS || castleBKS || castleBQS)){
            sb.append("-")
            return
        }
        if(castleWKS){
            sb.append("K")
        }
        if(castleWQS){
            sb.append("Q")
        }
        if(castleBKS){
            sb.append('k')
        }
        if(castleBQS){
            sb.append('q')
        }
    }

    private fun addEnPassant(board : Board, currentPlayer : Player){
        if(!board.canCaptureEnPassant(currentPlayer)){
            sb.append('-')
            return
        }

        val pos = board.getPawnSkipPosition(Player.opponent(currentPlayer)) ?: return
        val file : Char = 'a' + pos.column
        val rank : Int = 8 - pos.row
        sb.append(file)
        sb.append(rank)
    }
}