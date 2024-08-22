package com.example.schach1337

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.schach1337.logic.Board
import com.example.schach1337.logic.GameState
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.pieces.Piece

fun Int.toDp(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

class MainActivity : AppCompatActivity() {
    private var gameState : GameState = GameState(Player.White, Board.initial())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawActivity(gameState.board)
    }

    fun drawActivity(board : Board){
        val constraintLayout = ConstraintLayout(this)
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
                        TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f
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

        constraintLayout.addView(tableLayout)

        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.setMargins(0, 120.toDp(this), 0, 120.toDp(this))

        tableLayout.layoutParams = layoutParams

        setContentView(constraintLayout)
    }

    fun loadSourceDrawable(drawableKey: Int?) : Drawable? {
        return ResourcesCompat.getDrawable(resources, drawableKey ?: R.drawable.ic_launcher_background, null)
    }
}