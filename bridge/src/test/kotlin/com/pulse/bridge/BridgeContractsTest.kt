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
}
