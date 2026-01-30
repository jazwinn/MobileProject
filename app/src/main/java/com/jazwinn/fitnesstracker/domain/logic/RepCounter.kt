package com.jazwinn.fitnesstracker.domain.logic

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.jazwinn.fitnesstracker.domain.model.ExerciseType
import kotlin.math.abs
import kotlin.math.atan2

class RepCounter(private val exerciseType: ExerciseType) {

    private var state = RepState.UP
    var repCount = 0
        private set
    
    // Simple feedback message
    var feedback: String = "Get into position"
        private set

    enum class RepState {
        UP,
        DOWN
    }

    fun processPose(pose: Pose) {
        try {
            when (exerciseType) {
                ExerciseType.PUSH_UP -> processPushUp(pose)
                ExerciseType.SIT_UP -> processSitUp(pose)
            }
        } catch (e: Exception) {
            // Landmark might be missing
            feedback = "Make sure full body is visible"
        }
    }

    private fun processPushUp(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        
        // Fallback to right side if left is not visible (or merge logic)
        // For MVP, we stick to left side or assume one side is sufficient.
        
        if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            val elbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
            
            // Push-up Logic
            // UP: Angle > 160
            // DOWN: Angle < 90
            
            if (state == RepState.UP) {
                if (elbowAngle < 90) {
                    state = RepState.DOWN
                    feedback = "Push Up!"
                } else {
                    feedback = "Go Lower"
                }
            } else if (state == RepState.DOWN) {
                if (elbowAngle > 160) {
                    state = RepState.UP
                    repCount++
                    feedback = "Good Job!"
                } else {
                    feedback = "Push harder"
                }
            }
        } else {
            feedback = "Adjust visible side"
        }
    }

    private fun processSitUp(pose: Pose) {
        // Sit-up logic: Hip-Knee-Shoulder angle? Or Shoulder-Hip-Knee.
        // Simplified: Shoulder-Hip-Knee angle.
        // Lying down: Angle ~ 180 (straight body) or 130 (knees bent)
        // Sitting up: Angle < 60 (torso close to thighs)
        
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)

        if (leftShoulder != null && leftHip != null && leftKnee != null) {
            val hipAngle = calculateAngle(leftShoulder, leftHip, leftKnee)
            
            if (state == RepState.UP) { // Actually "Lying Down" might be the start state, but let's say UP = Started position (Lying)
               // Wait, normally UP means Sit Up.
               // Let's redefine: RepState.UP = Sitting Up, RepState.DOWN = Lying Down.
               // Start in DOWN (Lying).
               
               // Logic needs to be robust. Let's assume start state is "neutral".
            }
            
            // Doing a simplified version:
            // DOWN (Lying): Hip Angle > 120
            // UP (Sitting): Hip Angle < 60
            
            if (hipAngle > 120) {
                 if (state == RepState.UP) {
                     // Was UP, now DOWN -> Rep Complete? Or Start?
                     // Usually rep counts when you return to start.
                     state = RepState.DOWN
                     feedback = "Sit Up!"
                 }
            } else if (hipAngle < 60) {
                 if (state == RepState.DOWN) {
                     state = RepState.UP
                     repCount++
                     feedback = "Down we go"
                 }
            }
        }
    }

    private fun calculateAngle(
        firstPoint: PoseLandmark,
        middlePoint: PoseLandmark,
        lastPoint: PoseLandmark
    ): Double {
        val result = Math.toDegrees(
            atan2(
                (lastPoint.position.y - middlePoint.position.y).toDouble(),
                (lastPoint.position.x - middlePoint.position.x).toDouble()
            ) - atan2(
                (firstPoint.position.y - middlePoint.position.y).toDouble(),
                (firstPoint.position.x - middlePoint.position.x).toDouble()
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
        state = RepState.UP // Default start
        feedback = "Ready"
    }
}
