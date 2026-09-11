package com.example.physics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class SkidMark(
    val start: Offset,
    val end: Offset,
    var alpha: Float = 0.6f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val initialSize: Float,
    var currentSize: Float,
    val maxLife: Float,
    var life: Float = 0f
)

class ParticleManager {
    val skidMarks = mutableListOf<SkidMark>()
    val particles = mutableListOf<Particle>()
    private val maxSkids = 200
    private val maxParticles = 300

    fun addSkidSegment(start: Offset, end: Offset) {
        if (skidMarks.size >= maxSkids) {
            skidMarks.removeAt(0)
        }
        skidMarks.add(SkidMark(start, end))
    }

    fun spawnDriftSmoke(position: Offset, carAngleRad: Float) {
        if (particles.size >= maxParticles) return
        val spread = 15f
        val px = position.x + (Random.nextFloat() - 0.5f) * spread
        val py = position.y + (Random.nextFloat() - 0.5f) * spread
        val smokeSpeed = Random.nextFloat() * 30f + 10f
        val smokeAngle = carAngleRad + Math.PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.8f

        particles.add(
            Particle(
                x = px,
                y = py,
                vx = kotlin.math.cos(smokeAngle) * smokeSpeed,
                vy = kotlin.math.sin(smokeAngle) * smokeSpeed,
                color = Color(0xBBCCCCCC),
                initialSize = Random.nextFloat() * 8f + 6f,
                currentSize = 6f,
                maxLife = 0.5f
            )
        )
    }

    fun spawnNitroFlames(position: Offset, carAngleRad: Float) {
        if (particles.size >= maxParticles) return
        val flameSpeed = Random.nextFloat() * 90f + 70f
        val backAngle = carAngleRad + Math.PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.3f
        val color = if (Random.nextBoolean()) Color(0xFFFF6D00) else Color(0xFF00E5FF)

        particles.add(
            Particle(
                x = position.x,
                y = position.y,
                vx = kotlin.math.cos(backAngle) * flameSpeed,
                vy = kotlin.math.sin(backAngle) * flameSpeed,
                color = color,
                initialSize = Random.nextFloat() * 9f + 6f,
                currentSize = 8f,
                maxLife = 0.25f
            )
        )
    }

    fun spawnSparks(position: Offset, normalX: Float, normalY: Float) {
        val count = 8
        for (i in 0 until count) {
            if (particles.size >= maxParticles) break
            val speed = Random.nextFloat() * 150f + 80f
            val spreadAngle = (Random.nextFloat() - 0.5f) * 1.5f
            val baseAngle = kotlin.math.atan2(normalY, normalX) + spreadAngle
            particles.add(
                Particle(
                    x = position.x,
                    y = position.y,
                    vx = kotlin.math.cos(baseAngle) * speed,
                    vy = kotlin.math.sin(baseAngle) * speed,
                    color = Color(0xFFFFEA00),
                    initialSize = Random.nextFloat() * 4f + 3f,
                    currentSize = 4f,
                    maxLife = 0.3f
                )
            )
        }
    }

    fun update(dtSeconds: Float) {
        // Fade skids
        val skidIt = skidMarks.iterator()
        while (skidIt.hasNext()) {
            val mark = skidIt.next()
            mark.alpha -= dtSeconds * 0.05f
            if (mark.alpha <= 0f) {
                skidIt.remove()
            }
        }

        // Update particles
        val partIt = particles.iterator()
        while (partIt.hasNext()) {
            val p = partIt.next()
            p.life += dtSeconds
            if (p.life >= p.maxLife) {
                partIt.remove()
            } else {
                p.x += p.vx * dtSeconds
                p.y += p.vy * dtSeconds
                val progress = p.life / p.maxLife
                p.currentSize = p.initialSize * (1f + progress * 1.2f)
            }
        }
    }
}
