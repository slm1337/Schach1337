package com.example.schach1337

import android.content.Context
import com.example.schach1337.logic.FenUtils
import com.example.schach1337.logic.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

object StockfishEngine {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    var isInitialized = false
        private set

    var depth: Int? = 12
    var searchTime: Int? = null
    var skillElo: Int? = null
    var skillLevel: Int? = 20
    var numThreads: Int = 1

    fun initialize(context: Context) {
        if (isInitialized) return

        val path = context.applicationInfo.nativeLibraryDir + "/stockfish.so"
        val file = File(path)

        try {
            process = Runtime.getRuntime().exec(file.path)
            writer = BufferedWriter(OutputStreamWriter(process?.outputStream))
            reader = BufferedReader(InputStreamReader(process?.inputStream))

            sendCommand("uci")

            applyEngineSettings()

            isInitialized = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun applyEngineSettings() {
        if (skillLevel != null) {
            setUciOption("UCI_LimitStrength", "false")
            setUciOption("Skill Level", skillLevel.toString())
        } else {
            setUciOption("Skill Level", "20")
            setUciOption("UCI_LimitStrength", "true")
            setUciOption("UCI_Elo", skillElo.toString())
        }

        setUciOption("Threads", numThreads.toString())
    }

    private fun setUciOption(name: String, value: String) {
        sendCommand("setoption name $name value $value")
    }

    fun sendCommand(command: String) {
        try {
            writer?.apply {
                write("$command\n")
                flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun getBestMoveAndEval(fen: String): Pair<String, Any?> = withContext(Dispatchers.IO) {
        sendCommand("position fen $fen")

        when {
            depth != null -> sendCommand("go depth $depth")
            searchTime != null -> sendCommand("go movetime $searchTime")
            else -> sendCommand("go depth 10")
        }

        var bestMove = "unknown"
        var eval: Any? = null

        try {
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                if (line!!.startsWith("info")) {
                    val parts = line!!.split(" ")
                    for (i in parts.indices) {
                        if (parts[i] == "score") {
                            when (parts[i + 1]) {
                                "cp" -> {
                                    eval = parts[i + 2].toFloat() / 100f
                                }
                                "mate" -> {
                                    eval = "mate ${parts[i + 2]}"
                                }
                            }
                        }
                    }
                }

                if (line!!.startsWith("bestmove")) {
                    bestMove = line!!.split(" ")[1]
                    break
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val currentPlayer = FenUtils.currentPlayer(fen)

        eval = when {
            eval is Float && currentPlayer == Player.Black -> -eval
            eval is String && eval.startsWith("mate") -> {
                val mateValue = eval.split(" ")[1].toInt()
                if (currentPlayer == Player.Black) "mate -$mateValue" else "mate $mateValue"
            }
            else -> eval
        }

        Pair(bestMove, eval)
    }

    fun close() {
        sendCommand("quit")
        process?.destroy()
        isInitialized = false
    }
}