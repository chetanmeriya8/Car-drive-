package com.example.model

enum class RaceStatus {
    LOBBY,
    COUNTDOWN,
    RACING,
    FINISHED
}

data class RaceSettings(
    val trackId: String = TrackRepository.GRAND_PRIX.id,
    val totalLaps: Int = 3,
    val botCount: Int = 3,
    val maxPlayers: Int = 6
)

data class CarTelemetry(
    val playerId: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val angleDeg: Float = 0f,
    val angularVelocity: Float = 0f,
    val steerInput: Float = 0f,
    val throttleInput: Float = 0f,
    val isDrifting: Boolean = false,
    val isBoosting: Boolean = false,
    val nitroAmount: Float = 1f,
    val currentLap: Int = 1,
    val nextCheckpointIndex: Int = 1,
    val totalCheckpointsHit: Int = 0,
    val isFinished: Boolean = false,
    val finishTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class LeaderboardEntry(
    val rank: Int,
    val playerId: String,
    val playerName: String,
    val carType: CarType,
    val colorHex: Long,
    val currentLap: Int,
    val totalLaps: Int,
    val isFinished: Boolean,
    val finishTimeMs: Long,
    val bestLapMs: Long,
    val isLocalPlayer: Boolean = false
)

data class RaceResult(
    val rank: Int,
    val playerId: String,
    val playerName: String,
    val carType: CarType,
    val colorHex: Long,
    val totalTimeMs: Long,
    val bestLapMs: Long,
    val isLocalPlayer: Boolean
)
