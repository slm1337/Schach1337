package com.example.schach1337

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.schach1337.logic.Board
import com.example.schach1337.logic.GameState
import com.example.schach1337.logic.Player
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chess_settings")

object DataStoreManager {
    private val KEY_SKILL_LEVEL = intPreferencesKey("skill_level")
    private val KEY_SKILL_ELO = intPreferencesKey("skill_elo")
    private val KEY_DEPTH = intPreferencesKey("depth")
    private val KEY_SEARCH_TIME = intPreferencesKey("search_time")
    private val KEY_NUM_THREADS = intPreferencesKey("num_threads")
    private val KEY_HASH = intPreferencesKey("hash")
    private val KEY_NUMA_POLICY = stringPreferencesKey("numa_policy")
    private val KEY_MOVE_OVERHEAD = intPreferencesKey("move_overhead")
    private val KEY_NODES_TIME = intPreferencesKey("nodes_time")
    private val KEY_SYZYGY_50_MOVE_RULE = booleanPreferencesKey("syzygy_50_move_rule")
    private val KEY_SYZYGY_PROBE_DEPTH = intPreferencesKey("syzygy_probe_depth")
    private val KEY_SYZYGY_PROBE_LIMIT = intPreferencesKey("syzygy_probe_limit")
    private val KEY_GAME_HISTORY_FENS = stringPreferencesKey("game_history_fens")
    private val KEY_CURRENT_PLAYER = stringPreferencesKey("current_player")

    suspend fun saveEngineSettings(context: Context) {
        context.dataStore.edit { preferences ->
            StockfishEngine.skillLevel?.let { preferences[KEY_SKILL_LEVEL] = it }
            StockfishEngine.skillElo?.let { preferences[KEY_SKILL_ELO] = it }
            StockfishEngine.depth?.let { preferences[KEY_DEPTH] = it }
            StockfishEngine.searchTime?.let { preferences[KEY_SEARCH_TIME] = it }
            preferences[KEY_NUM_THREADS] = StockfishEngine.numThreads
            preferences[KEY_HASH] = StockfishEngine.hash
            preferences[KEY_NUMA_POLICY] = StockfishEngine.numaPolicy
            preferences[KEY_MOVE_OVERHEAD] = StockfishEngine.moveOverhead
            preferences[KEY_NODES_TIME] = StockfishEngine.nodesTime
            preferences[KEY_SYZYGY_50_MOVE_RULE] = StockfishEngine.syzygy50MoveRule
            preferences[KEY_SYZYGY_PROBE_DEPTH] = StockfishEngine.syzygyProbeDepth
            preferences[KEY_SYZYGY_PROBE_LIMIT] = StockfishEngine.syzygyProbeLimit
        }
    }

    suspend fun loadEngineSettings(context: Context) {
        context.dataStore.data.first().let { preferences ->
            StockfishEngine.skillLevel = preferences[KEY_SKILL_LEVEL]
            StockfishEngine.skillElo = preferences[KEY_SKILL_ELO]
            StockfishEngine.depth = preferences[KEY_DEPTH]
            StockfishEngine.searchTime = preferences[KEY_SEARCH_TIME]
            StockfishEngine.numThreads = preferences[KEY_NUM_THREADS] ?: 1
            StockfishEngine.hash = preferences[KEY_HASH] ?: 16
            StockfishEngine.numaPolicy = preferences[KEY_NUMA_POLICY] ?: "auto"
            StockfishEngine.moveOverhead = preferences[KEY_MOVE_OVERHEAD] ?: 10
            StockfishEngine.nodesTime = preferences[KEY_NODES_TIME] ?: 0
            StockfishEngine.syzygy50MoveRule = preferences[KEY_SYZYGY_50_MOVE_RULE] ?: true
            StockfishEngine.syzygyProbeDepth = preferences[KEY_SYZYGY_PROBE_DEPTH] ?: 1
            StockfishEngine.syzygyProbeLimit = preferences[KEY_SYZYGY_PROBE_LIMIT] ?: 0
            StockfishEngine.applyEngineSettings()
        }
    }

    suspend fun saveGameState(context: Context, gameState: GameState) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GAME_HISTORY_FENS] = gameState.gameHistoryFENs.joinToString(";")
            preferences[KEY_CURRENT_PLAYER] = gameState.currentPlayer.name
        }
    }

    suspend fun loadGameState(context: Context): GameState? {
        return context.dataStore.data.map { preferences ->
            val fenString = preferences[KEY_GAME_HISTORY_FENS]
            val playerName = preferences[KEY_CURRENT_PLAYER]
            if (fenString != null && playerName != null && fenString.isNotEmpty()) {
                val fenList = fenString.split(";").toMutableList()
                val player = Player.valueOf(playerName)
                val board = Board.initial(fenList.last())
                GameState(player, board).apply {
                    gameHistoryFENs = fenList
                    updateMoveHistoryFromFENs()
                }
            } else {
                null
            }
        }.first()
    }
}