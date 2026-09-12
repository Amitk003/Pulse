package com.pulse.bridge

const val CURRENT_BRIDGE_SCHEMA = 1

private val STABLE_ID = Regex("^[a-z0-9_]+$")

/**
 * Progress values sent from Pulse to the Unity arena.
 *
 * This is a platform-free contract. Android storage, JSON code, and Unity
 * message code must map to this model instead of sharing database classes.
 */
data class PlayerSnapshot(
    val schemaVersion: Int = CURRENT_BRIDGE_SCHEMA,
    val playerId: String,
    val cloneLevel: Int,
    val strength: Float,
    val formMastery: Float,
    val consistency: Float,
    val recovery: Float,
    val gameXp: Int,
    val unlockedAbilities: List<String>,
    val updatedAtEpochMs: Long
) {
    init {
        require(schemaVersion == CURRENT_BRIDGE_SCHEMA) {
            "Unsupported bridge schema: $schemaVersion"
        }
        require(playerId.isNotBlank()) { "playerId cannot be blank" }
        require(cloneLevel >= 1) { "cloneLevel must be at least 1" }
        require(strength.isFinite() && strength >= 0f) { "strength must be finite and non-negative" }
        require(formMastery in 0f..100f) { "formMastery must be between 0 and 100" }
        require(consistency in 0f..1f) { "consistency must be between 0 and 1" }
        require(recovery in 0f..1f) { "recovery must be between 0 and 1" }
        require(gameXp >= 0) { "gameXp must be non-negative" }
        require(unlockedAbilities.distinct().size == unlockedAbilities.size) {
            "unlockedAbilities must not contain duplicates"
        }
        require(unlockedAbilities.all { STABLE_ID.matches(it) }) {
            "unlockedAbilities must use stable lower-case ids"
        }
        require(updatedAtEpochMs > 0L) { "updatedAtEpochMs must be positive" }
    }
}

/**
 * Result sent from the Unity arena back to Pulse.
 */
data class FightResult(
    val schemaVersion: Int = CURRENT_BRIDGE_SCHEMA,
    val matchId: String,
    val playerId: String,
    val opponentId: String,
    val outcome: String,
    val playerPower: Float,
    val playerSkill: Float,
    val durationSec: Int,
    val createdAtEpochMs: Long
) {
    init {
        require(schemaVersion == CURRENT_BRIDGE_SCHEMA) {
            "Unsupported bridge schema: $schemaVersion"
        }
        require(matchId.isNotBlank()) { "matchId cannot be blank" }
        require(playerId.isNotBlank()) { "playerId cannot be blank" }
        require(opponentId.isNotBlank()) { "opponentId cannot be blank" }
        require(playerId != opponentId) { "playerId and opponentId must differ" }
        require(outcome in OUTCOMES) { "Unsupported fight outcome: $outcome" }
        require(playerPower.isFinite() && playerPower >= 0f) {
            "playerPower must be finite and non-negative"
        }
        require(playerSkill in 0f..1f) { "playerSkill must be between 0 and 1" }
        require(durationSec >= 0) { "durationSec must be non-negative" }
        require(createdAtEpochMs > 0L) { "createdAtEpochMs must be positive" }
    }

    companion object {
        const val PLAYER_WON = "player_won"
        const val OPPONENT_WON = "opponent_won"
        const val DRAW = "draw"
        const val ABANDONED = "abandoned"

        private val OUTCOMES = setOf(PLAYER_WON, OPPONENT_WON, DRAW, ABANDONED)
    }
}
