package com.example.network

import com.example.model.CarTelemetry
import com.example.model.CarType
import com.example.model.PlayerCar

object NetworkConfig {
    const val DISCOVERY_PORT = 8888
    const val GAME_PORT = 8889
    const val TELEMETRY_RATE_HZ = 40 // 40 packets/sec = 25ms interval for low latency
    const val HEARTBEAT_INTERVAL_MS = 1000L
    const val CLIENT_TIMEOUT_MS = 5000L
}

data class DiscoveredRoom(
    val roomId: String,
    val roomName: String,
    val hostName: String,
    val hostIp: String,
    val port: Int,
    val trackId: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val lastSeenMs: Long = System.currentTimeMillis()
)

sealed class Packet {
    abstract fun serialize(): String

    data class DiscoveryBeacon(
        val roomId: String,
        val roomName: String,
        val hostName: String,
        val hostIp: String,
        val port: Int,
        val trackId: String,
        val playerCount: Int,
        val maxPlayers: Int
    ) : Packet() {
        override fun serialize() = "BEACON|$roomId|$roomName|$hostName|$hostIp|$port|$trackId|$playerCount|$maxPlayers"
    }

    data class JoinRequest(
        val playerId: String,
        val playerName: String,
        val carType: CarType,
        val colorHex: Long
    ) : Packet() {
        override fun serialize() = "JOIN|$playerId|$playerName|${carType.name}|$colorHex"
    }

    data class JoinAccepted(
        val assignedId: String,
        val trackId: String,
        val totalLaps: Int,
        val playersString: String
    ) : Packet() {
        override fun serialize() = "ACCEPT|$assignedId|$trackId|$totalLaps|$playersString"
    }

    data class PlayerListUpdate(
        val playersString: String
    ) : Packet() {
        override fun serialize() = "PLAYERS|$playersString"
    }

    data class CountdownStart(
        val startTimeMillis: Long,
        val trackId: String,
        val totalLaps: Int
    ) : Packet() {
        override fun serialize() = "COUNTDOWN|$startTimeMillis|$trackId|$totalLaps"
    }

    data class Telemetry(
        val telemetry: CarTelemetry,
        val sequence: Long = 0L
    ) : Packet() {
        override fun serialize(): String {
            val t = telemetry
            return "TEL|${t.playerId}|$sequence|${"%.1f".format(t.x)}|${"%.1f".format(t.y)}|" +
                    "${"%.1f".format(t.vx)}|${"%.1f".format(t.vy)}|${"%.1f".format(t.angleDeg)}|" +
                    "${"%.1f".format(t.angularVelocity)}|${"%.2f".format(t.steerInput)}|${"%.2f".format(t.throttleInput)}|" +
                    "${if (t.isDrifting) 1 else 0}|${if (t.isBoosting) 1 else 0}|${"%.2f".format(t.nitroAmount)}|" +
                    "${t.currentLap}|${t.nextCheckpointIndex}|${t.totalCheckpointsHit}|" +
                    "${if (t.isFinished) 1 else 0}|${t.finishTimeMs}|${t.timestamp}"
        }
    }

    data class CollisionEvent(
        val id1: String,
        val id2: String,
        val nx: Float,
        val ny: Float,
        val impulse: Float
    ) : Packet() {
        override fun serialize() = "BUMP|$id1|$id2|${"%.2f".format(nx)}|${"%.2f".format(ny)}|${"%.2f".format(impulse)}"
    }

    data class Ping(val sendTime: Long) : Packet() {
        override fun serialize() = "PING|$sendTime"
    }

    data class Pong(val originalSendTime: Long) : Packet() {
        override fun serialize() = "PONG|$originalSendTime"
    }

    data class Leave(val playerId: String) : Packet() {
        override fun serialize() = "LEAVE|$playerId"
    }

    companion object {
        fun parse(data: String): Packet? {
            val parts = data.split("|")
            if (parts.isEmpty()) return null

            return try {
                when (parts[0]) {
                    "BEACON" -> DiscoveryBeacon(
                        roomId = parts[1],
                        roomName = parts[2],
                        hostName = parts[3],
                        hostIp = parts[4],
                        port = parts[5].toInt(),
                        trackId = parts[6],
                        playerCount = parts[7].toInt(),
                        maxPlayers = parts[8].toInt()
                    )
                    "JOIN" -> JoinRequest(
                        playerId = parts[1],
                        playerName = parts[2],
                        carType = CarType.valueOf(parts[3]),
                        colorHex = parts[4].toLong()
                    )
                    "ACCEPT" -> JoinAccepted(
                        assignedId = parts[1],
                        trackId = parts[2],
                        totalLaps = parts[3].toInt(),
                        playersString = parts.drop(4).joinToString("|")
                    )
                    "PLAYERS" -> PlayerListUpdate(
                        playersString = parts.drop(1).joinToString("|")
                    )
                    "COUNTDOWN" -> CountdownStart(
                        startTimeMillis = parts[1].toLong(),
                        trackId = parts[2],
                        totalLaps = parts[3].toInt()
                    )
                    "TEL" -> {
                        val tel = CarTelemetry(
                            playerId = parts[1],
                            x = parts[3].toFloat(),
                            y = parts[4].toFloat(),
                            vx = parts[5].toFloat(),
                            vy = parts[6].toFloat(),
                            angleDeg = parts[7].toFloat(),
                            angularVelocity = parts[8].toFloat(),
                            steerInput = parts[9].toFloat(),
                            throttleInput = parts[10].toFloat(),
                            isDrifting = parts[11] == "1",
                            isBoosting = parts[12] == "1",
                            nitroAmount = parts[13].toFloat(),
                            currentLap = parts[14].toInt(),
                            nextCheckpointIndex = parts[15].toInt(),
                            totalCheckpointsHit = parts[16].toInt(),
                            isFinished = parts[17] == "1",
                            finishTimeMs = parts[18].toLong(),
                            timestamp = parts[19].toLong()
                        )
                        Telemetry(telemetry = tel, sequence = parts[2].toLong())
                    }
                    "BUMP" -> CollisionEvent(
                        id1 = parts[1],
                        id2 = parts[2],
                        nx = parts[3].toFloat(),
                        ny = parts[4].toFloat(),
                        impulse = parts[5].toFloat()
                    )
                    "PING" -> Ping(sendTime = parts[1].toLong())
                    "PONG" -> Pong(originalSendTime = parts[1].toLong())
                    "LEAVE" -> Leave(playerId = parts[1])
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }

        // Helper to encode player list
        fun encodePlayers(players: List<PlayerCar>): String {
            return players.joinToString(";") { p ->
                "${p.id},${p.name},${p.carType.name},${p.colorHex},${p.isHost},${p.isReady},${p.pingMs}"
            }
        }

        // Helper to decode player list
        fun decodePlayers(encoded: String): List<PlayerCar> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { entry ->
                try {
                    val fields = entry.split(",")
                    if (fields.size >= 7) {
                        PlayerCar(
                            id = fields[0],
                            name = fields[1],
                            carType = CarType.valueOf(fields[2]),
                            colorHex = fields[3].toLong(),
                            isHost = fields[4].toBoolean(),
                            isReady = fields[5].toBoolean(),
                            pingMs = fields[6].toInt()
                        )
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}
