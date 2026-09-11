package com.example.physics

import androidx.compose.ui.geometry.Offset
import com.example.model.CarType
import com.example.model.Track
import kotlin.math.*

class BotAi(
    val physics: CarPhysics,
    val name: String,
    val colorHex: Long,
    private val aggression: Float = 0.85f
) {
    private var currentWaypointIndex = 0

    fun step(dtSeconds: Float, track: Track, particleManager: ParticleManager, isRaceActive: Boolean) {
        if (!isRaceActive || physics.isFinished) {
            physics.throttleInput = 0f
            physics.steerInput = 0f
            physics.isDrifting = false
            physics.isBoosting = false
            physics.update(dtSeconds, track, 3, particleManager, false)
            return
        }

        val waypoints = track.waypoints
        val currentPos = Offset(physics.x, physics.y)

        // Find distance to current target waypoint
        val targetWp = waypoints[currentWaypointIndex]
        val toTarget = targetWp - currentPos
        val distToWp = hypot(toTarget.x, toTarget.y)

        // Advance to next waypoint if close enough
        if (distToWp < 140f) {
            currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
        }

        // Lookahead to smooth steering
        val lookaheadIdx = (currentWaypointIndex + 1) % waypoints.size
        val lookaheadWp = waypoints[lookaheadIdx]
        val blendedTarget = Offset(
            targetWp.x * 0.7f + lookaheadWp.x * 0.3f,
            targetWp.y * 0.7f + lookaheadWp.y * 0.3f
        )

        val targetDir = blendedTarget - currentPos
        val desiredAngleRad = atan2(targetDir.y, targetDir.x)
        var desiredAngleDeg = Math.toDegrees(desiredAngleRad.toDouble()).toFloat()
        if (desiredAngleDeg < 0) desiredAngleDeg += 360f

        // Compute angle difference
        var angleDiff = (desiredAngleDeg - physics.angleDeg) % 360f
        if (angleDiff > 180f) angleDiff -= 360f
        if (angleDiff < -180f) angleDiff += 360f

        // Steering input (-1..1)
        val steer = (angleDiff / 35f).coerceIn(-1f, 1f)
        physics.steerInput = steer

        // Cornering / braking logic
        val absAngleDiff = abs(angleDiff)
        if (absAngleDiff > 55f) {
            // Sharp turn: drift and brake slightly
            physics.throttleInput = 0.4f * aggression
            physics.isDrifting = true
        } else if (absAngleDiff > 30f) {
            physics.throttleInput = 0.75f * aggression
            physics.isDrifting = false
        } else {
            // Straight: full throttle
            physics.throttleInput = 1.0f * aggression
            physics.isDrifting = false

            // Use boost on straights if available
            if (physics.nitroGauge > 0.4f && absAngleDiff < 15f) {
                physics.isBoosting = true
            } else if (physics.nitroGauge < 0.1f) {
                physics.isBoosting = false
            }
        }

        physics.update(dtSeconds, track, 3, particleManager, isRaceActive)
    }
}
