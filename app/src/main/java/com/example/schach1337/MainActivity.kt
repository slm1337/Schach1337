package com.example.schach1337

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.schach1337.logic.Board
import com.example.schach1337.logic.GameState
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.pieces.Piece


fun Int.toDp(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

class MainActivity : AppCompatActivity() {
    private val gameState : GameState = GameState(Player.White, Board.initial())
    private val moveCache = mutableMapOf<Position, Move>()
    private var selectedPos : Position? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawActivity(gameState.board)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawActivity(board : Board) {
        val constraintLayout = ConstraintLayout(this)
        val tableLayout : TableLayout = initialBoard(board)

        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ).apply {
            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        layoutParams.setMargins(0, 120.toDp(this), 0, 120.toDp(this))

        tableLayout.layoutParams = layoutParams

        tableLayout.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX
                val y = event.rawY
                boardMousedown(view, x, y)
            }
            true
        }

        constraintLayout.addView(tableLayout)
        setContentView(constraintLayout)
    }

    private fun initialBoard(board : Board) : TableLayout{
        val tableLayout = TableLayout(this)
        tableLayout.setBackgroundResource(R.drawable.board)

        for(r in 0..7){
            val tableRow = TableRow(this)

            for(c in 0..7){
                val piece : Piece? = board[r, c]
                val imageView = ImageView(this)

                if(piece == null){
                    imageView.setImageDrawable(loadSourceDrawable(R.drawable.ic_blank))
                }
                else{
                    imageView.setImageDrawable(loadSourceDrawable(Images.getImage(piece)))
                }

                tableRow.addView(
                    imageView, TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                )
            }

            tableLayout.addView(
                tableRow, TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }

        return tableLayout
    }

    private fun loadSourceDrawable(drawableKey: Int?) : Drawable? {
        return ResourcesCompat.getDrawable(resources, drawableKey ?: R.drawable.ic_launcher_background, null)
    }

    private fun cacheMoves(moves : Sequence<Move>){
        moveCache.clear()

        for(move in moves){
            moveCache[move.toPos] = move
        }
    }

    private fun boardMousedown(view: View, x: Float, y: Float) {
        val squareSizeWeight : Double = (view.width / 8).toDouble()
        val squareSizeHeight : Double = (view.height / 8).toDouble()

        val row : Int = ((y.toInt() - 571) / squareSizeHeight).toInt() // TODO: 571 - magic number
        val col : Int = (x.toInt() / squareSizeWeight).toInt()
        val pos = Position(row, col)


        if(selectedPos == null){
            onFromPositionSelected(pos)
        }else{
            onTopPositionSelected(pos)
        }
    }

    private fun onFromPositionSelected(pos : Position){
        val moves : Sequence<Move> = gameState.legalMovesForPiece(pos) ?: return

        if(moves.any()){
            selectedPos = pos
            cacheMoves(moves)
        }
    }


    private fun onTopPositionSelected(pos : Position){
        selectedPos = null

        val move = moveCache[pos]
        if(move != null){
            handleMove(move)
        }
    }

    private fun handleMove(move : Move){
        gameState.makeMove(move)
        drawActivity(gameState.board)       // TODO: полная отрисовка активити с нуля
    }


}