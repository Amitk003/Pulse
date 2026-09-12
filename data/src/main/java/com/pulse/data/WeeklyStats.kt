package com.pulse.data

/**
 * Pure presentation stats built only from real stored sessions.
 * No fake data: an empty history simply yields zeros.
 */
data class WeeklyStats(
    /** Trained at least one set on this day, Monday first. */
    val daysTrained: List<Boolean>,
    /** Sessions in the current Monday-Sunday week. */
    val weekSessionCount: Int,
    /** Consecutive calendar days with at least one set, ending today. */
    val streakDays: Int,
    /** Distinct training days in the last 7*24h (rolling). */
    val rollingDayCount: Int
)

/**
 * Builds [WeeklyStats] from session rows. Pure and unit-testable.
 * A day counts as trained when at least one session happened on
 * that local calendar day.
 */
object WeeklyStatsBuilder {

    fun build(sessions: List<SessionEntity>, now: Long): WeeklyStats {
        val zone = java.util.TimeZone.getDefault()
        val dayMs = 24 * 60 * 60 * 1000L

        fun dayIndex(timestamp: Long): Int {
            val offset = zone.getOffset(timestamp)
            return ((timestamp + offset) / dayMs).toInt()
        }

        val todayIndex = dayIndex(now)
        val days = MutableList(7) { false }
        var weekCount = 0

        // Walk back 6 days from today (rolling window of local days).
        val trainedDays = HashSet<Int>()
        for (s in sessions) {
            trainedDays.add(dayIndex(s.timestamp))
        }

        // Current calendar week starts Monday. Find its first day index.
        val calendar = java.util.Calendar.getInstance(zone)
        calendar.timeInMillis = now
        // Calendar.SUNDAY=1 .. SATURDAY=7; map Monday-first index 0..6.
        val dow = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val mondayOffset = ((dow + 5) % 7) // Mon->0, Sun->6
        val mondayIndex = todayIndex - mondayOffset

        for (s in sessions) {
            val idx = dayIndex(s.timestamp)
            val rel = idx - mondayIndex
            if (rel in 0..6) {
                weekCount++
            }
        }
        for (i in 0..6) {
            if (trainedDays.contains(mondayIndex + i)) {
                days[i] = true
            }
        }

        // Streak: consecutive trained local days ending today (or yesterday
        // if today has no session yet, so the streak survives until midnight).
        var streak = 0
        var cursor = if (trainedDays.contains(todayIndex)) todayIndex else todayIndex - 1
        while (trainedDays.contains(cursor)) {
            streak++
            cursor--
        }

        // Rolling 7*24h distinct-day count, mirrors consistency math.
        val weekStart = now - 7 * dayMs
        val rolling = sessions.filter { it.timestamp >= weekStart }
            .map { dayIndex(it.timestamp) }
            .distinct()
            .size

        return WeeklyStats(
            daysTrained = days.toList(),
            weekSessionCount = weekCount,
            streakDays = streak,
            rollingDayCount = rolling
        )
    }
}
