package com.example.schach1337.logic.pieces

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.PieceType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Castle
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.NormalMove

class King : Piece {
    override var type: PieceType = PieceType.King
    override var color: Player = Player.None

    constructor(color : Player){
        this.color = color
    }

    companion object{
        private val dirs = arrayOf(
            Direction.North,
            Direction.South,
            Direction.East,
            Direction.West,
            Direction.NorthWest,
            Direction.NorthEast,
            Direction.SouthWest,
            Direction.SouthEast
        )

        private fun isUnmovedRook(pos : Position, board : Board) : Boolean{
            if(board.isEmpty(pos)) {
                return false
            }

            val piece = board[pos];
            return piece?.type == PieceType.Rook && !piece.hasMoved
        }

        private fun allEmpty(positions : Sequence<Position>, board : Board) : Boolean{
            return positions.all {pos -> board.isEmpty(pos)}
        }
    }

    private fun canCastleKingSide(from : Position, board : Board) : Boolean{
        if(hasMoved){
            return false
        }

        val rookPos = Position(from.row, 7)
        val betweenPositions = sequenceOf(Position(from.row, 5), Position(from.row, 6))
        return isUnmovedRook(rookPos, board) && allEmpty(betweenPositions, board)
    }

    private fun canCastleQueenSide(from : Position, board : Board) : Boolean{
        if(hasMoved){
            return false
        }

        val rookPos = Position(from.row, 0)
        val betweenPositions = sequenceOf(Position(from.row, 1), Position(from.row, 2), Position(from.row, 3))
        return isUnmovedRook(rookPos, board) && allEmpty(betweenPositions, board)
    }

    override fun copy(): Piece {
        val copy = King(color)
        copy.hasMoved = hasMoved
        return copy
    }

    override fun getMoves(from: Position, board: Board): Sequence<Move> = sequence{
        for(to in movePositions(from, board)){
            yield(NormalMove(from, to))
        }

        if(canCastleKingSide(from, board)){
            yield (Castle(MoveType.CastleKS, from))
        }

        if(canCastleQueenSide(from, board)){
            yield (Castle(MoveType.CastleQS, from))
        }
    }

    private fun movePositions(from : Position, board : Board) : Sequence<Position> = sequence{
        for(dir in dirs){
            val to : Position = from + dir

            if(!Board.isInside(to)){
                continue
            }

            if(board.isEmpty(to) || board[to]?.color != color){
                yield(to)
            }
        }
    }

    override fun canCaptureOpponentKing(from: Position, board: Board): Boolean {
        return movePositions(from, board).any { to ->
            val piece = board[to]
            piece != null && piece.type == PieceType.King
        }
    }

}