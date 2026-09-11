package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.model.CarType
import com.example.model.PlayerCar
import com.example.model.Track
import com.example.network.RemoteCarInterpolator
import com.example.physics.CarPhysics
import com.example.physics.ParticleManager
import kotlin.math.*

@Composable
fun GameCanvas(
    track: Track,
    playerPhysics: CarPhysics,
    playerCar: PlayerCar,
    remoteCars: Map<String, RemoteCarInterpolator>,
    lobbyPlayers: List<PlayerCar>,
    particleManager: ParticleManager,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Camera tracking local car with velocity lookahead
        val lookaheadX = playerPhysics.vx * 0.25f
        val lookaheadY = playerPhysics.vy * 0.25f
        val camX = canvasWidth * 0.5f - (playerPhysics.x + lookaheadX)
        val camY = canvasHeight * 0.5f - (playerPhysics.y + lookaheadY)

        withTransform({
            translate(camX, camY)
        }) {
            // 1. Draw Off-road terrain background
            drawRect(
                color = Color(track.offRoadColorHex),
                topLeft = Offset(0f, 0f),
                size = Size(track.worldWidth, track.worldHeight)
            )

            // Grid lines on off-road terrain for speed reference
            val gridSize = 100f
            val gridColor = Color(0x15FFFFFF)
            var gx = 0f
            while (gx <= track.worldWidth) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, track.worldHeight), 1.5f)
                gx += gridSize
            }
            var gy = 0f
            while (gy <= track.worldHeight) {
                drawLine(gridColor, Offset(0f, gy), Offset(track.worldWidth, gy), 1.5f)
                gy += gridSize
            }

            // 2. Draw Track Curbs (Border strips)
            drawCurbs(track)

            // 3. Draw Asphalt Road Surface
            drawAsphaltRoad(track)

            // 4. Draw Centerline Dashes
            drawCenterlineDashes(track)

            // 5. Draw Start / Finish Line
            drawStartFinishLine(track)

            // 6. Draw Boost Pads
            drawBoostPads(track)

            // 7. Draw Tire Skid Marks
            for (skid in particleManager.skidMarks) {
                drawLine(
                    color = Color.Black.copy(alpha = skid.alpha.coerceIn(0f, 1f)),
                    start = skid.start,
                    end = skid.end,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            // 8. Draw Remote Multiplayer & Bot Cars
            for ((pId, remote) in remoteCars) {
                val playerInfo = lobbyPlayers.find { it.id == pId }
                val carType = playerInfo?.carType ?: CarType.PHANTOM_GT
                val colorHex = playerInfo?.colorHex ?: 0xFFFF1744

                drawCar(
                    x = remote.currentX,
                    y = remote.currentY,
                    angleDeg = remote.currentAngle,
                    carType = carType,
                    colorHex = colorHex,
                    isBraking = false,
                    isDrifting = remote.isDrifting,
                    isBoosting = remote.isBoosting,
                    nameTag = playerInfo?.name ?: "Racer"
                )
            }

            // 9. Draw Local Player's Car
            drawCar(
                x = playerPhysics.x,
                y = playerPhysics.y,
                angleDeg = playerPhysics.angleDeg,
                carType = playerPhysics.carType,
                colorHex = playerCar.colorHex,
                isBraking = playerPhysics.throttleInput < -0.1f,
                isDrifting = playerPhysics.isDrifting,
                isBoosting = playerPhysics.isBoosting,
                nameTag = "YOU"
            )

            // 10. Draw Particles (Smoke, Nitro Flames, Sparks)
            for (p in particleManager.particles) {
                val alpha = (1f - p.life / p.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.currentSize,
                    center = Offset(p.x, p.y)
                )
            }

            // 11. Draw Track Barriers / Guardrails
            drawBarriers(track)
        }
    }
}

private fun DrawScope.drawAsphaltRoad(track: Track) {
    val waypoints = track.waypoints
    val halfW = track.trackWidth * 0.5f

    // Draw connected thick quads for track road
    for (i in waypoints.indices) {
        val p1 = waypoints[i]
        val p2 = waypoints[(i + 1) % waypoints.size]
        drawLine(
            color = Color(track.asphaltColorHex),
            start = p1,
            end = p2,
            strokeWidth = track.trackWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCurbs(track: Track) {
    val waypoints = track.waypoints
    val curbWidth = track.trackWidth + 24f

    for (i in waypoints.indices) {
        val p1 = waypoints[i]
        val p2 = waypoints[(i + 1) % waypoints.size]
        val curbColor = if (i % 2 == 0) Color(track.curbColorHex1) else Color(track.curbColorHex2)
        drawLine(
            color = curbColor,
            start = p1,
            end = p2,
            strokeWidth = curbWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCenterlineDashes(track: Track) {
    val waypoints = track.waypoints
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 25f), 0f)

    val path = Path().apply {
        if (waypoints.isNotEmpty()) {
            moveTo(waypoints[0].x, waypoints[0].y)
            for (i in 1 until waypoints.size) {
                lineTo(waypoints[i].x, waypoints[i].y)
            }
            close()
        }
    }

    drawPath(
        path = path,
        color = Color(0x66FFFFFF),
        style = Stroke(width = 3f, pathEffect = dashEffect)
    )
}

private fun DrawScope.drawStartFinishLine(track: Track) {
    val startPos = track.startPosition
    val angleRad = Math.toRadians(track.startAngleDeg.toDouble()).toFloat()
    val normalX = -sin(angleRad)
    val normalY = cos(angleRad)
    val halfW = track.trackWidth * 0.5f

    val p1 = Offset(startPos.x - normalX * halfW, startPos.y - normalY * halfW)
    val p2 = Offset(startPos.x + normalX * halfW, startPos.y + normalY * halfW)

    // Checkerboard line
    val checkerEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
    drawLine(
        color = Color.White,
        start = p1,
        end = p2,
        strokeWidth = 14f,
        pathEffect = checkerEffect
    )
    val checkerEffect2 = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 12f)
    drawLine(
        color = Color.Black,
        start = p1,
        end = p2,
        strokeWidth = 14f,
        pathEffect = checkerEffect2
    )
}

private fun DrawScope.drawBoostPads(track: Track) {
    for (pad in track.boostPads) {
        // Glowing cyan/yellow ring
        drawCircle(
            color = Color(0x4400E5FF),
            radius = pad.radius,
            center = pad.position
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = pad.radius * 0.7f,
            center = pad.position,
            style = Stroke(width = 4f)
        )

        // Chevron arrow pointing in boost direction
        val dirRad = Math.toRadians(pad.directionAngle.toDouble()).toFloat()
        val fwd = Offset(cos(dirRad), sin(dirRad))
        val right = Offset(-sin(dirRad), cos(dirRad))

        val tip = pad.position + fwd * 20f
        val leftWing = pad.position - fwd * 12f - right * 14f
        val rightWing = pad.position - fwd * 12f + right * 14f

        val chevron = Path().apply {
            moveTo(leftWing.x, leftWing.y)
            lineTo(tip.x, tip.y)
            lineTo(rightWing.x, rightWing.y)
        }
        drawPath(
            path = chevron,
            color = Color(0xFFFFEA00),
            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawBarriers(track: Track) {
    // Outer and Inner neon barriers
    fun drawLoop(loop: List<Offset>, color: Color) {
        if (loop.size < 2) return
        for (i in loop.indices) {
            val p1 = loop[i]
            val p2 = loop[(i + 1) % loop.size]
            drawLine(color, p1, p2, 6f, StrokeCap.Round)
        }
    }
    drawLoop(track.outerWalls, Color(0x88FF1744))
    drawLoop(track.innerWalls, Color(0x8800E5FF))
}

private fun DrawScope.drawCar(
    x: Float,
    y: Float,
    angleDeg: Float,
    carType: CarType,
    colorHex: Long,
    isBraking: Boolean,
    isDrifting: Boolean,
    isBoosting: Boolean,
    nameTag: String
) {
    withTransform({
        translate(x, y)
        rotate(angleDeg)
    }) {
        val carLength = 34f
        val carWidth = 18f
        val halfL = carLength * 0.5f
        val halfW = carWidth * 0.5f

        // 1. Wheels (4 black rounded rectangles)
        val wheelW = 5f
        val wheelL = 10f
        val wheelColor = Color(0xFF111111)

        // Front-left
        drawRoundRect(wheelColor, Offset(-halfL + 4f, -halfW - 2f), Size(wheelL, wheelW), CornerRadius(2f))
        // Front-right
        drawRoundRect(wheelColor, Offset(-halfL + 4f, halfW - 3f), Size(wheelL, wheelW), CornerRadius(2f))
        // Rear-left
        drawRoundRect(wheelColor, Offset(halfL - 13f, -halfW - 2f), Size(wheelL, wheelW), CornerRadius(2f))
        // Rear-right
        drawRoundRect(wheelColor, Offset(halfL - 13f, halfW - 3f), Size(wheelL, wheelW), CornerRadius(2f))

        // 2. Car Chassis Body
        val carPaintColor = Color(colorHex)
        val bodyPath = Path().apply {
            // Front bumper tip
            moveTo(halfL, 0f)
            // Front-left curvature
            lineTo(halfL - 4f, -halfW + 2f)
            lineTo(halfL - 8f, -halfW)
            // Cabin side
            lineTo(0f, -halfW)
            // Rear side
            lineTo(-halfL + 4f, -halfW)
            // Rear-left bumper
            lineTo(-halfL, -halfW + 3f)
            // Rear bumper
            lineTo(-halfL, halfW - 3f)
            // Rear-right bumper
            lineTo(-halfL + 4f, halfW)
            // Right cabin side
            lineTo(0f, halfW)
            lineTo(halfL - 8f, halfW)
            lineTo(halfL - 4f, halfW - 2f)
            close()
        }
        drawPath(bodyPath, carPaintColor)

        // Highlighting edge stroke
        drawPath(bodyPath, Color.White.copy(alpha = 0.35f), style = Stroke(1.5f))

        // 3. Cabin & Tinted Windows
        val glassColor = Color(0xFF0D1B2A)
        val glassReflection = Color(0xAA00E5FF)
        val cabinPath = Path().apply {
            moveTo(halfL - 10f, 0f)
            lineTo(halfL - 14f, -halfW + 3f)
            lineTo(-halfL + 12f, -halfW + 3f)
            lineTo(-halfL + 8f, 0f)
            lineTo(-halfL + 12f, halfW - 3f)
            lineTo(halfL - 14f, halfW - 3f)
            close()
        }
        drawPath(cabinPath, glassColor)
        // Windshield reflection streak
        drawLine(glassReflection, Offset(halfL - 13f, -halfW + 4f), Offset(halfL - 13f, halfW - 4f), 2f)

        // 4. Racing Aero Spoiler
        val spoilerColor = carPaintColor.copy(alpha = 0.85f)
        drawRect(spoilerColor, Offset(-halfL - 2f, -halfW - 1f), Size(4f, carWidth + 2f))

        // 5. Headlights (LED beams projecting forward)
        val headlightColor = Color(0xFFFFEA00)
        drawCircle(headlightColor, 2.5f, Offset(halfL - 1f, -halfW + 4f))
        drawCircle(headlightColor, 2.5f, Offset(halfL - 1f, halfW - 4f))

        // 6. Tail lights / Brake lights
        val tailColor = if (isBraking) Color(0xFFFF1744) else Color(0x88FF1744)
        val tailSize = if (isBraking) 3.5f else 2f
        drawCircle(tailColor, tailSize, Offset(-halfL, -halfW + 4f))
        drawCircle(tailColor, tailSize, Offset(-halfL, halfW - 4f))

        // 7. Nitro Boost Glow
        if (isBoosting) {
            drawCircle(Color(0xFF00E5FF), 8f, Offset(-halfL - 4f, -halfW + 5f))
            drawCircle(Color(0xFF00E5FF), 8f, Offset(-halfL - 4f, halfW - 5f))
        }
    }

    // Name tag drawn un-rotated above the car
    withTransform({
        translate(x, y - 28f)
    }) {
        val tagBg = if (nameTag == "YOU") Color(0xDDFF6D00) else Color(0xBB161B22)
        drawRoundRect(
            color = tagBg,
            topLeft = Offset(-24f, -9f),
            size = Size(48f, 18f),
            cornerRadius = CornerRadius(4f)
        )
    }
}
