package com.example.schach1337.logic.moves

import com.example.schach1337.logic.Board
import com.example.schach1337.logic.Direction
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.pieces.Piece

class Castle : Move {
    override var type: MoveType
    override var fromPos: Position
    override lateinit var toPos: Position
    private lateinit var kingMoveDir : Direction;
    private lateinit var rookFromPos : Position;
    private lateinit var rookToPos : Position;

    constructor(type : MoveType, kingPos : Position){
        this.type = type;
        fromPos = kingPos;

        if(type == MoveType.CastleKS){
            kingMoveDir = Direction.East
            toPos = Position(kingPos.row, 6)
            rookFromPos = Position(kingPos.row, 7)
            rookToPos = Position(kingPos.row, 5)
        } else if(type == MoveType.CastleQS){
            kingMoveDir = Direction.West
            toPos = Position(kingPos.row, 2)
            rookFromPos = Position(kingPos.row, 0)
            rookToPos = Position(kingPos.row, 3)
        }
    }

    override fun execute(board: Board) {
        NormalMove(fromPos, toPos).execute(board)
        NormalMove(rookFromPos, rookToPos).execute(board)
    }

    override fun isLegal(board: Board): Boolean {
        val player = board[fromPos]!!.color

        if(board.isInCheck(player)){
            return false
        }

        val copy = board.copy()
        var kingPosInCopy = fromPos;

        for(i in 0..2){
            NormalMove(kingPosInCopy, kingPosInCopy + kingMoveDir).execute(copy)
            kingPosInCopy += kingMoveDir

            if(copy.isInCheck(player)){
                return false
            }
        }

        return true
    }
}