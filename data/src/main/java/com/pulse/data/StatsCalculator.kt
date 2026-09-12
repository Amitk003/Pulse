package com.pulse.data

/**
 * Secondary clone stats from plain session history. Pure math.
 *
 * Consistency counts recent training. Form mastery averages recent
 * scores. Recovery looks at rest gaps: about one day of rest scores
 * best, back to back sessions score low. All results fit 0 to 1.
 */
object StatsCalculator {

    private const val SESSIONS_FOR_FULL = 5f
    private const val FULL_REST_HOURS = 24.0

    fun consistency(sessionsLast7Days: Int): Float {
        return (sessionsLast7Days / SESSIONS_FOR_FULL).coerceIn(0f, 1f)
    }

    fun formMastery(lastScores: List<Int>): Float {
        if (lastScores.isEmpty()) {
            return 0f
        }
        return lastScores.average().toFloat().coerceIn(0f, 100f)
    }

    fun averageGapHours(orderedTimestamps: List<Long>): Double? {
        if (orderedTimestamps.size < 2) {
            return null
        }
        val gaps = orderedTimestamps.zipWithNext { a, b -> (b - a) / 3_600_000.0 }
        return gaps.average()
    }

    fun recovery(averageGapHours: Double?): Float {
        if (averageGapHours == null) {
            return 1f
        }
        return (averageGapHours / FULL_REST_HOURS).coerceIn(0.0, 1.0).toFloat()
    }

    /** Pack mistake lists into one text column. Ids never hold newline or comma. */
    fun encodeMemory(memory: List<List<String>>): String {
        return memory.takeLast(10).joinToString("\n") { it.joinToString(",") }
    }

    fun decodeMemory(raw: String): List<List<String>> {
        if (raw.isBlank()) {
            return emptyList()
        }
        return raw.lines()
            .map { line -> line.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
            .filter { it.isNotEmpty() }
            .takeLast(10)
    }
}
