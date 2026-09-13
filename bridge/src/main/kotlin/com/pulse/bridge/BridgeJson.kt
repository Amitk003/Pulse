package com.pulse.bridge

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned JSON (de)serialization for the bridge contracts.
 *
 * This is the single place that decides the exact wire format that Pulse and
 * Unity exchange. Key names mirror the Kotlin data class fields so the Unity
 * C# side (JsonUtility) and the Android side never drift apart. Field order is
 * not part of the contract; only these exact key names and value types are.
 */
object BridgeJson {

    /**
     * Serialize a [PlayerSnapshot] to the JSON string Pulse sends into Unity.
     */
    fun playerSnapshotToJson(snapshot: PlayerSnapshot): String =
        JSONObject().apply {
            put("schemaVersion", snapshot.schemaVersion)
            put("playerId", snapshot.playerId)
            put("cloneLevel", snapshot.cloneLevel)
            put("strength", snapshot.strength)
            put("formMastery", snapshot.formMastery)
            put("consistency", snapshot.consistency)
            put("recovery", snapshot.recovery)
            put("gameXp", snapshot.gameXp)
            put("unlockedAbilities", JSONArray().apply {
                snapshot.unlockedAbilities.forEach { put(it) }
            })
            put("updatedAtEpochMs", snapshot.updatedAtEpochMs)
        }.toString()

    /**
     * Parse a [PlayerSnapshot] from the [json] produced by Pulse (or test data).
     * Returns null when the payload is missing fields or violates the contract
     * (for example an out-of-range stat), so callers can refuse bad data safely.
     */
    fun playerSnapshotFromJson(json: String): PlayerSnapshot? = try {
        val o = JSONObject(json)
        PlayerSnapshot(
            schemaVersion = o.getInt("schemaVersion"),
            playerId = o.getString("playerId"),
            cloneLevel = o.getInt("cloneLevel"),
            strength = o.getDouble("strength").toFloat(),
            formMastery = o.getDouble("formMastery").toFloat(),
            consistency = o.getDouble("consistency").toFloat(),
            recovery = o.getDouble("recovery").toFloat(),
            gameXp = o.getInt("gameXp"),
            unlockedAbilities = o.optJSONArray("unlockedAbilities")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            updatedAtEpochMs = o.getLong("updatedAtEpochMs")
        )
    } catch (_: Exception) {
        null
    }

    /**
     * Serialize a [FightResult] to the JSON string Unity sends back to Pulse.
     */
    fun fightResultToJson(result: FightResult): String =
        JSONObject().apply {
            put("schemaVersion", result.schemaVersion)
            put("matchId", result.matchId)
            put("playerId", result.playerId)
            put("opponentId", result.opponentId)
            put("outcome", result.outcome)
            put("playerPower", result.playerPower)
            put("playerSkill", result.playerSkill)
            put("durationSec", result.durationSec)
            put("createdAtEpochMs", result.createdAtEpochMs)
        }.toString()

    /**
     * Parse a [FightResult] from the [json] produced by Unity. Returns null when
     * a required field is missing or the outcome is not a known one, so Pulse can
     * discard malformed / adversarial results instead of trusting them.
     */
    fun fightResultFromJson(json: String): FightResult? = try {
        val o = JSONObject(json)
        FightResult(
            schemaVersion = o.getInt("schemaVersion"),
            matchId = o.getString("matchId"),
            playerId = o.getString("playerId"),
            opponentId = o.getString("opponentId"),
            outcome = o.getString("outcome"),
            playerPower = o.getDouble("playerPower").toFloat(),
            playerSkill = o.getDouble("playerSkill").toFloat(),
            durationSec = o.getInt("durationSec"),
            createdAtEpochMs = o.getLong("createdAtEpochMs")
        )
    } catch (_: Exception) {
        null
    }
}