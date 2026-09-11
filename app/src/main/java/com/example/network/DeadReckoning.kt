package com.example.network

import androidx.compose.ui.geometry.Offset
import com.example.model.CarTelemetry
import kotlin.math.*

class RemoteCarInterpolator(
    val playerId: String,
    initialX: Float,
    initialY: Float,
    initialAngle: Float
) {
    var currentX = initialX
    var currentY = initialY
    var currentAngle = initialAngle
    var currentVx = 0f
    var currentVy = 0f
    var isDrifting = false
    var isBoosting = false
    var lastPacketTime = System.currentTimeMillis()

    fun updateFromTelemetry(telemetry: CarTelemetry) {
        val now = System.currentTimeMillis()
        val dt = ((now - telemetry.timestamp) / 1000f).coerceIn(0f, 0.2f)

        // Dead reckoning prediction
        val targetX = telemetry.x + telemetry.vx * dt
        val targetY = telemetry.y + telemetry.vy * dt

        // If delta is huge (>300px), snap immediately (teleport)
        val distSq = (targetX - currentX) * (targetX - currentX) + (targetY - currentY) * (targetY - currentY)
        if (distSq > 90000f) {
            currentX = targetX
            currentY = targetY
            currentAngle = telemetry.angleDeg
        }

        currentVx = telemetry.vx
        currentVy = telemetry.vy
        isDrifting = telemetry.isDrifting
        isBoosting = telemetry.isBoosting
        lastPacketTime = now
    }

    fun step(dtSeconds: Float, latestTelemetry: CarTelemetry?) {
        if (latestTelemetry != null) {
            // Lerp position
            val targetX = latestTelemetry.x + latestTelemetry.vx * 0.03f
            val targetY = latestTelemetry.y + latestTelemetry.vy * 0.03f

            val blend = (15f * dtSeconds).coerceIn(0f, 1f)
            currentX += (targetX - currentX) * blend
            currentY += (targetY - currentY) * blend

            // Smooth angle interpolation handling 360 wrap
            var diff = (latestTelemetry.angleDeg - currentAngle) % 360f
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            currentAngle += diff * (12f * dtSeconds).coerceIn(0f, 1f)

            currentVx = latestTelemetry.vx
            currentVy = latestTelemetry.vy
            isDrifting = latestTelemetry.isDrifting
            isBoosting = latestTelemetry.isBoosting
        } else {
            // Extrapolate if packet is delayed
            currentX += currentVx * dtSeconds
            currentY += currentVy * dtSeconds
        }
    }

    val position: Offset get() = Offset(currentX, currentY)
}
