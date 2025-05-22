package com.example.schach1337

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.schach1337.databinding.ActivityMainBinding
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), PgnImportDialogFragment.PgnImportListener {
    private var binding: ActivityMainBinding? = null
    private lateinit var adapter: MoveAdapter
    private lateinit var multiPvAdapter: MultiPvAdapter
    private lateinit var arrowOverlay: ArrowOverlayView

    private var currentIndex = 0
    private var cutIndex = 0
    private var isAnalysisVisible = true
    private var isPaused = false
    private var moveEngine: Boolean = false
    private var gameVsEngine: Boolean = false
    private var gameState: GameState = GameState(Player.White, Board.initial())
    private lateinit var UIboard: Array<Array<ImageView?>>
    private val moveCache = mutableMapOf<Position, Move>()
    private var selectedPos: Position? = null

    private var moveAnalyses: MutableList<MoveAnalysis?> = mutableListOf()

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_CUT_INDEX = "cut_index"
        private const val KEY_MOVE_ENGINE = "move_engine"
        private const val KEY_GAME_VS_ENGINE = "game_vs_engine"
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        StockfishEngine.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX, 0)
            cutIndex = savedInstanceState.getInt(KEY_CUT_INDEX, 0)
            moveEngine = savedInstanceState.getBoolean(KEY_MOVE_ENGINE, true)
            gameVsEngine = savedInstanceState.getBoolean(KEY_GAME_VS_ENGINE, true)
        }

        binding!!.menuButton.setOnClickListener {
            binding!!.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding!!.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.play_chess -> {
                    binding!!.recyclerMultiPv.visibility = View.INVISIBLE
                    binding!!.multipvButtonsContainer.visibility = View.INVISIBLE
                    StockfishEngine.stop()
                    StockfishEngine.sendCommand("stop")
                    arrowOverlay.clearAnalysisArrows()
                    clearHistory()
                    gameVsEngine = true
                    isPaused = false
                    binding!!.btnGoPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0)
                    arrowOverlay.clearHintArrow()
                    restartGame()
                    binding!!.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.analyze_board -> {
                    binding!!.recyclerMultiPv.visibility = View.VISIBLE
                    binding!!.multipvButtonsContainer.visibility = View.VISIBLE
                    clearHistory()
                    gameVsEngine = false
                    updateButtonVisibility()
                    hideHighlights()
                    moveCache.clear()
                    gameState = GameState(Player.White, Board.initial())
                    drawBoard(gameState.board)
                    binding!!.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.import_game_pgn -> {
                    gameVsEngine = false
                    binding!!.drawerLayout.closeDrawer(GravityCompat.START)
                    val dialog = PgnImportDialogFragment.newInstance()
                    dialog.setPgnImportListener(this)
                    dialog.show(supportFragmentManager, "PgnImportDialog")
                    true
                }
                R.id.settings -> {
                    openSettings()
                    binding!!.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        adapter = MoveAdapter(currentIndex)
        binding!!.moveHistoryList.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding!!.moveHistoryList.adapter = adapter

        binding!!.btnPrevMove.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                cutIndex--
                gameState.currentPlayer = Player.opponent(gameState.currentPlayer)
                updateSelection()
            }
        }

        binding!!.btnNextMove.setOnClickListener {
            if (currentIndex < gameState.gameHistory.size - 1) {
                currentIndex++
                cutIndex++
                gameState.currentPlayer = Player.opponent(gameState.currentPlayer)
                updateSelection()
            }
        }

        binding!!.btnGoAnalyse.setOnClickListener {
            val analysePosition = gameState.gameHistoryFENs
            lifecycleScope.launch {
                val analysis = GameAnalysis(analysePosition, this@MainActivity)
                analysis.analyzeGame()
                moveAnalyses.clear()
                moveAnalyses.add(null)
                moveAnalyses.addAll(analysis.getMoveAnalyses())
                updateMoveHistory()
                updateLastMoveArrow()
            }
        }

        multiPvAdapter = MultiPvAdapter()
        binding!!.recyclerMultiPv.layoutManager = LinearLayoutManager(this)
        binding!!.recyclerMultiPv.adapter = multiPvAdapter

        drawBoard(gameState.board)

        arrowOverlay = ArrowOverlayView(this).apply {
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        binding!!.chessboardContainer.addView(
            arrowOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        lifecycleScope.launch {
            StockfishEngine.infoFlow.collectLatest { infoList ->
                if (!gameVsEngine) {
                    multiPvAdapter.updateData(infoList)
                    arrowOverlay.clearAnalysisArrows()
                    val cellSize = binding!!.chessboardContainer.width / 8f
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
                            arrowOverlay.addAnalysisArrow(startX, startY, endX, endY, index + 1)
                        }
                    }
                    updateLastMoveArrow()
                }
            }
        }

        lifecycleScope.launch {
            StockfishEngine.evalFlow.collectLatest { eval ->
                if (!gameVsEngine) {
                    eval?.let {
                        binding!!.evalText.text = "%.2f".format(it)
                        binding!!.evalBar.progress = ((it + 10) * 5).toInt().coerceIn(0, 100)
                    }
                }
            }
        }

        lifecycleScope.launch {
            DataStoreManager.loadEngineSettings(this@MainActivity)
            val savedGameState = DataStoreManager.loadGameState(this@MainActivity)
            if (savedGameState != null) {
                gameState = savedGameState
                currentIndex = gameState.gameHistory.lastIndex
                drawBoard(gameState.board)
                if (gameVsEngine && FenUtils.currentPlayer(gameState.stateString) != Player.White) {
                    makeAnOpponentMove()
                }
            } else {
                drawBoard(gameState.board)
                updateMoveHistory()
            }
        }

        binding!!.btnHint.setOnClickListener {
            if (gameVsEngine) {
                lifecycleScope.launch {
                    try {
                        val (bestMove, _) = StockfishEngine.getBestMoveAndEval(gameState.gameHistoryFENs.last())
                        if (bestMove.length >= 4) {
                            val (startFile, startRank) = convertChessCoordToIndex(bestMove.substring(0, 2))
                            val (endFile, endRank) = convertChessCoordToIndex(bestMove.substring(2, 4))
                            val cellSize = binding!!.chessboardContainer.width / 8f
                            val startX = (startFile - 1) * cellSize + cellSize / 2
                            val startY = (8 - startRank) * cellSize + cellSize / 2
                            val endX = (endFile - 1) * cellSize + cellSize / 2
                            val endY = (8 - endRank) * cellSize + cellSize / 2
                            arrowOverlay.addHintArrow(startX, startY, endX, endY)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        updateButtonVisibility()
        updateMultiPvLabel()

        binding!!.showHideAnalyse.setOnClickListener {
            isAnalysisVisible = !isAnalysisVisible
            if (isAnalysisVisible) {
                binding!!.multipvButtonsContainer.visibility = View.VISIBLE
                binding!!.recyclerMultiPv.visibility = View.VISIBLE
                binding!!.evalBar.visibility = View.VISIBLE
                binding!!.evalText.visibility = View.VISIBLE
                binding!!.showHideAnalyse.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)
                if (!isPaused) {
                    StockfishEngine.analyzePosition(gameState.gameHistoryFENs.last())
                }
            } else {
                binding!!.multipvButtonsContainer.visibility = View.INVISIBLE
                binding!!.recyclerMultiPv.visibility = View.GONE
                binding!!.evalBar.visibility = View.GONE
                binding!!.evalText.visibility = View.GONE
                binding!!.showHideAnalyse.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_off_visibility, 0)
                StockfishEngine.sendCommand("stop")
                StockfishEngine.stop()
                arrowOverlay.clearAnalysisArrows()
            }
        }

        binding!!.btnGoPause.setOnClickListener {
            isPaused = !isPaused
            if (isPaused) {
                binding!!.btnGoPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0)
                StockfishEngine.sendCommand("stop")
                StockfishEngine.stop()
            } else {
                binding!!.btnGoPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0)
                if (isAnalysisVisible) {
                    StockfishEngine.analyzePosition(gameState.gameHistoryFENs.last())
                }
            }
        }

        binding!!.btnIncreaseMultipv.setOnClickListener {
            StockfishEngine.multiPv += 1
            updateMultiPvLabel()
            if (!isPaused && isAnalysisVisible) {
                StockfishEngine.analyzePosition(gameState.gameHistoryFENs.last())
            }
        }

        binding!!.btnDecreaseMultipv.setOnClickListener {
            if (StockfishEngine.multiPv > 1) {
                StockfishEngine.sendCommand("stop")
                StockfishEngine.stop()

                StockfishEngine.multiPv -= 1
                updateMultiPvLabel()
                if (!isPaused && isAnalysisVisible) {
                    StockfishEngine.analyzePosition(gameState.gameHistoryFENs.last())
                }
            }
        }
    }

    private fun updateMultiPvLabel() {
        binding!!.multipvLabel.text = "MultiPV List (Number of lines: ${StockfishEngine.multiPv})"
    }

    private fun updateButtonVisibility() {
        binding!!.btnHint.visibility = if (gameVsEngine) View.VISIBLE else View.GONE
        binding!!.btnGoPause.visibility = if (!gameVsEngine) View.VISIBLE else View.GONE
        if (gameVsEngine) {
            arrowOverlay.clearHintArrow()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
        outState.putInt(KEY_CUT_INDEX, cutIndex)
        outState.putBoolean(KEY_MOVE_ENGINE, moveEngine)
        outState.putBoolean(KEY_GAME_VS_ENGINE, gameVsEngine)
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            DataStoreManager.saveEngineSettings(this@MainActivity)
            DataStoreManager.saveGameState(this@MainActivity, gameState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        StockfishEngine.close()
        binding = null
    }

    override fun onPgnImported(pgnText: String) {
        val fens = PgnParser.parsePgnToFens(pgnText)
        val board = Board()
        board.setFromFen(fens.last())
        gameState = GameState(Player.White, board)
        drawBoard(board)
        gameState.gameHistoryFENs = fens.toMutableList()
        gameState.updateMoveHistoryFromFENs()
        updateMoveHistory()
        updateLastMoveArrow()
    }

    private fun convertChessCoordToIndex(coord: String): Pair<Int, Int> {
        val file = coord[0] - 'a' + 1
        val rank = coord[1].digitToInt()
        return Pair(file, rank)
    }

    private fun clearHistory() {
        gameState.clearHistory()
        currentIndex = 0
        cutIndex = 0
        moveAnalyses.clear()
        updateMoveHistory()
        arrowOverlay.clearMoveArrow()
    }

    private fun applyFen(gameHistory: String) {
        gameState.board = Board.initial(gameHistory)
        drawBoard(gameState.board)
        updateLastMoveArrow()
    }

    private fun updateSelection() {
        if (currentIndex >= gameState.gameHistoryFENs.size) {
            currentIndex = gameState.gameHistoryFENs.lastIndex
        }
        if (currentIndex < 0) {
            currentIndex = 0
        }
        adapter.setSelectedIndex(currentIndex)
        binding!!.moveHistoryList.smoothScrollToPosition(currentIndex)
        applyFen(gameState.gameHistoryFENs[currentIndex])
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
        if (binding!!.chessboardContainer.childCount > 0 && binding!!.chessboardContainer.getChildAt(0) is TableLayout) {
            binding!!.chessboardContainer.removeViewAt(0)
        }
        binding!!.chessboardContainer.addView(tableLayout, 0)
    }

    private fun initialBoard(board: Board): TableLayout {
        val tableLayout = TableLayout(this)
        UIboard = Array(8) { arrayOfNulls<ImageView>(8) }
        for (r in 0..7) {
            val tableRow = TableRow(this)
            for (c in 0..7) {
                val piece: Piece? = board[r, c]
                val imageView = ImageView(this)
                imageView.setImageDrawable(
                    if (piece == null) {
                        loadSourceDrawable(R.drawable.ic_blank)
                    } else {
                        loadSourceDrawable(Images.getImage(piece))
                    }
                )
                UIboard[r][c] = imageView
                tableRow.addView(
                    imageView,
                    TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                )
            }
            tableLayout.addView(
                tableRow,
                TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }
        return tableLayout
    }

    private fun loadSourceDrawable(drawableKey: Int?): Drawable? {
        return ResourcesCompat.getDrawable(
            resources,
            drawableKey ?: R.drawable.ic_highlight_green,
            null
        )
    }

    private fun cacheMoves(moves: Sequence<Move>) {
        moveCache.clear()
        for (move in moves) {
            moveCache[move.toPos] = move
        }
    }

    private fun boardMousedown(view: View, x: Float, y: Float) {
        val squareSize: Double = (view.width / 8).toDouble()
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val yLocView = viewLocation[1].toFloat()
        val row: Int = ((y - yLocView) / squareSize).toInt()
        val col: Int = (x / squareSize).toInt()
        val pos = Position(row, col)
        if (selectedPos == null) {
            onFromPositionSelected(pos)
        } else {
            onTopPositionSelected(pos)
        }
    }

    @Synchronized
    private fun onFromPositionSelected(pos: Position) {
        val moves: Sequence<Move> = gameState.legalMovesForPiece(pos) ?: return
        if (moves.any()) {
            selectedPos = pos
            cacheMoves(moves)
            showHighlights()
        }
    }

    private fun onTopPositionSelected(pos: Position) {
        selectedPos = null
        hideHighlights()
        moveCache[pos]?.let { move ->
            if (move.type == MoveType.PawnPromotion) {
                handlePromotion(move.fromPos, move.toPos)
            } else {
                handleMove(move)
                updateMoveHistory()
                updateLastMoveArrow()
                if (!gameVsEngine) {
                    StockfishEngine.analyzePosition(gameState.stateString)
                }
                if (moveEngine) {
                    moveEngine = false
                    makeAnOpponentMove()
                } else {
                    moveEngine = true
                }
            }
        }
    }

    private fun makeAnOpponentMove() {
        if (!gameVsEngine) return
        lifecycleScope.launch {
            try {
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

    private fun makeAnOpponentMove(fen: String) {
        if (!gameVsEngine) return
        lifecycleScope.launch {
            try {
                val (bestMove, eval) = StockfishEngine.getBestMoveAndEval(fen)
                if (bestMove.length < 4) return@launch
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

    private fun handleMove(move: Move) {
        gameState.makeMove(move)
        if (move.type == MoveType.EnPassant || move.type == MoveType.CastleKS || move.type == MoveType.CastleQS) {
            drawBoard(gameState.board)
        } else {
            val oldPos = UIboard[move.fromPos.row][move.fromPos.column]
            val newPos = UIboard[move.toPos.row][move.toPos.column]
            newPos?.setImageDrawable(oldPos?.drawable)
            oldPos?.setImageDrawable(loadSourceDrawable(R.drawable.ic_blank))
        }
        arrowOverlay.clearHintArrow()
        if (gameState.isGameOver()) {
            showGameOver()
        }
        lifecycleScope.launch {
            DataStoreManager.saveGameState(this@MainActivity, gameState)
        }
    }

    private fun updateLastMoveArrow() {
        arrowOverlay.clearMoveArrow()
        if (currentIndex > 0 && currentIndex < gameState.gameHistory.size) {
            val moveStr = gameState.gameHistory[currentIndex]
            val parts = moveStr.split("-")
            if (parts.size == 2) {
                val start = parts[0]
                val end = parts[1]
                val (startFile, startRank) = convertChessCoordToIndex(start)
                val (endFile, endRank) = convertChessCoordToIndex(end)
                val cellSize = binding!!.chessboardContainer.width / 8f
                val startX = (startFile - 1) * cellSize + cellSize / 2
                val startY = (8 - startRank) * cellSize + cellSize / 2
                val endX = (endFile - 1) * cellSize + cellSize / 2
                val endY = (8 - endRank) * cellSize + cellSize / 2
                val category = moveAnalyses.getOrNull(currentIndex)?.category
                arrowOverlay.addMoveArrow(startX, startY, endX, endY, category)
            }
        }
    }

    fun updateMoveHistory() {
        currentIndex = gameState.gameHistory.lastIndex
        if (currentIndex != currentIndex + cutIndex) {
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
        val paddedAnalyses = mutableListOf<MoveAnalysis?>()
        paddedAnalyses.add(null) // For initial position
        paddedAnalyses.addAll(moveAnalyses)
        while (paddedAnalyses.size < moves.size) {
            paddedAnalyses.add(null)
        }
        if (paddedAnalyses.size > moves.size) {
            paddedAnalyses.subList(moves.size, paddedAnalyses.size).clear()
        }
        adapter.updateMoves(moves, paddedAnalyses, currentIndex)
        binding!!.moveHistoryList.scrollToPosition(currentIndex)
        updateLastMoveArrow()
    }

    fun updateEvaluation(eval: Any?) {
        runOnUiThread {
            if (eval is String && eval.startsWith("mate")) {
                val mateValue = eval.split(" ").last().toInt()
                binding!!.evalText.text = "Mate in ${if (mateValue > 0) mateValue else -mateValue}"
                binding!!.evalBar.progress = if (mateValue > 0) 100 else 0
            } else if (eval is Float) {
                val clampedEval = eval.coerceIn(-10f, 10f)
                val progress = (((clampedEval + 10f) / 20f) * 100f).toInt()
                binding!!.evalText.text = String.format("%.2f", clampedEval)
                binding!!.evalBar.progress = progress
            }
        }
    }

    private fun handlePromotion(from: Position, to: Position) {
        val oldPos = UIboard[to.row][to.column]
        val newPos = UIboard[from.row][from.column]
        val pawnPromotionsMenu = PawnPromotionsMenu(this)
        pawnPromotionsMenu.setOnPromotionSelected { selectedPiece ->
            val promPieceResId: Int? = Images.getImage(gameState.currentPlayer, selectedPiece)
            val promPieceDrawable = promPieceResId?.let { ContextCompat.getDrawable(this, it) }
            newPos?.setImageDrawable(promPieceDrawable)
            oldPos?.setImageDrawable(loadSourceDrawable(R.drawable.ic_blank))
            val promMove = PawnPromotion(from, to, selectedPiece)
            handleMove(promMove)
            updateLastMoveArrow()
        }
        pawnPromotionsMenu.show()
    }

    private fun showHighlights() {
        for (to in moveCache.keys) {
            val piece: Piece? = gameState.board[to.row, to.column]
            val img: ImageView? = UIboard[to.row][to.column]
            img?.setBackgroundResource(
                if (piece != null) R.drawable.ic_highlight_red else R.drawable.ic_highlight_green
            )
        }
    }

    private fun hideHighlights() {
        for (r in 0..7) {
            for (c in 0..7) {
                UIboard[r][c]?.setBackgroundResource(0)
            }
        }
    }

    fun getWinnerText(winner: Player): String {
        return when (winner) {
            Player.White -> "WHITE WINS!"
            Player.Black -> "BLACK WINS!"
            else -> "IT'S A DRAW"
        }
    }

    fun PlayerString(player: Player): String {
        return when (player) {
            Player.White -> "WHITE"
            Player.Black -> "BLACK"
            else -> ""
        }
    }

    fun getReasonText(reason: EndReason, currentPlayer: Player): String {
        return when (reason) {
            EndReason.Stalemate -> "STALEMATE - ${PlayerString(currentPlayer)} CAN'T MOVE"
            EndReason.Checkmate -> "CHECKMATE - ${PlayerString(currentPlayer)} CAN'T MOVE"
            EndReason.FiftyMoveRule -> "FIFTY-MOVE RULE"
            EndReason.InsufficientMaterial -> "INSUFFICIENT MATERIAL"
            EndReason.ThreefoldRepetition -> "THREEFOLD REPETITION"
        }
    }

    private fun showGameOver() {
        val gameOverMenu = GameOverMenu(this)
        val result = gameState.result!!
        gameOverMenu.setWinnerText(getWinnerText(result.winner))
        gameOverMenu.setReasonText(getReasonText(result.reason, gameState.currentPlayer))
        gameOverMenu.show()
        gameOverMenu.setRestart(object : GameOverMenu.RestartClick {
            override fun onRestartClick() {
                gameOverMenu.cancel()
                restartGame()
            }
        })
        gameOverMenu.setClose(object : GameOverMenu.CloseClick {
            override fun onCloseClick() {
                gameOverMenu.cancel()
            }
        })
        gameOverMenu.setAnalyze(object : GameOverMenu.AnalyzeClick {
            override fun onAnalyzeClick() {
                val analysePosition = gameState.gameHistoryFENs
                lifecycleScope.launch {
                    val analysis = GameAnalysis(analysePosition, this@MainActivity)
                    analysis.analyzeGame()
                    moveAnalyses.clear()
                    moveAnalyses.add(null)
                    moveAnalyses.addAll(analysis.getMoveAnalyses())
                    updateMoveHistory()
                    updateLastMoveArrow()
                }
            }
        })
    }

    private fun restartGame() {
        hideHighlights()
        moveCache.clear()
        moveAnalyses.clear()
        val dialog = PlayChessMenu()
        dialog.listener = object : PlayChessMenu.OnSettingsConfirmedListener {
            override fun onSettingsConfirmed(
                playAsWhite: Boolean,
                isLevel: Boolean,
                eloOrLevelLimitValue: Int,
                isTime: Boolean,
                depthOrTimeLimitValue: Int,
                startPos: String
            ) {
                startGameWithSettings(
                    playAsWhite,
                    isLevel,
                    eloOrLevelLimitValue,
                    isTime,
                    depthOrTimeLimitValue,
                    startPos
                )
            }
        }
        dialog.show(supportFragmentManager, "PlayChessMenu")
    }

    private fun startGameWithSettings(
        playAsWhite: Boolean,
        isLevel: Boolean,
        eloOrLevelLimitValue: Int,
        isTime: Boolean,
        depthOrTimeLimitValue: Int,
        startPos: String
    ) {
        val player: Player = if (playAsWhite) Player.White else Player.Black
        if (isLevel) {
            StockfishEngine.skillLevel = eloOrLevelLimitValue
            StockfishEngine.skillElo = null
        } else {
            StockfishEngine.skillElo = eloOrLevelLimitValue
            StockfishEngine.skillLevel = null
        }
        if (isTime) {
            StockfishEngine.searchTime = depthOrTimeLimitValue
            StockfishEngine.depth = null
        } else {
            StockfishEngine.depth = depthOrTimeLimitValue
            StockfishEngine.searchTime = null
        }
        StockfishEngine.applyEngineSettings()
        var startPosition = startPos
        if (!FenUtils.isValidFEN(startPos) || startPosition == "") {
            startPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        }
        val board: Board = Board.initial(startPosition)
        gameState = GameState(
            if (FenUtils.isValidFEN(startPosition) && FenUtils.currentPlayer(startPosition) != player)
                Player.opponent(player) else player,
            board
        )
        drawBoard(gameState.board)
        gameVsEngine = true
        moveEngine = true
        updateButtonVisibility()
        if (FenUtils.isValidFEN(startPosition) && FenUtils.currentPlayer(startPosition) != player) {
            lifecycleScope.launch {
                makeAnOpponentMove(startPosition)
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }
}