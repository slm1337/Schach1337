package com.example.schach1337

import android.annotation.SuppressLint
import android.content.Context
import com.example.schach1337.logic.FenUtils
import com.example.schach1337.logic.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

object StockfishEngine {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    var isInitialized = false
        private set

    var depth: Int? = 20
    var searchTime: Int? = null
    var skillElo: Int? = null
    var skillLevel: Int? = 20
    var numThreads: Int = 1
    var multiPv : Int = 2

    var FEN : String = ""
    private var currentJob: Job? = null

    private val _infoFlow = MutableStateFlow<List<MultipvInfo>>(emptyList())
    val infoFlow: StateFlow<List<MultipvInfo>> get() = _infoFlow

    private val _evalFlow = MutableStateFlow<Float?>(null)
    val evalFlow: StateFlow<Float?> get() = _evalFlow

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
        setUciOption("MultiPV", "2")
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

    @SuppressLint("SuspiciousIndentation")
    fun analyzePosition(fen: String, depthLimit: Int? = null, timeLimit: Int? = null) {
        currentJob?.cancel()
        sendCommand("stop")

        CoroutineScope(Dispatchers.IO).launch {
            _infoFlow.emit(emptyList())
        }

        currentJob = CoroutineScope(Dispatchers.IO).launch {
            _infoFlow.emit(emptyList())
            sendCommand("position fen $fen")
            FEN = fen;

            sendCommand(if (depthLimit != null) "go depth $depthLimit" else "go movetime ${timeLimit ?: 1000}")

            val results = mutableMapOf<Int, MultipvInfo>()
            val currentPlayer = FenUtils.currentPlayer(fen)

            try {
                var line: String? = ""
                while (isActive && reader?.readLine().also { line = it } != null) {
                    val l = line ?: continue
                        if (l.startsWith("info")) {
                            _infoFlow.emit(emptyList())
                            val parts = l.split(" ")
                            var multipv = 1
                            var local_depth = -1
                            var score: Any? = null
                            val pv = mutableListOf<String>()

                            for (i in parts.indices) {
                                when (parts[i]) {
                                    "depth" -> local_depth = parts.getOrNull(i + 1)?.toIntOrNull() ?: -1
                                    "multipv" -> multipv = parts.getOrNull(i + 1)?.toIntOrNull() ?: 1
                                    "score" -> {
                                        when (parts.getOrNull(i + 1)) {
                                            "cp" -> {
                                                val cp = parts.getOrNull(i + 2)?.toFloatOrNull()
                                                if (cp != null) score = cp / 100f
                                            }
                                            "mate" -> {
                                                val mate = parts.getOrNull(i + 2)?.toIntOrNull()
                                                if (mate != null) score = "mate $mate"
                                            }
                                        }
                                    }
                                    "pv" -> {
                                        pv.addAll(parts.drop(i + 1))
                                        break
                                    }
                                }
                            }

                            score = when {
                                score is Float && currentPlayer == Player.Black -> -(score as Float)
                                score is String && (score as String).startsWith("mate") -> {
                                    val mateVal = score.split(" ")[1].toInt()
                                    if (currentPlayer == Player.Black) "mate -$mateVal" else score
                                }
                                else -> score
                            }

                            if (local_depth >= 0 && pv.isNotEmpty()) {
                                    results[multipv] = MultipvInfo(FEN, local_depth, score, pv, pv.first())
                                    println(results.values.toList())
                                    _infoFlow.emit(results.values.toList())
                                    if (multipv == 1 && score is Float) {
                                        _evalFlow.emit(score)
                                    }
                            }
                        }
                    delay(50)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
    }

    fun close() {
        sendCommand("quit")
        process?.destroy()
        isInitialized = false
    }
}