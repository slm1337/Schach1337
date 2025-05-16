package com.example.schach1337

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.schach1337.logic.Board
import com.example.schach1337.logic.EndReason
import com.example.schach1337.logic.FenUtils
import com.example.schach1337.logic.GameState
import com.example.schach1337.logic.MoveType
import com.example.schach1337.logic.Player
import com.example.schach1337.logic.Position
import com.example.schach1337.logic.moves.Move
import com.example.schach1337.logic.moves.PawnPromotion
import com.example.schach1337.logic.pieces.Piece
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull


class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var menuButton: ImageButton

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MoveAdapter
    private lateinit var multiPvAdapter: MultiPvAdapter
    private lateinit var arrowOverlay: ArrowOverlayView

    private var currentIndex = 0
    private var cutIndex = 0
    private var moveEngine : Boolean = true

    private var gameState : GameState = GameState(Player.White, Board.initial())
    private lateinit var UIboard: Array<Array<ImageView?>>
    private val moveCache = mutableMapOf<Position, Move>()
    private var selectedPos : Position? = null
    private var gameVsEngine : Boolean = true


    @SuppressLint("NotifyDataSetChanged")
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
                    clearHistory()
                    gameVsEngine = true
                    restartGame()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.analyze_board -> {
                    clearHistory()
                    gameVsEngine = false

                    hideHighlights()
                    moveCache.clear()
                    gameState = GameState(Player.White, Board.initial())

                    drawBoard(gameState.board)

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

        val container = findViewById<View>(R.id.move_history_container)
        val toggleButton = findViewById<Button>(R.id.btn_toggle_history)

        toggleButton.setOnClickListener {
            if (container.visibility == View.VISIBLE) {
                container.visibility = View.GONE
                toggleButton.text = "Показать"
            } else {
                container.visibility = View.VISIBLE
                toggleButton.text = "Скрыть"
            }
        }

        recyclerView = findViewById(R.id.move_history_list)
        adapter = MoveAdapter( 0)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btn_prev_move).setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                cutIndex--
                gameState.currentPlayer = Player.opponent(gameState.currentPlayer)
                updateSelection()
            }
        }

        findViewById<Button>(R.id.btn_next_move).setOnClickListener {
            if (currentIndex < gameState.gameHistory.size - 1) {
                currentIndex++
                cutIndex++
                gameState.currentPlayer = Player.opponent(gameState.currentPlayer)
                updateSelection()
            }
        }

        updateSelection()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerMultiPv)
        val evalText = findViewById<TextView>(R.id.evalText)
        val evalBar = findViewById<ProgressBar>(R.id.evalBar)

        multiPvAdapter = MultiPvAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = multiPvAdapter

        StockfishEngine.initialize(this)
        drawBoard(gameState.board)


        val bordContainer = findViewById<FrameLayout>(R.id.chessboard_container)
        arrowOverlay = ArrowOverlayView(this)
        arrowOverlay.isClickable = false
        arrowOverlay.isFocusable = false
        arrowOverlay.isFocusableInTouchMode = false
        arrowOverlay.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        bordContainer.addView(arrowOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))


        lifecycleScope.launch {
            StockfishEngine.infoFlow.collectLatest { infoList ->
                if (!gameVsEngine) {
                    multiPvAdapter.updateData(infoList)

                    arrowOverlay.clearArrows()
                    val cellSize = container.width / 8f

                    val sortedInfoList = if (gameState.currentPlayer == Player.White) {
                        infoList.sortedByDescending { (it.score as? Float) ?: 0f }
                    } else {
                        infoList.sortedBy { (it.score as? Float) ?: 0f }
                    }

                    sortedInfoList.forEachIndexed { index, info ->
                        val bestMove = info.bestMove
                        if (bestMove.length == 4) {
                            val start = bestMove.substring(0, 2)
                            val end = bestMove.substring(2, 4)

                            val (startFile, startRank) = convertChessCoordToIndex(start)
                            val (endFile, endRank) = convertChessCoordToIndex(end)

                            val startX = (startFile - 1) * cellSize + cellSize / 2
                            val startY = (8 - startRank) * cellSize + cellSize / 2
                            val endX = (endFile - 1) * cellSize + cellSize / 2
                            val endY = (8 - endRank) * cellSize + cellSize / 2

                            arrowOverlay.addArrow(startX, startY, endX, endY, index + 1)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            StockfishEngine.evalFlow.collectLatest { eval ->
                if(!gameVsEngine) {
                    eval?.let {
                        evalText.text = "%.2f".format(it)
                        evalBar.progress = ((it + 10) * 5).toInt().coerceIn(0, 100)
                    }
                }
            }
        }

    }

    private fun convertChessCoordToIndex(coord: String): Pair<Int, Int> {
        val file = coord[0] - 'a' + 1   // 'a' -> 1, 'b' -> 2, ..., 'h' -> 8
        val rank = coord[1].digitToInt()  // '1' -> 1, ..., '8' -> 8
        return Pair(file, rank)
    }

    private fun clearHistory(){
        gameState.clearHistory()
        currentIndex = 0
        cutIndex = 0
        updateMoveHistory()
    }

    private fun applyFen(gameHistory: String) {
        gameState.board = Board.initial(gameHistory)
        drawBoard(gameState.board)
    }

    private fun updateSelection() {
        adapter.setSelectedIndex(currentIndex)
        recyclerView.smoothScrollToPosition(currentIndex)
        applyFen(gameState.gameHistoryFENs[currentIndex])
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

        // Удаляем только шахматную доску, но не overlay
        if (container.childCount > 0 && container.getChildAt(0) is TableLayout) {
            container.removeViewAt(0)
        }

        // Добавляем доску под стрелками
        container.addView(tableLayout, 0) // Добавляем доску в самый нижний слой
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
        }
    }

    @Synchronized
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
                updateMoveHistory()

                if(!gameVsEngine){
                    StockfishEngine.analyzePosition(gameState.stateString, depthLimit = 20)
                }

                if (moveEngine){
                    moveEngine = false
                    makeAnOpponentMove()
                } else {
                    moveEngine = true
                }

            }

        }
    }

    suspend fun awaitBestMove(minDepth: Int = 20, timeoutMs: Long = 30_000): String? {
        return withTimeoutOrNull(timeoutMs) {
            StockfishEngine.infoFlow
                .filter { list -> list.isNotEmpty() && list.firstOrNull()?.depth ?: 0 >= minDepth }
                .first()
                .firstOrNull()
                ?.bestMove
        }
    }

    private fun makeAnOpponentMove(){
        if(!gameVsEngine){
            return;
        }

        lifecycleScope.launch {
            try {
//                val bestMove = awaitBestMove()
//                if (bestMove == null) {
//                    println("Нет хода — анализ не завершён вовремя")
//                    return@launch
//                }

                val (bestMove, eval) = StockfishEngine.getBestMoveAndEval(gameState.stateString)


                val fromCol = bestMove[0] - 'a'
                val fromRow = 8 - (bestMove[1] - '0')
                val toCol = bestMove[2] - 'a'
                val toRow = 8 - (bestMove[3] - '0')

                val fromPos = Position(fromRow, fromCol)
                val toPos = Position(toRow, toCol)

                withContext(Dispatchers.Main) {
                    onFromPositionSelected(fromPos)
                    onTopPositionSelected(toPos)
                    updateEvaluation(eval)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private fun makeAnOpponentMove(fen : String){
        if(!gameVsEngine){
            return;
        }

        lifecycleScope.launch {
            try {
//                val bestMove = awaitBestMove()
//                if (bestMove == null) {
//                    println("Нет хода — анализ не завершён вовремя")
//                    return@launch
//                }
//
//                println("Stockfish recommends: $bestMove")

                val (bestMove, eval) = StockfishEngine.getBestMoveAndEval(gameState.stateString)

                if (bestMove.length < 4) {
                    return@launch
                }

                val fromCol = bestMove[0] - 'a'
                val fromRow = 8 - (bestMove[1] - '0')
                val toCol = bestMove[2] - 'a'
                val toRow = 8 - (bestMove[3] - '0')

                val fromPos = Position(fromRow, fromCol)
                val toPos = Position(toRow, toCol)

                withContext(Dispatchers.Main) {
                    onFromPositionSelected(fromPos)
                    onTopPositionSelected(toPos)
                    updateEvaluation(eval)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleMove(move : Move){
        gameState.makeMove(move)

        if(move.type == MoveType.EnPassant || move.type == MoveType.CastleKS || move.type == MoveType.CastleQS){
            drawBoard(gameState.board)
        } else{

            val oldPos = UIboard[move.fromPos.row][move.fromPos.column]
            val newPos = UIboard[move.toPos.row][move.toPos.column]

            //runOnUiThread {
                newPos?.setImageDrawable(oldPos?.drawable)
                oldPos?.setImageDrawable(loadSourceDrawable(R.drawable.ic_blank))
            //}
        }


        if(gameState.isGameOver()){
            showGameOver()
        }
    }

    fun updateMoveHistory() {
        currentIndex = gameState.gameHistory.lastIndex
        if (currentIndex != currentIndex+cutIndex) {
            val cutStart = currentIndex + cutIndex

            val leftPart = gameState.gameHistory.subList(0, cutStart)
            val rightPart = listOf(gameState.gameHistory.last())
            gameState.gameHistory = (leftPart + rightPart).toMutableList()

            val leftFENs = gameState.gameHistoryFENs.subList(0, cutStart)
            val rightFEN = listOf(gameState.gameHistoryFENs.last())
            gameState.gameHistoryFENs = (leftFENs + rightFEN).toMutableList()

            currentIndex += cutIndex
            cutIndex = 0
        }
        val moves = gameState.gameHistory
        adapter.updateMoves(moves, currentIndex)
        recyclerView.scrollToPosition(currentIndex)
    }

    fun updateEvaluation(eval: Any?) {
        val evalText = findViewById<TextView>(R.id.evalText)
        val evalBar = findViewById<ProgressBar>(R.id.evalBar)

        runOnUiThread {
            if (eval is String && eval.startsWith("mate")) {
                val mateValue = eval.split(" ").last().toInt()
                evalText.text = "Mate in ${if (mateValue > 0) mateValue else -mateValue}"
                evalBar.progress = if (mateValue > 0) 100 else 0
            } else if (eval is Float) {
                val clampedEval = eval.coerceIn(-10f, 10f)
                val progress = (((clampedEval + 10f) / 20f) * 100f).toInt()

                evalText.text = String.format("%.2f", clampedEval)
                evalBar.progress = progress
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

        val dialog = PlayChessMenu()
        dialog.listener = object : PlayChessMenu.OnSettingsConfirmedListener {
            override fun onSettingsConfirmed(
                playAsWhite: Boolean,
                isLevel : Boolean, eloOrLevelLimitValue: Int,
                isTime: Boolean, depthOrTimeLimitValue: Int,
                startPos: String
            ) {
                startGameWithSettings(playAsWhite, isLevel, eloOrLevelLimitValue, isTime, depthOrTimeLimitValue, startPos)
            }
        }
        dialog.show(supportFragmentManager, "PlayChessMenu")
    }

    private fun startGameWithSettings(playAsWhite: Boolean,
                                      isLevel : Boolean, eloOrLevelLimitValue: Int,
                                      isTime: Boolean, depthOrTimeLimitValue: Int,
                                      startPos: String) {

        val player : Player = if(playAsWhite){
            Player.White
        } else {
            Player.Black
        }

        if(isLevel){
            StockfishEngine.skillLevel = eloOrLevelLimitValue
            StockfishEngine.skillElo = null
        } else {
            StockfishEngine.skillElo = eloOrLevelLimitValue
            StockfishEngine.skillLevel = null
        }

        if (isTime){
            StockfishEngine.searchTime = depthOrTimeLimitValue
            StockfishEngine.depth = null
        } else {
            StockfishEngine.depth = depthOrTimeLimitValue
            StockfishEngine.searchTime = null
        }

        StockfishEngine.applyEngineSettings()

        var startPosition = startPos;
        if (!FenUtils.isValidFEN(startPos)) {
            startPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        }
        val board : Board = Board.initial(startPosition)

        gameState = GameState(
            if (FenUtils.isValidFEN(startPosition) && FenUtils.currentPlayer(startPosition) != player)
                Player.opponent(player) else player,
            board
        )

        drawBoard(gameState.board)

        if (FenUtils.isValidFEN(startPosition) && FenUtils.currentPlayer(startPosition) != player) {
            lifecycleScope.launch {
                //makeAnOpponentMove(startPosition)
            }
        }
    }

    private fun openAnalyzeGameWindow() {
        return
    }

    private fun openSettings() {
        return
    }
}