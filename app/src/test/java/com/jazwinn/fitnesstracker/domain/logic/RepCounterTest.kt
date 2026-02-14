package com.jazwinn.fitnesstracker.domain.logic

import com.jazwinn.fitnesstracker.domain.model.ExerciseType
import com.jazwinn.fitnesstracker.ui.camera.Keypoint
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RepCounterTest {

    private lateinit var pushUpCounter: RepCounter
    private lateinit var sitUpCounter: RepCounter

    @Before
    fun setup() {
        pushUpCounter = RepCounter(ExerciseType.PUSH_UP)
        sitUpCounter = RepCounter(ExerciseType.SIT_UP)
    }

    private fun createPose(
        shoulderY: Float, elbowY: Float, wristY: Float, 
        hipY: Float, kneeY: Float,
        side: String = "left"
    ): List<Keypoint> {
        // Init all keypoints with low confidence
        val keypoints = MutableList(17) { Keypoint(0f, 0f, 0.0f) }
        
        // Helper to set a keypoint
        fun setKp(idx: Int, x: Float, y: Float) {
            keypoints[idx] = Keypoint(x, y, 0.9f)
        }

        // We only care about Y coordinates for simple angle simulation in this test helper
        // X coordinates set to create valid angles
        
        if (side == "left") {
            // Left Push-up: Shoulder(5), Elbow(7), Wrist(9)
            // Vertical arm (UP): S(0,0), E(0,10), W(0,20) -> angle 180
            // Bent arm (DOWN): S(0,0), E(10,10), W(0,20) -> angle < 90
            
            // Left Sit-up: Shoulder(5), Hip(11), Knee(13)
            // Lying (DOWN): S(0,0), H(0,10), K(0,20) -> angle 180
            // Sitting (UP): S(10,0), H(0,10), K(0,20) -> angle < 60
            
            setKp(5, 0.5f, shoulderY)
            setKp(7, 0.5f, elbowY) // X is varying for angles
            setKp(9, 0.5f, wristY)
            
            setKp(11, 0.5f, hipY)
            setKp(13, 0.5f, kneeY)
        }

        return keypoints
    }
    
    private fun createPushUpPose(angle: Double): List<Keypoint> {
        val keypoints = MutableList(17) { Keypoint(0f, 0f, 0.0f) }
        
        // Center at 0.5, 0.5
        // Shoulder at 0.5, 0.2
        val sx = 0.5f; val sy = 0.2f
        
        // Elbow at 0.5, 0.5? No, let's use simple trig
        // Shoulder(0, 1), Elbow(0,0), Wrist(x, y)
        // Let's keep it simple. RepCounter calculates angle at MIDDLE point.
        // PushUp: Angle at Elbow (Shoulder-Elbow-Wrist)
        
        // Shoulder
        keypoints[5] = Keypoint(0f, 1f, 0.9f) 
        // Elbow
        keypoints[7] = Keypoint(0f, 0f, 0.9f)
        // Wrist - rotate based on angle
        val rad = Math.toRadians(angle)
        // If angle is 180, wrist is at (0, -1)
        // If angle is 90, wrist is at (1, 0)
        // The formula in RepCounter:
        // atan2(last.y - mid.y, last.x - mid.x) - atan2(first.y - mid.y, first.x - mid.x)
        // first=Shoulder, mid=Elbow, last=Wrist
        // Shoulder relative to elbow: (0, 1) -> angle 90 deg
        // We want total angle `a`. 
        // Wrist relative angle should be 90 + a?
        
        // Let's just hardcode verified coordinates for states
        
        return keypoints
    }

    @Test
    fun testPushUpCounts() {
        // UP State: Angle > 160
        // Shoulder(0, 10), Elbow(0, 0), Wrist(0, -10) -> Straight line, 180 deg
        val upPose = mockKeypoints(
            5 to Keypoint(0f, 10f, 0.9f),
            7 to Keypoint(0f, 0f, 0.9f),
            9 to Keypoint(0f, -10f, 0.9f)
        )
        
        // DOWN State: Angle < 90
        // Shoulder(0, 10), Elbow(0, 0), Wrist(10, 0) -> 90 deg. Let's make it acute.
        // Wrist(5, 5) relative to elbow?
        // S(0,10), E(0,0). Vector E->S is (0,10), angle 90.
        // Vector E->W. We want difference to be < 90.
        // If E->W is (1, 1) -> 45 deg. Diff is 45.
        val downPose = mockKeypoints(
            5 to Keypoint(0f, 10f, 0.9f),
            7 to Keypoint(0f, 0f, 0.9f),
            9 to Keypoint(10f, 10f, 0.9f) // Creates sharp angle
        )

        // Start UP
        pushUpCounter.processKeypoints(upPose)
        assertEquals(0, pushUpCounter.repCount)
        assertEquals("Go Lower", pushUpCounter.feedback)

        // Go DOWN
        pushUpCounter.processKeypoints(downPose)
        assertEquals(0, pushUpCounter.repCount)
        assertEquals("Push Up!", pushUpCounter.feedback)

        // Return UP (Complete Rep)
        pushUpCounter.processKeypoints(upPose)
        assertEquals(1, pushUpCounter.repCount)
        assertEquals("Good Job!", pushUpCounter.feedback)
    }

    @Test
    fun testSitUpCounts() {
        // SitUp: Angle at Hip (Shoulder-Hip-Knee)
        
        // DOWN (Lying): Angle > 120
        // Shoulder(-10, 0), Hip(0,0), Knee(10, 0) -> 180 deg
        val lyingPose = mockKeypoints(
            5 to Keypoint(-10f, 0f, 0.9f),
            11 to Keypoint(0f, 0f, 0.9f),
            13 to Keypoint(10f, 0f, 0.9f)
        )

        // UP (Sitting): Angle < 60
        // Shoulder(0, 10), Hip(0,0), Knee(10, 0) -> 90 deg. Need sharper.
        // Shoulder(8, 2), Hip(0,0), Knee(10, 0).
        // Vector H->S (8, 2), H->K (10, 0).
        // atan2(0, 10) = 0.
        // atan2(2, 8) = ~14 deg. Diff = 14 deg.
        val sittingPose = mockKeypoints(
            5 to Keypoint(8f, 2f, 0.9f),
            11 to Keypoint(0f, 0f, 0.9f),
            13 to Keypoint(10f, 0f, 0.9f)
        )

        // Start DOWN (Lying) for SitUp?
        // Logic says:
        // if angle > 120: if state==UP -> state=DOWN.
        // if angle < 60: if state==DOWN -> state=UP, count++.
        
        // Initial state is UP. 
        // Feed Lying Pose (>120) -> Sets state to DOWN.
        sitUpCounter.processKeypoints(lyingPose)
        assertEquals("Sit Up!", sitUpCounter.feedback) // State is now DOWN

        // Feed Sitting Pose (<60) -> Sets state to UP, increments count
        sitUpCounter.processKeypoints(sittingPose)
        assertEquals(1, sitUpCounter.repCount)
        assertEquals("Down we go", sitUpCounter.feedback)
    }

    @Test
    fun testSideSelection() {
        // High confidence right side, low confidence left side
        val keypoints = MutableList(17) { Keypoint(0f, 0f, 0.0f) }
        
        // Left side (indices 5,7,9) - Conf 0.1
        keypoints[5] = Keypoint(0f, 0f, 0.1f)
        keypoints[7] = Keypoint(0f, 0f, 0.1f)
        keypoints[9] = Keypoint(0f, 0f, 0.1f)
        
        // Right side (indices 6,8,10) - Conf 0.9
        // Set coordinates for UP state pushup on right side
        keypoints[6] = Keypoint(0f, 10f, 0.9f)
        keypoints[8] = Keypoint(0f, 0f, 0.9f)
        keypoints[10] = Keypoint(0f, -10f, 0.9f)

        pushUpCounter.processKeypoints(keypoints)
        
        // Should have processed right side, identified as UP state
        // Initial state is UP, so nothing changes rep-wise, but logic shouldn't reject it
        assertEquals("Go Lower", pushUpCounter.feedback)
    }

    private fun mockKeypoints(vararg points: Pair<Int, Keypoint>): List<Keypoint> {
        val list = MutableList(17) { Keypoint(0f, 0f, 0.0f) }
        for ((idx, kp) in points) {
            list[idx] = kp
        }
        return list
    }
}
