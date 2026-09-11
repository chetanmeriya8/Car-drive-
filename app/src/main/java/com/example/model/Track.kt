package com.example.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

data class Checkpoint(
    val index: Int,
    val center: Offset,
    val width: Float = 220f,
    val angleRad: Float = 0f
)

data class BoostPad(
    val position: Offset,
    val radius: Float = 36f,
    val directionAngle: Float
)

data class WallSegment(
    val start: Offset,
    val end: Offset
)

data class Track(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String,
    val worldWidth: Float,
    val worldHeight: Float,
    val trackWidth: Float = 160f,
    val startPosition: Offset,
    val startAngleDeg: Float,
    val gridSlots: List<Offset>, // Starting grid positions for multiple cars
    val waypoints: List<Offset>,
    val checkpoints: List<Checkpoint>,
    val boostPads: List<BoostPad> = emptyList(),
    val innerWalls: List<Offset>,
    val outerWalls: List<Offset>,
    val asphaltColorHex: Long = 0xFF1C222B,
    val offRoadColorHex: Long = 0xFF0B3318,
    val curbColorHex1: Long = 0xFFFF1744,
    val curbColorHex2: Long = 0xFFFFFFFF
)

object TrackRepository {
    val GRAND_PRIX = createGrandPrixTrack()
    val NEON_CYBER = createNeonCyberTrack()
    val DESERT_CANYON = createDesertCanyonTrack()

    val ALL_TRACKS = listOf(GRAND_PRIX, NEON_CYBER, DESERT_CANYON)

    fun getById(id: String): Track = ALL_TRACKS.find { it.id == id } ?: GRAND_PRIX

    private fun createGrandPrixTrack(): Track {
        val w = 2400f
        val h = 1800f
        val waypoints = listOf(
            Offset(400f, 400f),
            Offset(1200f, 380f),
            Offset(1900f, 420f),
            Offset(2050f, 750f),
            Offset(1750f, 950f),
            Offset(1400f, 850f),
            Offset(1200f, 1150f),
            Offset(1700f, 1350f),
            Offset(2000f, 1550f),
            Offset(1450f, 1600f),
            Offset(750f, 1550f),
            Offset(400f, 1200f),
            Offset(550f, 850f),
            Offset(400f, 600f)
        )

        val checkpoints = waypoints.mapIndexed { index, pt ->
            val next = waypoints[(index + 1) % waypoints.size]
            val angle = atan2(next.y - pt.y, next.x - pt.x)
            Checkpoint(index = index, center = pt, angleRad = angle)
        }

        val startPos = Offset(500f, 400f)
        val grid = listOf(
            Offset(550f, 380f),
            Offset(520f, 440f),
            Offset(440f, 380f),
            Offset(410f, 440f),
            Offset(330f, 380f),
            Offset(300f, 440f)
        )

        // Generate inner and outer wall loops from waypoints
        val halfW = 85f
        val inner = mutableListOf<Offset>()
        val outer = mutableListOf<Offset>()

        for (i in waypoints.indices) {
            val curr = waypoints[i]
            val next = waypoints[(i + 1) % waypoints.size]
            val prev = waypoints[(i - 1 + waypoints.size) % waypoints.size]

            val dir1 = (curr - prev)
            val dir2 = (next - curr)
            val avgDir = Offset(dir1.x + dir2.x, dir1.y + dir2.y)
            val len = hypot(avgDir.x, avgDir.y).coerceAtLeast(1f)
            val normal = Offset(-avgDir.y / len, avgDir.x / len)

            outer.add(curr + normal * halfW)
            inner.add(curr - normal * halfW)
        }

        val boostPads = listOf(
            BoostPad(Offset(1000f, 385f), 35f, 0f),
            BoostPad(Offset(1150f, 1580f), 35f, 180f)
        )

        return Track(
            id = "grand_prix",
            name = "Apex International Circuit",
            description = "High-speed sweeping corners, technical chicane, and long overtaking straights.",
            difficulty = "Medium",
            worldWidth = w,
            worldHeight = h,
            trackWidth = halfW * 2,
            startPosition = startPos,
            startAngleDeg = 0f,
            gridSlots = grid,
            waypoints = waypoints,
            checkpoints = checkpoints,
            boostPads = boostPads,
            innerWalls = inner,
            outerWalls = outer,
            asphaltColorHex = 0xFF1A1F26,
            offRoadColorHex = 0xFF14301C,
            curbColorHex1 = 0xFFFF1744,
            curbColorHex2 = 0xFFFFFFFF
        )
    }

    private fun createNeonCyberTrack(): Track {
        val w = 2200f
        val h = 1800f
        val waypoints = listOf(
            Offset(400f, 350f),
            Offset(1000f, 350f),
            Offset(1750f, 350f),
            Offset(1850f, 750f),
            Offset(1350f, 750f),
            Offset(1350f, 1150f),
            Offset(1850f, 1150f),
            Offset(1850f, 1550f),
            Offset(1100f, 1550f),
            Offset(500f, 1550f),
            Offset(380f, 1100f),
            Offset(850f, 1100f),
            Offset(850f, 750f),
            Offset(380f, 750f)
        )

        val checkpoints = waypoints.mapIndexed { index, pt ->
            val next = waypoints[(index + 1) % waypoints.size]
            val angle = atan2(next.y - pt.y, next.x - pt.x)
            Checkpoint(index = index, center = pt, angleRad = angle)
        }

        val halfW = 90f
        val inner = mutableListOf<Offset>()
        val outer = mutableListOf<Offset>()
        for (i in waypoints.indices) {
            val curr = waypoints[i]
            val next = waypoints[(i + 1) % waypoints.size]
            val prev = waypoints[(i - 1 + waypoints.size) % waypoints.size]
            val dir1 = (curr - prev)
            val dir2 = (next - curr)
            val avgDir = Offset(dir1.x + dir2.x, dir1.y + dir2.y)
            val len = hypot(avgDir.x, avgDir.y).coerceAtLeast(1f)
            val normal = Offset(-avgDir.y / len, avgDir.x / len)
            outer.add(curr + normal * halfW)
            inner.add(curr - normal * halfW)
        }

        val boostPads = listOf(
            BoostPad(Offset(800f, 350f), 38f, 0f),
            BoostPad(Offset(1400f, 1550f), 38f, 180f),
            BoostPad(Offset(850f, 925f), 35f, 270f)
        )

        val grid = listOf(
            Offset(550f, 330f),
            Offset(520f, 380f),
            Offset(440f, 330f),
            Offset(410f, 380f),
            Offset(340f, 330f),
            Offset(310f, 380f)
        )

        return Track(
            id = "neon_cyber",
            name = "Neon Metropolis Raceway",
            description = "Grid-mapped neon speedway with intense right-angle drift corners and nitro recharge pads.",
            difficulty = "Hard",
            worldWidth = w,
            worldHeight = h,
            trackWidth = halfW * 2,
            startPosition = Offset(500f, 350f),
            startAngleDeg = 0f,
            gridSlots = grid,
            waypoints = waypoints,
            checkpoints = checkpoints,
            boostPads = boostPads,
            innerWalls = inner,
            outerWalls = outer,
            asphaltColorHex = 0xFF0D1524,
            offRoadColorHex = 0xFF080D18,
            curbColorHex1 = 0xFF00E5FF,
            curbColorHex2 = 0xFFD500F9
        )
    }

    private fun createDesertCanyonTrack(): Track {
        val w = 2400f
        val h = 1800f
        val waypoints = listOf(
            Offset(450f, 450f),
            Offset(1050f, 400f),
            Offset(1650f, 500f),
            Offset(1950f, 850f),
            Offset(1700f, 1150f),
            Offset(1250f, 1050f),
            Offset(900f, 1250f),
            Offset(1300f, 1450f),
            Offset(1850f, 1550f),
            Offset(1150f, 1650f),
            Offset(550f, 1550f),
            Offset(350f, 1150f),
            Offset(600f, 800f)
        )

        val checkpoints = waypoints.mapIndexed { index, pt ->
            val next = waypoints[(index + 1) % waypoints.size]
            val angle = atan2(next.y - pt.y, next.x - pt.x)
            Checkpoint(index = index, center = pt, angleRad = angle)
        }

        val halfW = 95f
        val inner = mutableListOf<Offset>()
        val outer = mutableListOf<Offset>()
        for (i in waypoints.indices) {
            val curr = waypoints[i]
            val next = waypoints[(i + 1) % waypoints.size]
            val prev = waypoints[(i - 1 + waypoints.size) % waypoints.size]
            val dir1 = (curr - prev)
            val dir2 = (next - curr)
            val avgDir = Offset(dir1.x + dir2.x, dir1.y + dir2.y)
            val len = hypot(avgDir.x, avgDir.y).coerceAtLeast(1f)
            val normal = Offset(-avgDir.y / len, avgDir.x / len)
            outer.add(curr + normal * halfW)
            inner.add(curr - normal * halfW)
        }

        val boostPads = listOf(
            BoostPad(Offset(800f, 410f), 35f, 0f),
            BoostPad(Offset(850f, 1600f), 35f, 180f)
        )

        val grid = listOf(
            Offset(550f, 430f),
            Offset(520f, 480f),
            Offset(440f, 430f),
            Offset(410f, 480f),
            Offset(340f, 430f),
            Offset(310f, 480f)
        )

        return Track(
            id = "desert_canyon",
            name = "Desert Canyon Drift",
            description = "Wide sand-swept canyon road designed for high-angle power slides and drifting duels.",
            difficulty = "Easy",
            worldWidth = w,
            worldHeight = h,
            trackWidth = halfW * 2,
            startPosition = Offset(500f, 450f),
            startAngleDeg = 0f,
            gridSlots = grid,
            waypoints = waypoints,
            checkpoints = checkpoints,
            boostPads = boostPads,
            innerWalls = inner,
            outerWalls = outer,
            asphaltColorHex = 0xFF2A231D,
            offRoadColorHex = 0xFF422E1B,
            curbColorHex1 = 0xFFFF6D00,
            curbColorHex2 = 0xFFFFD600
        )
    }
}
