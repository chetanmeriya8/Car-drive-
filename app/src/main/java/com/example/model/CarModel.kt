package com.example.model

import androidx.compose.ui.graphics.Color

enum class CarType(
    val displayName: String,
    val description: String,
    val topSpeed: Float,
    val acceleration: Float,
    val turnRate: Float,
    val driftFactor: Float,
    val boostPower: Float
) {
    PHANTOM_GT(
        displayName = "Apex GT",
        description = "Balanced handling and agility with precise apex control",
        topSpeed = 480f,
        acceleration = 310f,
        turnRate = 180f,
        driftFactor = 0.88f,
        boostPower = 1.45f
    ),
    TURBO_VIPER(
        displayName = "Turbo Viper",
        description = "Maximum straight-line velocity and explosive nitro thrusters",
        topSpeed = 540f,
        acceleration = 290f,
        turnRate = 160f,
        driftFactor = 0.85f,
        boostPower = 1.65f
    ),
    DRIFT_KING(
        displayName = "Drift King",
        description = "Smooth lateral slip, fast slide recovery, and rapid boost regen",
        topSpeed = 470f,
        acceleration = 330f,
        turnRate = 210f,
        driftFactor = 0.94f,
        boostPower = 1.40f
    ),
    CYBER_BEAST(
        displayName = "Cyber Beast",
        description = "Heavyweight chassis that dominates collisions and maintains momentum",
        topSpeed = 500f,
        acceleration = 320f,
        turnRate = 170f,
        driftFactor = 0.82f,
        boostPower = 1.50f
    )
}

enum class CarColor(val displayName: String, val hex: Long, val composeColor: Color) {
    CRIMSON_RED("Crimson Red", 0xFFFF1744, Color(0xFFFF1744)),
    ELECTRIC_CYAN("Electric Cyan", 0xFF00E5FF, Color(0xFF00E5FF)),
    NEON_LIME("Neon Lime", 0xFF00E676, Color(0xFF00E676)),
    TURBO_ORANGE("Turbo Orange", 0xFFFF6D00, Color(0xFFFF6D00)),
    CYBER_PURPLE("Cyber Purple", 0xFFD500F9, Color(0xFFD500F9)),
    GOLD_YELLOW("Racing Gold", 0xFFFFD600, Color(0xFFFFD600))
}

data class PlayerCar(
    val id: String,
    val name: String,
    val carType: CarType = CarType.PHANTOM_GT,
    val colorHex: Long = CarColor.CRIMSON_RED.hex,
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val isBot: Boolean = false,
    val pingMs: Int = 0
)
