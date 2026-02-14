package com.jazwinn.fitnesstracker.domain.logic

import com.jazwinn.fitnesstracker.ui.camera.Keypoint
import com.jazwinn.fitnesstracker.domain.model.ExerciseType
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Rep counter using COCO keypoint indices from YOLOv8 pose detection.
 * 
 * Uses angle-based state machine to count push-up and sit-up repetitions.
 * 
 * COCO Keypoint indices used:
 * 5=L_Shoulder, 6=R_Shoulder, 7=L_Elbow, 8=R_Elbow,
 * 9=L_Wrist, 10=R_Wrist, 11=L_Hip, 12=R_Hip,
 * 13=L_Knee, 14=R_Knee
 */
class RepCounter(private val exerciseType: ExerciseType) {

    private var state = RepState.UP
    var repCount = 0
        private set

    var feedback: String = "Get into position"
        private set

    enum class RepState {
        UP,
        DOWN
    }

    // COCO keypoint indices
    companion object {
        const val LEFT_SHOULDER = 5
        const val RIGHT_SHOULDER = 6
        const val LEFT_ELBOW = 7
        const val RIGHT_ELBOW = 8
        const val LEFT_WRIST = 9
        const val RIGHT_WRIST = 10
        const val LEFT_HIP = 11
        const val RIGHT_HIP = 12
        const val LEFT_KNEE = 13
        const val RIGHT_KNEE = 14
    }

    /**
     * Process a list of 17 COCO keypoints for rep counting.
     */
    fun processKeypoints(keypoints: List<Keypoint>) {
        if (keypoints.size < 15) {
            feedback = "Make sure full body is visible"
            return
        }

        try {
            when (exerciseType) {
                ExerciseType.PUSH_UP -> processPushUp(keypoints)
                ExerciseType.SIT_UP -> processSitUp(keypoints)
            }
        } catch (e: Exception) {
            feedback = "Make sure full body is visible"
        }
    }

    private fun processPushUp(keypoints: List<Keypoint>) {
        // Try left side first, fall back to right side
        val (shoulder, elbow, wrist) = pickBestSide(
            keypoints, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST,
            RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST
        ) ?: run {
            feedback = "Adjust visible side"
            return
        }

        val elbowAngle = calculateAngle(shoulder, elbow, wrist)
        android.util.Log.d("RepCounter", "PushUp Angle: $elbowAngle | State: $state")

        // Push-up state machine
        // UP: arms extended, angle > 150°
        // DOWN: arms bent, angle < 100°
        if (state == RepState.UP) {
            if (elbowAngle < 100) {
                state = RepState.DOWN
                feedback = "Push Up!"
                android.util.Log.d("RepCounter", "State changed to DOWN")
            } else {
                feedback = "Go Lower"
            }
        } else if (state == RepState.DOWN) {
            if (elbowAngle > 150) {
                state = RepState.UP
                repCount++
                feedback = "Good Job!"
                android.util.Log.d("RepCounter", "Rep detected! Count: $repCount")
            } else {
                feedback = "Push harder"
            }
        }
    }

    private fun processSitUp(keypoints: List<Keypoint>) {
        // Try left side first, fall back to right
        val (shoulder, hip, knee) = pickBestSide(
            keypoints, LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE,
            RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE
        ) ?: run {
            feedback = "Make sure full body is visible"
            return
        }

        val hipAngle = calculateAngle(shoulder, hip, knee)
        android.util.Log.d("RepCounter", "SitUp Angle: $hipAngle | State: $state")

        // Sit-up state machine
        // DOWN (lying): hip angle > 110°
        // UP (sitting): hip angle < 70°
        if (state == RepState.UP) {
            if (hipAngle > 110) { 
                 state = RepState.DOWN
                 feedback = "Sit Up!"
                 android.util.Log.d("RepCounter", "State changed to DOWN (Lying)")
            }
        } else if (state == RepState.DOWN) {
            if (hipAngle < 70) {
                state = RepState.UP
                repCount++
                feedback = "Down we go"
                android.util.Log.d("RepCounter", "Rep detected! Count: $repCount")
            }
        }
    }

    /**
     * Pick the body side with higher average keypoint confidence.
     * Returns triple of (first, middle, last) keypoints, or null if both sides are too low confidence.
     */
    private fun pickBestSide(
        keypoints: List<Keypoint>,
        leftA: Int, leftB: Int, leftC: Int,
        rightA: Int, rightB: Int, rightC: Int
    ): Triple<Keypoint, Keypoint, Keypoint>? {
        val minConf = 0.15f

        val leftConf = (keypoints[leftA].confidence + keypoints[leftB].confidence + keypoints[leftC].confidence) / 3
        val rightConf = (keypoints[rightA].confidence + keypoints[rightB].confidence + keypoints[rightC].confidence) / 3

        return when {
            leftConf >= minConf && leftConf >= rightConf -> Triple(keypoints[leftA], keypoints[leftB], keypoints[leftC])
            rightConf >= minConf -> Triple(keypoints[rightA], keypoints[rightB], keypoints[rightC])
            else -> null
        }
    }

    /**
     * Calculate angle at the middle point formed by three keypoints (in degrees).
     */
    private fun calculateAngle(
        firstPoint: Keypoint,
        middlePoint: Keypoint,
        lastPoint: Keypoint
    ): Double {
        val result = Math.toDegrees(
            atan2(
                (lastPoint.y - middlePoint.y).toDouble(),
                (lastPoint.x - middlePoint.x).toDouble()
            ) - atan2(
                (firstPoint.y - middlePoint.y).toDouble(),
                (firstPoint.x - middlePoint.x).toDouble()
            )
        )
        var angle = abs(result)
        if (angle > 180) {
            angle = 360.0 - angle
        }
        return angle
    }

    fun reset() {
        repCount = 0
        state = RepState.UP
        feedback = "Ready"
    }
}
