package com.pulse.camera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.pulse.movement.Landmark
import com.pulse.movement.Point2D

/**
 * Jetpack Compose Canvas overlay drawing BlazePose 33 body keypoints
 * and skeleton bone connections in real time.
 */
@Composable
fun PoseOverlayCanvas(
    joints: Map<String, Point2D>,
    landmarks: List<Landmark>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val pointColor = Color(0xFF00F2FE)
        val lineColor = Color(0xFF4FACFE)

        // Draw connections between tracked joints
        val bonePairs = listOf(
            Pair("left_shoulder", "left_elbow"),
            Pair("left_elbow", "left_wrist"),
            Pair("left_shoulder", "left_hip"),
            Pair("left_hip", "left_knee"),
            Pair("left_knee", "left_ankle"),
            Pair("right_shoulder", "right_elbow"),
            Pair("right_elbow", "right_wrist"),
            Pair("right_shoulder", "right_hip"),
            Pair("right_hip", "right_knee"),
            Pair("right_knee", "right_ankle"),
            Pair("left_shoulder", "right_shoulder"),
            Pair("left_hip", "right_hip")
        )

        for ((p1Key, p2Key) in bonePairs) {
            val pt1 = joints[p1Key]
            val pt2 = joints[p2Key]
            if (pt1 != null && pt2 != null) {
                drawLine(
                    color = lineColor,
                    start = Offset((pt1.x * width).toFloat(), (pt1.y * height).toFloat()),
                    end = Offset((pt2.x * width).toFloat(), (pt2.y * height).toFloat()),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw joint nodes
        for ((_, point) in joints) {
            drawCircle(
                color = pointColor,
                radius = 10f,
                center = Offset((point.x * width).toFloat(), (point.y * height).toFloat())
            )
        }
    }
}
