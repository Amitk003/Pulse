package com.pulse.bridge

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class BridgeContractsTest {

    @Test
    fun validSnapshotIsAccepted() {
        val snapshot = PlayerSnapshot(
            playerId = "player_1",
            cloneLevel = 2,
            strength = 12.5f,
            formMastery = 80f,
            consistency = 0.6f,
            recovery = 0.9f,
            gameXp = 300,
            unlockedAbilities = listOf("quick_step"),
            updatedAtEpochMs = 1L
        )

        assertEquals(CURRENT_BRIDGE_SCHEMA, snapshot.schemaVersion)
    }

    @Test
    fun snapshotRejectsBadProgressRanges() {
        assertFailsWith<IllegalArgumentException> {
            PlayerSnapshot(
                playerId = "player_1",
                cloneLevel = 2,
                strength = 12.5f,
                formMastery = 101f,
                consistency = 0.6f,
                recovery = 0.9f,
                gameXp = 300,
                unlockedAbilities = emptyList(),
                updatedAtEpochMs = 1L
            )
        }
    }

    @Test
    fun snapshotRejectsUnstableAbilityIds() {
        assertFailsWith<IllegalArgumentException> {
            PlayerSnapshot(
                playerId = "player_1",
                cloneLevel = 2,
                strength = 12.5f,
                formMastery = 80f,
                consistency = 0.6f,
                recovery = 0.9f,
                gameXp = 300,
                unlockedAbilities = listOf("Quick Step"),
                updatedAtEpochMs = 1L
            )
        }
    }

    @Test
    fun fightResultAcceptsKnownOutcome() {
        val result = FightResult(
            matchId = "match_1",
            playerId = "player_1",
            opponentId = "player_2",
            outcome = FightResult.PLAYER_WON,
            playerPower = 10f,
            playerSkill = 0.75f,
            durationSec = 42,
            createdAtEpochMs = 1L
        )

        assertEquals(FightResult.PLAYER_WON, result.outcome)
    }

    @Test
    fun fightResultRejectsUnknownOutcome() {
        assertFailsWith<IllegalArgumentException> {
            FightResult(
                matchId = "match_1",
                playerId = "player_1",
                opponentId = "player_2",
                outcome = "winner",
                playerPower = 10f,
                playerSkill = 0.75f,
                durationSec = 42,
                createdAtEpochMs = 1L
            )
        }
    }

    // --- BridgeJson: versioned JSON wire format is the contract Unity mirrors ---

    @Test
    fun snapshotJsonRoundTrips() {
        val snapshot = PlayerSnapshot(
            playerId = "player_1",
            cloneLevel = 3,
            strength = 75f,
            formMastery = 60f,
            consistency = 0.7f,
            recovery = 0.9f,
            gameXp = 150,
            unlockedAbilities = listOf("quick_step"),
            updatedAtEpochMs = 1234L
        )

        val json = BridgeJson.playerSnapshotToJson(snapshot)
        assertEquals(snapshot, BridgeJson.playerSnapshotFromJson(json))
    }

    @Test
    fun snapshotJsonUsesContractKeys() {
        val snapshot = PlayerSnapshot(
            playerId = "player_1", cloneLevel = 3, strength = 75f, formMastery = 60f,
            consistency = 0.7f, recovery = 0.9f, gameXp = 150,
            unlockedAbilities = emptyList(), updatedAtEpochMs = 1L
        )
        val json = BridgeJson.playerSnapshotToJson(snapshot)
        for (key in listOf(
            "schemaVersion", "playerId", "cloneLevel", "strength", "formMastery",
            "consistency", "recovery", "gameXp", "unlockedAbilities", "updatedAtEpochMs"
        )) {
            assert(json.contains("\"$key\"")) { "missing snapshot key '$key' in $json" }
        }
    }

    @Test
    fun snapshotFromMalformedOrInvalidJsonReturnsNull() {
        assertEquals(null, BridgeJson.playerSnapshotFromJson("not json"))
        // Out-of-range stat (formMastery 200) must be rejected by the contract.
        val invalid = "{\"schemaVersion\":1,\"playerId\":\"p\",\"cloneLevel\":1,\"strength\":10.0," +
            "\"formMastery\":200.0,\"consistency\":0.5,\"recovery\":0.5,\"gameXp\":0," +
            "\"unlockedAbilities\":[],\"updatedAtEpochMs\":1}"
        assertEquals(null, BridgeJson.playerSnapshotFromJson(invalid))
    }

    @Test
    fun fightResultJsonRoundTrips() {
        val result = FightResult(
            matchId = "match_1",
            playerId = "player_1",
            opponentId = "arena_boss",
            outcome = FightResult.PLAYER_WON,
            playerPower = 10f,
            playerSkill = 0.75f,
            durationSec = 42,
            createdAtEpochMs = 5L
        )

        val json = BridgeJson.fightResultToJson(result)
        assertEquals(result, BridgeJson.fightResultFromJson(json))
    }

    @Test
    fun fightResultJsonUsesContractKeys() {
        val result = FightResult(
            matchId = "match_1", playerId = "player_1", opponentId = "arena_boss",
            outcome = FightResult.PLAYER_WON, playerPower = 10f, playerSkill = 0.75f,
            durationSec = 42, createdAtEpochMs = 5L
        )
        val json = BridgeJson.fightResultToJson(result)
        for (key in listOf(
            "schemaVersion", "matchId", "playerId", "opponentId", "outcome",
            "playerPower", "playerSkill", "durationSec", "createdAtEpochMs"
        )) {
            assert(json.contains("\"$key\"")) { "missing fight-result key '$key' in $json" }
        }
    }

    @Test
    fun fightResultFromMalformedJsonReturnsNull() {
        assertEquals(null, BridgeJson.fightResultFromJson("not json"))
        assertEquals(null, BridgeJson.fightResultFromJson("{}"))
    }
}
