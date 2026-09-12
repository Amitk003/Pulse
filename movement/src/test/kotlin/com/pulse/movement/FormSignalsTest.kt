package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormSignalsTest {

    private val config = MovementConfig()

    // Knee valgus: knees clearly narrower than ankles in a front view.

    @Test
    fun symmetricStanceHasNoValgus() {
        val joints = mapOf(
            "left_knee" to Point2D(-0.3, 1.0),
            "right_knee" to Point2D(0.3, 1.0),
            "left_ankle" to Point2D(-0.3, 2.0),
            "right_ankle" to Point2D(0.3, 2.0)
        )
        assertFalse(FormSignals.kneeValgus(joints, config))
    }

    @Test
    fun cavedKneesFlagValgus() {
        val joints = mapOf(
            "left_knee" to Point2D(-0.1, 1.0),
            "right_knee" to Point2D(0.1, 1.0),
            "left_ankle" to Point2D(-1.1, 2.0),
            "right_ankle" to Point2D(1.1, 2.0)
        )
        assertTrue(FormSignals.kneeValgus(joints, config))
    }

    @Test
    fun missingLegNeverFlagsValgus() {
        val joints = mapOf(
            "left_knee" to Point2D(-0.1, 1.0),
            "left_ankle" to Point2D(-1.1, 2.0)
        )
        assertFalse(FormSignals.kneeValgus(joints, config))
    }

    @Test
    fun sideViewOverlapNeverFlagsValgus() {
        // Side view stacks both legs, so both gaps are near zero.
        val joints = mapOf(
            "left_knee" to Point2D(0.5, 1.0),
            "right_knee" to Point2D(0.52, 1.0),
            "left_ankle" to Point2D(0.1, 2.0),
            "right_ankle" to Point2D(0.12, 2.0)
        )
        assertFalse(FormSignals.kneeValgus(joints, config))
    }

    // Squat back-round: shoulders below hips without a deep knee bend.

    @Test
    fun uprightSquatHasNoBackRound() {
        val joints = mapOf(
            "left_shoulder" to Point2D(0.0, -1.0),
            "left_hip" to Point2D(0.0, 0.0)
        )
        assertFalse(FormSignals.squatBackRound(joints, 90.0, config))
    }

    @Test
    fun collapsedShallowSquatFlagsBackRound() {
        val joints = mapOf(
            "left_shoulder" to Point2D(0.1, 0.5),
            "left_hip" to Point2D(0.0, 0.0)
        )
        assertTrue(FormSignals.squatBackRound(joints, 120.0, config))
    }

    @Test
    fun deepLeaningSquatPassesBackRound() {
        // Deep reps may lean, so the knee guard keeps them passing.
        val joints = mapOf(
            "left_shoulder" to Point2D(0.1, 0.5),
            "left_hip" to Point2D(0.0, 0.0)
        )
        assertFalse(FormSignals.squatBackRound(joints, 85.0, config))
    }

    // Hinge rounded-back: horizontal torso without much hip hinge.

    @Test
    fun stiffSpineBendFlagsRoundedBack() {
        val joints = mapOf(
            "left_shoulder" to Point2D(0.9, 0.2),
            "left_hip" to Point2D(0.0, 0.0)
        )
        assertTrue(FormSignals.hingeRoundedBack(joints, 130.0, config))
    }

    @Test
    fun deepHipHingePassesRoundedBack() {
        val joints = mapOf(
            "left_shoulder" to Point2D(0.9, 0.2),
            "left_hip" to Point2D(0.0, 0.0)
        )
        assertFalse(FormSignals.hingeRoundedBack(joints, 95.0, config))
    }

    // Push-up sagging: the shoulder-hip-knee line must stay near straight.

    @Test
    fun straightPlankHasNoSagging() {
        val joints = mapOf(
            "left_shoulder" to Point2D(0.0, 1.0),
            "left_hip" to Point2D(1.0, 1.0),
            "left_knee" to Point2D(2.0, 1.0)
        )
        assertFalse(FormSignals.pushUpSagging(joints, config))
    }

    @Test
    fun droppedHipsFlagSagging() {
        val joints = mapOf(
            "left_shoulder" to Point2D(0.0, 1.0),
            "left_hip" to Point2D(1.0, 1.3),
            "left_knee" to Point2D(2.0, 1.0)
        )
        assertTrue(FormSignals.pushUpSagging(joints, config))
    }

    // Dispatcher picks the right checks per exercise.

    @Test
    fun faultsForRoutesByExercise() {
        val squatJoints = mapOf(
            "left_knee" to Point2D(-0.1, 1.0),
            "right_knee" to Point2D(0.1, 1.0),
            "left_ankle" to Point2D(-1.1, 2.0),
            "right_ankle" to Point2D(1.1, 2.0),
            "left_shoulder" to Point2D(0.0, -1.0),
            "left_hip" to Point2D(0.0, 0.0)
        )
        val squat = FormSignals.faultsFor(Exercise.SQUAT, squatJoints, 90.0, config)
        assertTrue(squat.kneeValgus)
        assertFalse(squat.backRound)
        assertFalse(squat.sagging)

        val pushJoints = mapOf(
            "left_shoulder" to Point2D(0.0, 1.0),
            "left_hip" to Point2D(1.0, 1.3),
            "left_knee" to Point2D(2.0, 1.0)
        )
        val push = FormSignals.faultsFor(Exercise.PUSH_UP, pushJoints, 85.0, config)
        assertTrue(push.sagging)
        assertFalse(push.kneeValgus)
        assertEquals(FormSignals.FrameFaults(), FormSignals.faultsFor(Exercise.HINGE, emptyMap(), null, config))
    }
}
