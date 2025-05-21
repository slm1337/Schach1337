package com.example.schach1337

import android.content.Intent
import android.content.Context
import com.example.schach1337.logic.FenUtils
import com.example.schach1337.logic.Player
import kotlin.math.exp

class GameAnalysis(
    private val fenHistory: MutableList<String>,
    private val context: Context
) {
    private val moveAnalyses = mutableListOf<MoveAnalysis>()

    private val inaccuracyThreshold = 0.5f
    private val mistakeThreshold = 1.0f
    private val blunderThreshold = 2.0f

    suspend fun analyzeGame() {
        moveAnalyses.clear()

        for (moveIndex in 1 until fenHistory.size) {
            val fenBefore = fenHistory[moveIndex - 1]
            val fenAfter = fenHistory[moveIndex]

            val (bestMove, evalBefore) = StockfishEngine.getBestMoveAndEval(fenBefore)
            val (_, evalAfter) = StockfishEngine.getBestMoveAndEval(fenAfter)

            val move = FenUtils.getMoveFromFENs(fenBefore, fenAfter)!!

            val category = classifyMove(evalBefore, evalAfter)

            val moveNumber = (moveIndex + 1) / 2

            val analysis = MoveAnalysis(
                moveNumber = moveNumber,
                move = move,
                fenBefore = fenBefore,
                fenAfter = fenAfter,
                evalBefore = evalBefore,
                evalAfter = evalAfter,
                bestMove = bestMove,
                category = category
            )
            moveAnalyses.add(analysis)
        }

        displayResults()
    }

    fun getMoveAnalyses(): List<MoveAnalysis> = moveAnalyses.toList()

    private fun classifyMove(
        evalBefore: Any?,
        evalAfter: Any?
    ): MoveCategory {
        if (evalBefore is Float && evalAfter is Float) {
            val evalDiff = kotlin.math.abs(evalBefore - evalAfter)

            return when {
                evalDiff < 0.1f -> MoveCategory.BEST
                evalDiff < inaccuracyThreshold -> MoveCategory.GOOD
                evalDiff < mistakeThreshold -> MoveCategory.INACCURACY
                evalDiff < blunderThreshold -> MoveCategory.MISTAKE
                else -> MoveCategory.BLUNDER
            }
        }

        if (evalBefore is String && evalBefore.startsWith("mate") ||
            evalAfter is String && evalAfter.startsWith("mate")
        ) {
            if (evalAfter is String && evalAfter.startsWith("mate -")) {
                return MoveCategory.BLUNDER
            }
            if (evalBefore is String && evalBefore.startsWith("mate ") && evalAfter !is String) {
                return MoveCategory.MISTAKE
            }
        }

        return MoveCategory.GOOD
    }

    private fun calculateWinPercent(eval: Float, isWhite: Boolean): Float {
        val adjustedEval = if (isWhite) eval else -eval
        return (50 + 50 * (2 / (1 + exp(-0.368208f * adjustedEval)) - 1)).toFloat()
    }

    private fun calculateAccuracy(winPercentBefore: Float, winPercentAfter: Float): Float {
        val accuracy = 103.1668f * exp(-0.04354f * (winPercentBefore - winPercentAfter)) - 3.1669f
        return accuracy.coerceIn(0f, 100f)
    }

    private fun getAnalysisData(): AnalysisData {
        val evaluations = mutableListOf<Float>()
        val whiteCounts = MoveCounts(0, 0, 0, 0, 0)
        val blackCounts = MoveCounts(0, 0, 0, 0, 0)
        var whiteAccuracy = 0f
        var blackAccuracy = 0f

        val whiteMoves = moveAnalyses.filter { FenUtils.currentPlayer(it.fenBefore) == Player.White }
        val blackMoves = moveAnalyses.filter { FenUtils.currentPlayer(it.fenBefore) == Player.Black }

        moveAnalyses.forEach { analysis ->
            if (analysis.evalAfter is Float) {
                evaluations.add(analysis.evalAfter)
            }
        }

        whiteMoves.forEach { analysis ->
            when (analysis.category) {
                MoveCategory.BEST -> whiteCounts.best++
                MoveCategory.GOOD -> whiteCounts.good++
                MoveCategory.INACCURACY -> whiteCounts.inaccuracy++
                MoveCategory.MISTAKE -> whiteCounts.mistake++
                MoveCategory.BLUNDER -> whiteCounts.blunder++
            }
        }

        blackMoves.forEach { analysis ->
            when (analysis.category) {
                MoveCategory.BEST -> blackCounts.best++
                MoveCategory.GOOD -> blackCounts.good++
                MoveCategory.INACCURACY -> blackCounts.inaccuracy++
                MoveCategory.MISTAKE -> blackCounts.mistake++
                MoveCategory.BLUNDER -> blackCounts.blunder++
            }
        }

        val whiteAccuracies = mutableListOf<Float>()
        val blackAccuracies = mutableListOf<Float>()

        for (move in whiteMoves) {
            when {
                move.evalBefore is Float && move.evalAfter is Float -> {
                    val winPercentBefore = calculateWinPercent(move.evalBefore, isWhite = true)
                    val winPercentAfter = calculateWinPercent(move.evalAfter, isWhite = true)
                    val accuracy = calculateAccuracy(winPercentBefore, winPercentAfter)
                    whiteAccuracies.add(accuracy)
                }
                move.evalAfter is String && move.evalAfter.startsWith("mate -") -> {
                    whiteAccuracies.add(0f)
                }
                move.evalAfter is String && move.evalAfter.startsWith("mate ") -> {
                    whiteAccuracies.add(100f)
                }
            }
        }

        for (move in blackMoves) {
            when {
                move.evalBefore is Float && move.evalAfter is Float -> {
                    val winPercentBefore = calculateWinPercent(move.evalBefore, isWhite = false)
                    val winPercentAfter = calculateWinPercent(move.evalAfter, isWhite = false)
                    val accuracy = calculateAccuracy(winPercentBefore, winPercentAfter)
                    blackAccuracies.add(accuracy)
                }
                move.evalAfter is String && move.evalAfter.startsWith("mate -") -> {
                    blackAccuracies.add(0f)
                }
                move.evalAfter is String && move.evalAfter.startsWith("mate ") -> {
                    blackAccuracies.add(100f)
                }
            }
        }

        whiteAccuracy = if (whiteAccuracies.isNotEmpty()) {
            whiteAccuracies.average().toFloat().coerceIn(0f, 100f)
        } else 0f
        blackAccuracy = if (blackAccuracies.isNotEmpty()) {
            blackAccuracies.average().toFloat().coerceIn(0f, 100f)
        } else 0f

        return AnalysisData(
            evaluations = evaluations,
            whiteCounts = whiteCounts,
            blackCounts = blackCounts,
            whiteAccuracy = whiteAccuracy,
            blackAccuracy = blackAccuracy
        )
    }

    private fun displayResults() {
        val analysisData = getAnalysisData()

        val intent = Intent(context, ActivityAnalysisResults::class.java).apply {
            putExtra("evaluations", ArrayList(analysisData.evaluations))
            putExtra("whiteBestCount", analysisData.whiteCounts.best)
            putExtra("whiteGoodCount", analysisData.whiteCounts.good)
            putExtra("whiteInaccuracyCount", analysisData.whiteCounts.inaccuracy)
            putExtra("whiteMistakeCount", analysisData.whiteCounts.mistake)
            putExtra("whiteBlunderCount", analysisData.whiteCounts.blunder)
            putExtra("blackBestCount", analysisData.blackCounts.best)
            putExtra("blackGoodCount", analysisData.blackCounts.good)
            putExtra("blackInaccuracyCount", analysisData.blackCounts.inaccuracy)
            putExtra("blackMistakeCount", analysisData.blackCounts.mistake)
            putExtra("blackBlunderCount", analysisData.blackCounts.blunder)
            putExtra("whiteAccuracy", analysisData.whiteAccuracy)
            putExtra("blackAccuracy", analysisData.blackAccuracy)
        }

        context.startActivity(intent)
    }

    private fun printReport() {
        println("=== Game Analysis Report ===")

        val bestMoves = moveAnalyses.filter { it.category == MoveCategory.BEST }
        val goodMoves = moveAnalyses.filter { it.category == MoveCategory.GOOD }
        val inaccuracies = moveAnalyses.filter { it.category == MoveCategory.INACCURACY }
        val mistakes = moveAnalyses.filter { it.category == MoveCategory.MISTAKE }
        val blunders = moveAnalyses.filter { it.category == MoveCategory.BLUNDER }

        println("Total moves: ${moveAnalyses.size}")
        println("Best moves: ${bestMoves.size}")
        println("Good moves: ${goodMoves.size}")
        println("Inaccuracies: ${inaccuracies.size}")
        println("Mistakes: ${mistakes.size}")
        println("Blunders: ${blunders.size}")
        println()

        if (bestMoves.isNotEmpty()) {
            println("Best Moves:")
            bestMoves.forEach { printMove(it) }
        }
        if (goodMoves.isNotEmpty()) {
            println("Good Moves:")
            goodMoves.forEach { printMove(it) }
        }
        if (inaccuracies.isNotEmpty()) {
            println("Inaccuracies:")
            inaccuracies.forEach { printMove(it) }
        }
        if (mistakes.isNotEmpty()) {
            println("Mistakes:")
            mistakes.forEach { printMove(it) }
        }
        if (blunders.isNotEmpty()) {
            println("Blunders:")
            blunders.forEach { printMove(it) }
        }
    }

    private fun printMove(analysis: MoveAnalysis) {
        val evalBeforeStr = when (analysis.evalBefore) {
            is Float -> String.format("%.2f", analysis.evalBefore)
            else -> analysis.evalBefore.toString()
        }
        val evalAfterStr = when (analysis.evalAfter) {
            is Float -> String.format("%.2f", analysis.evalAfter)
            else -> analysis.evalAfter.toString()
        }

        println("Move ${analysis.moveNumber}: ${analysis.move} (${analysis.category})")
        println("  Eval before: $evalBeforeStr")
        println("  Eval after: $evalAfterStr")
        println("  Best move: ${analysis.bestMove}")
        println()
    }
}