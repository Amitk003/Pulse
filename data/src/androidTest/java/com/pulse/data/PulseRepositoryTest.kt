package com.pulse.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.movement.SetResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real phone. Proves Room stores sets and the clone
 * grows, decays, and pays comeback bonus on the device itself.
 */
@RunWith(AndroidJUnit4::class)
class PulseRepositoryTest {

    private fun repo(): PulseRepository {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("pulse.db")
        return PulseRepository(context)
    }

    private fun result(): SetResult {
        return SetResult(
            exercise = "squat",
            reps = 10,
            formScore = 80,
            mistakes = listOf("shallow"),
            durationSec = 60
        )
    }

    @Test
    fun saveGrowsClone() = runTest {
        val repository = repo()
        val info = repository.saveResult(result())
        assertEquals(800, info.xpGained)
        val clone = repository.cloneFlow.first()
        assertEquals(2, clone.level)
        assertEquals(800, clone.xp)
        assertEquals(listOf(listOf("shallow")), clone.mistakeMemory)
    }

    @Test
    fun breakDecaysThenComebackPays() = runTest {
        val repository = repo()
        repository.saveResult(result())
        repository.shiftLastTrainedBy(9)
        val info = repository.saveResult(result())
        assertTrue(info.decayed)
        assertTrue(info.comebackApplied)
        assertEquals(1400, info.xpGained)
        val clone = repository.cloneFlow.first()
        assertEquals(1, clone.comebackLeft)
    }
}
