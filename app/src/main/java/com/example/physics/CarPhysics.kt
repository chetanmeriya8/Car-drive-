package com.example.physics

import androidx.compose.ui.geometry.Offset
import com.example.model.CarTelemetry
import com.example.model.CarType
import com.example.model.Track
import kotlin.math.*

class CarPhysics(
    val playerId: String,
    val carType: CarType,
    initialPosition: Offset,
    initialAngleDeg: Float
) {
    var x = initialPosition.x
    var y = initialPosition.y
    var vx = 0f
    var vy = 0f
    var angleDeg = initialAngleDeg
    var angularVelocity = 0f

    var steerInput = 0f
    var throttleInput = 0f
    var isDrifting = false
    var isBoosting = false

    var nitroGauge = 1f
    var isOffRoad = false

    var currentLap = 1
    var nextCheckpointIndex = 1
    var totalCheckpointsHit = 0
    var lapStartTimeMs = 0L
    var currentLapTimeMs = 0L
    var bestLapTimeMs = 0L
    var isFinished = false
    var finishTimeMs = 0L

    var lastLeftTirePos: Offset? = null
    var lastRightTirePos: Offset? = null

    // Callback triggers for audio/haptics
    var onWallCollision: ((Float) -> Unit)? = null
    var onBoostPadHit: (() -> Unit)? = null
    var onLapCompleted: ((Int, Long) -> Unit)? = null

    fun resetTo(pos: Offset, angle: Float) {
        x = pos.x
        y = pos.y
        vx = 0f
        vy = 0f
        angleDeg = angle
        angularVelocity = 0f
        nitroGauge = 1f
        isOffRoad = false
        currentLap = 1
        nextCheckpointIndex = 1
        totalCheckpointsHit = 0
        lapStartTimeMs = System.currentTimeMillis()
        currentLapTimeMs = 0L
        bestLapTimeMs = 0L
        isFinished = false
        finishTimeMs = 0L
    }

    fun update(
        dtSeconds: Float,
        track: Track,
        totalRaceLaps: Int,
        particleManager: ParticleManager,
        isRaceActive: Boolean
    ) {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)

        if (!isFinished && isRaceActive && lapStartTimeMs > 0) {
            currentLapTimeMs = System.currentTimeMillis() - lapStartTimeMs
        }

        val headingRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val forwardX = cos(headingRad)
        val forwardY = sin(headingRad)
        val rightX = -sin(headingRad)
        val rightY = cos(headingRad)

        // Decompose velocity into forward & lateral components
        val forwardSpeed = vx * forwardX + vy * forwardY
        val lateralSpeed = vx * rightX + vy * rightY

        // Check off-road status (distance from track centerline)
        val distToTrack = calculateDistanceToCenterline(Offset(x, y), track.waypoints)
        val halfRoad = track.trackWidth * 0.5f
        isOffRoad = distToTrack > halfRoad

        // Nitro replenishment and consumption
        if (isBoosting && nitroGauge > 0f && isRaceActive) {
            nitroGauge = (nitroGauge - dt * 0.35f).coerceAtLeast(0f)
            if (nitroGauge <= 0f) isBoosting = false
        } else {
            // Replenish nitro slowly, faster if drifting
            val regenRate = if (isDrifting) 0.12f else 0.04f
            nitroGauge = (nitroGauge + dt * regenRate).coerceAtMost(1f)
        }

        // Compute acceleration limits
        var maxSpd = carType.topSpeed
        var accel = carType.acceleration

        if (isBoosting) {
            maxSpd *= carType.boostPower
            accel *= 1.4f
        }

        if (isOffRoad) {
            maxSpd *= 0.55f
            accel *= 0.6f
        }

        // Apply engine throttle or braking/reversing
        if (isRaceActive && !isFinished) {
            if (throttleInput > 0.05f) {
                if (forwardSpeed < maxSpd) {
                    vx += forwardX * (accel * throttleInput * dt)
                    vy += forwardY * (accel * throttleInput * dt)
                }
            } else if (throttleInput < -0.05f) {
                // Reverse or brake
                if (forwardSpeed > 10f) {
                    // Braking hard
                    val brakePower = 480f * dt
                    vx -= forwardX * brakePower * (-throttleInput)
                    vy -= forwardY * brakePower * (-throttleInput)
                } else if (forwardSpeed > -maxSpd * 0.35f) {
                    // Reversing
                    vx += forwardX * (accel * 0.5f * throttleInput * dt)
                    vy += forwardY * (accel * 0.5f * throttleInput * dt)
                }
            }
        }

        // Natural rolling resistance and aerodynamic drag
        val speedSq = vx * vx + vy * vy
        val drag = if (isOffRoad) 2.2f else 0.85f
        vx -= vx * drag * dt
        vy -= vy * drag * dt

        // Lateral tire friction (killing sideways sliding)
        // During drift, lateral grip drops substantially allowing sustained slide
        val lateralFriction = if (isDrifting) {
            carType.driftFactor * 0.55f
        } else {
            0.92f // High grip
        }

        val newLateralSpeed = lateralSpeed * (1f - (lateralFriction * dt * 10f).coerceIn(0f, 1f))
        val currentForwardSpeed = vx * forwardX + vy * forwardY

        vx = forwardX * currentForwardSpeed + rightX * newLateralSpeed
        vy = forwardY * currentForwardSpeed + rightY * newLateralSpeed

        // Steering rotation based on speed and input
        val absSpeed = sqrt(speedSq)
        val speedTurnFactor = (absSpeed / 180f).coerceIn(0f, 1f)
        var turnRate = carType.turnRate * steerInput * speedTurnFactor

        if (isDrifting) {
            turnRate *= 1.25f // agile pivot when drifting
        }

        if (forwardSpeed < -5f) {
            turnRate = -turnRate // invert steering when reversing
        }

        angularVelocity = turnRate
        angleDeg = (angleDeg + angularVelocity * dt) % 360f
        if (angleDeg < 0f) angleDeg += 360f

        // Position integration
        x += vx * dt
        y += vy * dt

        // Tire locations for particles and skid marks
        val carLength = 26f
        val carWidth = 14f
        val rearLeft = Offset(
            x - forwardX * (carLength * 0.4f) - rightX * (carWidth * 0.5f),
            y - forwardY * (carLength * 0.4f) - rightY * (carWidth * 0.5f)
        )
        val rearRight = Offset(
            x - forwardX * (carLength * 0.4f) + rightX * (carWidth * 0.5f),
            y - forwardY * (carLength * 0.4f) + rightY * (carWidth * 0.5f)
        )

        // Particle generation: Drifting skids & smoke
        if (isDrifting && absSpeed > 60f) {
            lastLeftTirePos?.let { prev -> particleManager.addSkidSegment(prev, rearLeft) }
            lastRightTirePos?.let { prev -> particleManager.addSkidSegment(prev, rearRight) }
            particleManager.spawnDriftSmoke(rearLeft, headingRad)
            particleManager.spawnDriftSmoke(rearRight, headingRad)
        }

        // Nitro exhaust flames
        if (isBoosting) {
            particleManager.spawnNitroFlames(rearLeft, headingRad)
            particleManager.spawnNitroFlames(rearRight, headingRad)
        }

        lastLeftTirePos = rearLeft
        lastRightTirePos = rearRight

        // Boost Pad collisions
        for (pad in track.boostPads) {
            val dx = x - pad.position.x
            val dy = y - pad.position.y
            if (dx * dx + dy * dy < pad.radius * pad.radius) {
                // Rocket the car forward!
                val boostAngleRad = Math.toRadians(pad.directionAngle.toDouble()).toFloat()
                val boostMagnitude = 320f
                vx += cos(boostAngleRad) * boostMagnitude
                vy += sin(boostAngleRad) * boostMagnitude
                nitroGauge = (nitroGauge + 0.35f).coerceAtMost(1f)
                onBoostPadHit?.invoke()
            }
        }

        // Wall boundary collision
        val barrierDist = halfRoad + 45f
        if (distToTrack > barrierDist) {
            // Push back towards nearest waypoint and bounce
            val nearestWp = findNearestWaypoint(Offset(x, y), track.waypoints)
            val toCenter = nearestWp - Offset(x, y)
            val len = hypot(toCenter.x, toCenter.y).coerceAtLeast(1f)
            val normalX = toCenter.x / len
            val normalY = toCenter.y / len

            // Push out of wall
            val penetration = distToTrack - barrierDist
            x += normalX * penetration * 0.8f
            y += normalY * penetration * 0.8f

            // Elastic bounce
            val dot = vx * normalX + vy * normalY
            if (dot < 0) {
                vx -= 1.4f * dot * normalX
                vy -= 1.4f * dot * normalY
                val impactForce = abs(dot)
                if (impactForce > 40f) {
                    particleManager.spawnSparks(Offset(x, y), normalX, normalY)
                    onWallCollision?.invoke(impactForce)
                }
            }
        }

        // Checkpoint checking
        if (isRaceActive && !isFinished && track.checkpoints.isNotEmpty()) {
            val targetCp = track.checkpoints[nextCheckpointIndex]
            val cpx = x - targetCp.center.x
            val cpy = y - targetCp.center.y
            if (cpx * cpx + cpy * cpy < targetCp.width * targetCp.width) {
                // Checkpoint passed!
                totalCheckpointsHit++
                val totalCpCount = track.checkpoints.size
                nextCheckpointIndex = (nextCheckpointIndex + 1) % totalCpCount

                // Check for full lap completion (passed checkpoint 0 after hitting other checkpoints)
                if (nextCheckpointIndex == 1 && totalCheckpointsHit >= totalCpCount - 1) {
                    val lapDuration = System.currentTimeMillis() - lapStartTimeMs
                    if (bestLapTimeMs == 0L || lapDuration < bestLapTimeMs) {
                        bestLapTimeMs = lapDuration
                    }
                    onLapCompleted?.invoke(currentLap, lapDuration)

                    if (currentLap >= totalRaceLaps) {
                        isFinished = true
                        finishTimeMs = currentLapTimeMs
                    } else {
                        currentLap++
                        lapStartTimeMs = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    fun applyImpulse(impulseX: Float, impulseY: Float) {
        vx += impulseX
        vy += impulseY
    }

    val telemetry: CarTelemetry
        get() = CarTelemetry(
            playerId = playerId,
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            angleDeg = angleDeg,
            angularVelocity = angularVelocity,
            steerInput = steerInput,
            throttleInput = throttleInput,
            isDrifting = isDrifting,
            isBoosting = isBoosting,
            nitroAmount = nitroGauge,
            currentLap = currentLap,
            nextCheckpointIndex = nextCheckpointIndex,
            totalCheckpointsHit = totalCheckpointsHit,
            isFinished = isFinished,
            finishTimeMs = finishTimeMs,
            timestamp = System.currentTimeMillis()
        )

    private fun calculateDistanceToCenterline(pos: Offset, waypoints: List<Offset>): Float {
        var minDist = Float.MAX_VALUE
        for (i in waypoints.indices) {
            val p1 = waypoints[i]
            val p2 = waypoints[(i + 1) % waypoints.size]
            val d = distToSegment(pos, p1, p2)
            if (d < minDist) minDist = d
        }
        return minDist
    }

    private fun findNearestWaypoint(pos: Offset, waypoints: List<Offset>): Offset {
        var nearest = waypoints[0]
        var minDistSq = Float.MAX_VALUE
        for (wp in waypoints) {
            val dx = pos.x - wp.x
            val dy = pos.y - wp.y
            val dSq = dx * dx + dy * dy
            if (dSq < minDistSq) {
                minDistSq = dSq
                nearest = wp
            }
        }
        return nearest
    }

    private fun distToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return hypot(p.x - v.x, p.y - v.y)
        val t = (((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2).coerceIn(0f, 1f)
        val proj = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return hypot(p.x - proj.x, p.y - proj.y)
    }
}
