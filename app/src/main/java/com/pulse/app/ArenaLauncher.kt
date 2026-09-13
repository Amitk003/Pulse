package com.pulse.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.pulse.bridge.PlayerSnapshot
import org.json.JSONArray
import org.json.JSONObject

/**
 * Launches the Unity arena.
 * First tries to launch embedded Unity activity.
 * Falls back to separate APK if not available.
 */
internal object ArenaLauncher {
    
    const val arenaPackage = "com.pulse.arena"
    
    enum class Result {
        LAUNCHED,
        NOT_INSTALLED,
        EMBEDDED_LAUNCHED
    }
    
    /**
     * Launch the arena with player snapshot data.
     */
    fun launch(context: Context, playerSnapshot: String? = null): Result {
        // Try embedded Unity first
        if (isEmbeddedUnityAvailable(context)) {
            return launchEmbedded(context, playerSnapshot)
        }
        
        // Fall back to separate APK
        val launchIntent = context.packageManager.getLaunchIntentForPackage(arenaPackage)
            ?: return Result.NOT_INSTALLED
        
        if (playerSnapshot != null) {
            launchIntent.putExtra(UnityPlayerActivity.EXTRA_PLAYER_SNAPSHOT, playerSnapshot)
        }
        
        if (context !is Activity) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)
        return Result.LAUNCHED
    }
    
    private fun isEmbeddedUnityAvailable(context: Context): Boolean {
        // Defer to the Android manifest registration of UnityPlayerActivity.
        // If Unity is not exported as a library yet, this should return false
        // and the app should fall back to the separate arena APK.
        return try {
            val intent = Intent(context, UnityPlayerActivity::class.java)
            context.packageManager.resolveActivity(intent, 0) != null
        } catch (_: Exception) {
            false
        }
    }
    
    private fun launchEmbedded(context: Context, playerSnapshot: String?): Result {
        val intent = Intent(context, UnityPlayerActivity::class.java)
        
        if (playerSnapshot != null) {
            intent.putExtra(UnityPlayerActivity.EXTRA_PLAYER_SNAPSHOT, playerSnapshot)
        }
        
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return Result.EMBEDDED_LAUNCHED
    }
    
    /**
     * Build a player snapshot from Pulse clone progress.
     *
     * This uses the shared bridge contract so Pulse and Unity stay on
     * the same schema. If Unity needs extra arena stats later, extend
     * the contract in bridge/ instead of duplicating JSON here.
     */
    fun createPlayerSnapshot(
        playerId: String,
        cloneLevel: Int,
        strength: Float,
        formMastery: Float,
        consistency: Float,
        recovery: Float,
        gameXp: Int
    ): String {
        val snapshot = PlayerSnapshot(
            playerId = playerId,
            cloneLevel = cloneLevel,
            strength = strength,
            formMastery = formMastery,
            consistency = consistency,
            recovery = recovery,
            gameXp = gameXp,
            unlockedAbilities = emptyList(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        return snapshot.toJson()
    }

    private fun PlayerSnapshot.toJson(): String {
        return JSONObject().apply {
            put("schemaVersion", schemaVersion)
            put("playerId", playerId)
            put("cloneLevel", cloneLevel)
            put("strength", strength)
            put("formMastery", formMastery)
            put("consistency", consistency)
            put("recovery", recovery)
            put("gameXp", gameXp)
            put("unlockedAbilities", JSONArray().apply {
                unlockedAbilities.forEach { put(it) }
            })
            put("updatedAtEpochMs", updatedAtEpochMs)
        }.toString()
    }
}
