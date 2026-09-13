package com.pulse.data

import com.pulse.bridge.FightResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FightRewardsTest {

    private fun result(outcome: String): FightResult = FightResult(
        matchId = "match_1",
        playerId = "player_1",
        opponentId = "arena_boss",
        outcome = outcome,
        playerPower = 10f,
        playerSkill = 0.75f,
        durationSec = 42,
        createdAtEpochMs = 5L
    )

    @Test
    fun winUpgradesAttackAndDefence() {
        val reward = FightRewards.fromResult(result(FightResult.PLAYER_WON))!!
        assertTrue(reward.xpGained > 0)
        assertTrue(reward.strengthGain > 0f)      // -> Unity attack / HP
        assertTrue(reward.formMasteryGain > 0f)   // -> Unity defence
    }

    @Test
    fun drawPaysSmallerWithoutStrength() {
        val reward = FightRewards.fromResult(result(FightResult.DRAW))!!
        assertTrue(reward.xpGained < FightRewards.WIN_XP)
        assertEquals(0f, reward.strengthGain, 0f)
        assertTrue(reward.formMasteryGain > 0f)
    }

    @Test
    fun lossPaysTokenWithoutStatBump() {
        val reward = FightRewards.fromResult(result(FightResult.OPPONENT_WON))!!
        assertEquals(FightRewards.LOSS_XP, reward.xpGained)
        assertEquals(0f, reward.strengthGain, 0f)
        assertEquals(0f, reward.formMasteryGain, 0f)
    }

    @Test
    fun abandonedFightEarnsNothing() {
        assertNull(FightRewards.fromResult(result(FightResult.ABANDONED)))
    }

    @Test
    fun applyFightRewardBumpsStatsAndLevel() {
        val state = CloneStateManager.fresh().copy(xp = 400, strength = 10f, formMastery = 50f)
        val reward = FightRewards.fromResult(result(FightResult.PLAYER_WON))!!

        val next = CloneStateManager.applyFightReward(state, reward)

        assertEquals(400 + reward.xpGained, next.xp)
        assertEquals((400 + reward.xpGained) / CloneStateManager.XP_PER_LEVEL + 1, next.level)
        assertEquals(state.strength + reward.strengthGain, next.strength, 0f)
        assertEquals(state.formMastery + reward.formMasteryGain, next.formMastery, 0f)
    }

    @Test
    fun applyFightRewardCapsFormMasteryAt100() {
        val state = CloneStateManager.fresh().copy(formMastery = 99.8f)
        val reward = FightRewards.fromResult(result(FightResult.PLAYER_WON))!!

        val next = CloneStateManager.applyFightReward(state, reward)

        assertEquals(100f, next.formMastery, 0f)
    }
}