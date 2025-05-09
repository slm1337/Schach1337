package com.example.schach1337

import android.content.Context
import java.io.*

object StockfishEngine {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    var isInitialized = false
        private set

    var depth: Int? = 12
    var searchTime: Int? = null
    var skillLevel: Int? = 1
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
            //skillLevel?.let { setUciOption("Skill Level", it.toString()) } ?: setUciOption("Skill Level", "20")
            setUciOption("Threads", numThreads.toString())
            isInitialized = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
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

    fun getBestMove(fen: String): String {
        sendCommand("position fen $fen")

        when {
            depth != null -> sendCommand("go depth $depth")
            searchTime != null -> sendCommand("go movetime $searchTime")
            else -> sendCommand("go depth 10")
        }

        var bestMove = "unknown"

        try {
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                if (line!!.startsWith("bestmove")) {
                    bestMove = line!!.split(" ")[1]
                    break
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bestMove
    }

    fun getPositionEval(fen: String): Float? {
        sendCommand("position fen $fen")

        when {
            depth != null -> sendCommand("go depth $depth")
            searchTime != null -> sendCommand("go movetime $searchTime")
            else -> sendCommand("go depth 10") // Значение по умолчанию
        }

        var eval: Float? = null

        try {
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                if (line!!.startsWith("info")) {
                    val parts = line!!.split(" ")
                    for (i in parts.indices) {
                        if (parts[i] == "score") {
                            if (parts[i + 1] == "cp") {
                                eval = parts[i + 2].toFloat() / 100f
                            }
                        }
                    }
                    if (eval != null) break
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return eval
    }

    fun close() {
        sendCommand("quit")
        process?.destroy()
        isInitialized = false
    }
}