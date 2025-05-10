package com.example.schach1337

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.schach1337.logic.Board
import com.example.schach1337.logic.EndReason
import com.example.schach1337.logic.GameState
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.PawnPromotion
import com.example.schach1337.logic.pieces.Piece
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var menuButton: ImageButton

    private var gameState : GameState = GameState(Player.White, Board.initial())
    private lateinit var UIboard: Array<Array<ImageView?>>
    private val moveCache = mutableMapOf<Position, Move>()
    private var selectedPos : Position? = null
    private var gameVsEngine : Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        StockfishEngine.initialize(this@MainActivity)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        menuButton = findViewById(R.id.menu_button)

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.play_chess -> {
                    gameVsEngine = true
                    restartGame()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.analyze_board -> {
                    gameVsEngine = false
                    restartGame()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.analyze_game -> {
                    gameVsEngine = false
                    openAnalyzeGameWindow();
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.settings -> {
                    openSettings();
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                else -> false
            }
        }

        drawBoard(gameState.board)
    }

    override fun onDestroy() {
        StockfishEngine.close()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawBoard(board: Board) {
        val tableLayout: TableLayout = initialBoard(board)
        tableLayout.setBackgroundResource(R.drawable.board)

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        tableLayout.layoutParams = layoutParams

        tableLayout.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX
                val y = event.rawY
                boardMousedown(view, x, y)
            }
            true
        }

        val container = findViewById<FrameLayout>(R.id.chessboard_container)
        container.removeAllViews()
        container.addView(tableLayout)
    }

    private fun initialBoard(board : Board) : TableLayout{
        val tableLayout = TableLayout(this)

        UIboard = Array(8) { arrayOfNulls<ImageView>(8) }

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

                UIboard[r][c] = imageView

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
        return ResourcesCompat.getDrawable(resources,
            drawableKey ?: R.drawable.ic_highlight_green, null)
    }

    private fun cacheMoves(moves : Sequence<Move>){
        moveCache.clear()

        for(move in moves){
            moveCache[move.toPos] = move
        }
    }

    private fun boardMousedown(view: View, x: Float, y: Float) {
        val squareSize : Double = (view.width / 8).toDouble()

        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val yLocView= viewLocation[1].toFloat()

        val row : Int = ((y.toInt() - yLocView) / squareSize).toInt()
        val col : Int = (x.toInt() / squareSize).toInt()
        val pos = Position(row, col)

        if(selectedPos == null){
            onFromPositionSelected(pos)
        }else{
            onTopPositionSelected(pos)
            moveCache[pos]?.let {
                val handler = android.os.Handler()
                handler.postDelayed({
                    makeAnOpponentMove()
                }, 1)
            }
        }
    }

    private fun makeAnOpponentMove(){
        if(!gameVsEngine){
            return;
        }

        val bestMove = StockfishEngine.getBestMove(gameState.stateString)

        val fromCol = bestMove[0] - 'a'
        val fromRow = 8 - (bestMove[1] - '0')
        val toCol = bestMove[2] - 'a'
        val toRow = 8 - (bestMove[3] - '0')

        val fromPos = Position(fromRow, fromCol)
        val toPos = Position(toRow, toCol)

        onFromPositionSelected(fromPos)
        onTopPositionSelected(toPos)
    }

    private fun onFromPositionSelected(pos : Position){
        val moves : Sequence<Move> = gameState.legalMovesForPiece(pos) ?: return

        if(moves.any()){
            selectedPos = pos
            cacheMoves(moves)
            showHighlights()
        }
    }

    private fun onTopPositionSelected(pos : Position){
        selectedPos = null
        hideHighlights()

        moveCache[pos]?.let { move ->
            if (move.type == MoveType.PawnPromotion) {
                handlePromotion(move.fromPos, move.toPos)
            } else {
                handleMove(move)
            }
        }
    }

    private fun handlePromotion(from : Position, to : Position){
        val oldPos = UIboard[to.row][to.column]
        val newPos = UIboard[from.row][from.column]

        val pawnPromotionsMenu = PawnPromotionsMenu(this)

        pawnPromotionsMenu.setOnPromotionSelected { selectedPiece ->
            val promPieceResId : Int? = Images.getImage(gameState.currentPlayer, selectedPiece)
            val promPieceDrawable = promPieceResId?.let { ContextCompat.getDrawable(this, it) }
            newPos?.setImageDrawable(promPieceDrawable)
            oldPos?.setImageDrawable(loadSourceDrawable(R.drawable.ic_blank))

            val promMove = PawnPromotion(from, to, selectedPiece)
            handleMove(promMove)
        }

        pawnPromotionsMenu.show()
    }

    private fun handleMove(move : Move){
        gameState.makeMove(move)

        if(move.type == MoveType.EnPassant || move.type == MoveType.CastleKS || move.type == MoveType.CastleQS){
            drawBoard(gameState.board)
        } else{
            val oldPos = UIboard[move.fromPos.row][move.fromPos.column]
            val newPos = UIboard[move.toPos.row][move.toPos.column]

            newPos?.setImageDrawable(oldPos?.drawable)
            oldPos?.setImageDrawable(loadSourceDrawable(R.drawable.ic_blank))
        }

        if(gameState.isGameOver()){
            showGameOver()
        }
    }

    private fun showHighlights(){
        for(to in moveCache.keys){
            val piece : Piece? = gameState.board[to.row, to.column]
            val img : ImageView? = UIboard[to.row][to.column]

            if(piece != null){
                img?.setBackgroundResource(R.drawable.ic_highlight_red)
            } else {
                img?.setBackgroundResource(R.drawable.ic_highlight_green)
            }
        }
    }

    private fun hideHighlights(){
        for(r in 0..7){
            for(c in 0..7){
                val img = UIboard[r][c]
                img?.setBackgroundResource(0)
            }
        }
    }

    fun getWinnerText(winner : Player) : String{
        return when(winner){
            Player.White -> "WHITE WINS!"
            Player.Black -> "BLACK WINS!"
            else -> "IT'S A DRAW"
        }
    }

    fun PlayerString(player : Player) : String{
        return when(player){
            Player.White -> "WHITE"
            Player.Black -> "BLACK"
            else -> ""
        }

    }

    fun getReasonText(reason : EndReason, currentPlayer : Player) : String{
        return when(reason){
            EndReason.Stalemate -> "STALEMATE - ${PlayerString(currentPlayer)} CAN'T MOVE"
            EndReason.Checkmate -> "CHECKMATE - ${PlayerString(currentPlayer)} CAN'T MOVE"
            EndReason.FiftyMoveRule -> "FIFTY-MOVE RULE"
            EndReason.InsufficientMaterial -> "INSUFFICIENT MATERIAL"
            EndReason.ThreefoldRepetition -> "THREEFOLD REPETITION"
        }
    }

    private fun showGameOver(){
        val gameOverMenu = GameOverMenu(this)
        val result = gameState.result!!
        gameOverMenu.setWinnerText(getWinnerText(result.winner))
        gameOverMenu.setReasonText(getReasonText(result.reason, gameState.currentPlayer))
        gameOverMenu.show()

        gameOverMenu.setRestart(object : GameOverMenu.RestartClick{
            override fun onRestartClick() {
                gameOverMenu.cancel()
                restartGame()
            }
        })

        gameOverMenu.setClose(object : GameOverMenu.CloseClick{
            override fun onCloseClick() {
                gameOverMenu.cancel()
            }
        })

        gameOverMenu.setAnalyze(object : GameOverMenu.AnalyzeClick{
            override fun onAnalyzeClick() {
                TODO("Not yet implemented")
            }
        })
    }

    private fun restartGame(){
        hideHighlights()
        moveCache.clear()
        gameState = GameState(Player.White, Board.initial())
        drawBoard(gameState.board)
    }

    private fun openAnalyzeGameWindow() {
        return
    }

    private fun openSettings() {
        return
    }
}