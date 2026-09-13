package com.pulse.data

import com.pulse.bridge.FightResult

/**
 * How a real arena fight pays back into the clone.
 *
 * A win is worth the most and upgrades both attack and defence paths: strength
 * feeds attack (and HP) while formMastery feeds defence in the Unity arena. A
 * draw pays a smaller amount, a loss pays a token amount, and an abandoned match
 * is not a fair result so it pays nothing and must not count as a win or a loss.
 */
data class FightReward(
    val xpGained: Int,
    val strengthGain: Float,
    val formMasteryGain: Float
)

object FightRewards {

    const val WIN_XP = 120
    const val WIN_STRENGTH = 1.5f     // -> Unity attack (and HP)
    const val WIN_FORM_MASTERY = 0.8f // -> Unity defence

    const val DRAW_XP = 60
    const val DRAW_FORM_MASTERY = 0.3f

    const val LOSS_XP = 30

    /**
     * Convert a Unity fight result into the reward Pulse applies.
     * Returns null when the match was abandoned or has an unknown outcome, in
     * which case no XP or stat credit is given.
     */
    fun fromResult(result: FightResult): FightReward? = when (result.outcome) {
        FightResult.PLAYER_WON -> FightReward(WIN_XP, WIN_STRENGTH, WIN_FORM_MASTERY)
        FightResult.DRAW -> FightReward(DRAW_XP, 0f, DRAW_FORM_MASTERY)
        FightResult.OPPONENT_WON -> FightReward(LOSS_XP, 0f, 0f)
        else -> null // abandoned / unknown -> no reward, no credit
    }
}