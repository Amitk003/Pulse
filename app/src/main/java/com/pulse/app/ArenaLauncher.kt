package com.pulse.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.pulse.bridge.BridgeJson
import com.pulse.bridge.PlayerSnapshot

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
        val launchIntent = separateIntent(context, playerSnapshot)
            ?: return Result.NOT_INSTALLED

        // Route through MainActivity so the return can be read back when the
        // arena reports one. A plain Unity APK won't set a result -> null.
        val activity = context as? MainActivity
        if (activity != null) {
            activity.launchArena(launchIntent) { json ->
                activity.arenaResultHandler = null
                pendingResultCallback?.invoke(json)
                pendingResultCallback = null
            }
        } else {
            if (context !is Activity) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launchIntent)
        }
        return Result.LAUNCHED
    }

    /** Callback waiting for the arena to return (separate or embedded). */
    var pendingResultCallback: ((String?) -> Unit)? = null

    /** Intent for the separate com.pulse.arena APK, or null if not installed. */
    fun separateIntent(context: Context, playerSnapshot: String? = null): Intent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(arenaPackage)
            ?: return null
        if (playerSnapshot != null) {
            launchIntent.putExtra(
                UnityPlayerActivity.EXTRA_PLAYER_SNAPSHOT, playerSnapshot
            )
        }
        return launchIntent
    }

    /** Intent for the embedded UnityPlayerActivity. */
    fun embeddedIntent(context: Context, playerSnapshot: String? = null): Intent {
        val intent = Intent(context, UnityPlayerActivity::class.java)
        if (playerSnapshot != null) {
            intent.putExtra(UnityPlayerActivity.EXTRA_PLAYER_SNAPSHOT, playerSnapshot)
        }
        return intent
    }

    fun isEmbedded(context: Context): Boolean = isEmbeddedUnityAvailable(context)
    
    private fun isEmbeddedUnityAvailable(context: Context): Boolean {
        // UnityPlayerActivity is always declared in the manifest, so resolving
        // it proves nothing. Only return true once the Unity runtime is present
        // (exported as a library). Until then use the separate arena APK.
        return try {
            Class.forName("com.unity3d.player.UnityPlayer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
    
    private fun launchEmbedded(context: Context, playerSnapshot: String?): Result {
        val intent = embeddedIntent(context, playerSnapshot)

        val activity = context as? MainActivity
        if (activity != null) {
            activity.launchArena(intent) { json ->
                activity.arenaResultHandler = null
                pendingResultCallback?.invoke(json)
                pendingResultCallback = null
            }
            return Result.EMBEDDED_LAUNCHED
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
        return BridgeJson.playerSnapshotToJson(snapshot)
    }
}
