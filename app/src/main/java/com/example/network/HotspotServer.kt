package com.example.network

import com.example.model.CarTelemetry
import com.example.model.PlayerCar
import com.example.model.RaceSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class HotspotServer(
    private val localIp: String,
    private val broadcastIp: String,
    private val hostPlayer: PlayerCar,
    private val initialSettings: RaceSettings
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var gameSocket: DatagramSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private var isRunning = false

    data class ClientEndpoint(
        val playerId: String,
        val address: InetAddress,
        val port: Int,
        var lastSeenMs: Long = System.currentTimeMillis()
    )

    private val clients = ConcurrentHashMap<String, ClientEndpoint>()
    private val _connectedPlayers = MutableStateFlow<List<PlayerCar>>(listOf(hostPlayer))
    val connectedPlayers: StateFlow<List<PlayerCar>> = _connectedPlayers.asStateFlow()

    private val _raceSettings = MutableStateFlow(initialSettings)
    val raceSettings: StateFlow<RaceSettings> = _raceSettings.asStateFlow()

    // Remote players' latest telemetry
    val remoteCarTelemetry = ConcurrentHashMap<String, CarTelemetry>()

    // Callback for server events (e.g. race started, client left)
    var onCountdownStarted: ((Long, String, Int) -> Unit)? = null
    var onRemoteBump: ((Packet.CollisionEvent) -> Unit)? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            runGameSocketListener()
        }

        scope.launch {
            runDiscoveryBroadcaster()
        }

        scope.launch {
            runClientPruningLoop()
        }
    }

    fun updateSettings(newSettings: RaceSettings) {
        _raceSettings.value = newSettings
        broadcastPlayerList()
    }

    fun startRaceCountdown() {
        val startTimestamp = System.currentTimeMillis() + 3000L // 3s synchronized countdown
        val packet = Packet.CountdownStart(
            startTimeMillis = startTimestamp,
            trackId = _raceSettings.value.trackId,
            totalLaps = _raceSettings.value.totalLaps
        )
        sendToAll(packet.serialize())
        onCountdownStarted?.invoke(startTimestamp, _raceSettings.value.trackId, _raceSettings.value.totalLaps)
    }

    fun broadcastHostTelemetry(telemetry: CarTelemetry) {
        val packet = Packet.Telemetry(telemetry)
        sendToAll(packet.serialize())
    }

    fun broadcastCollision(bump: Packet.CollisionEvent) {
        sendToAll(bump.serialize())
    }

    private suspend fun runGameSocketListener() = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket(NetworkConfig.GAME_PORT)
            gameSocket = socket
            val buffer = ByteArray(1024)

            while (isRunning && !socket.isClosed) {
                val recvPacket = DatagramPacket(buffer, buffer.size)
                socket.receive(recvPacket)

                val message = String(recvPacket.data, 0, recvPacket.length).trim()
                handleIncomingMessage(message, recvPacket.address, recvPacket.port)
            }
        } catch (_: Exception) {
            // Socket closed or error
        }
    }

    private fun handleIncomingMessage(message: String, address: InetAddress, port: Int) {
        val packet = Packet.parse(message) ?: return

        when (packet) {
            is Packet.JoinRequest -> {
                val newPlayer = PlayerCar(
                    id = packet.playerId,
                    name = packet.playerName,
                    carType = packet.carType,
                    colorHex = packet.colorHex,
                    isHost = false,
                    isReady = true
                )

                clients[packet.playerId] = ClientEndpoint(
                    playerId = packet.playerId,
                    address = address,
                    port = port
                )

                val updatedList = _connectedPlayers.value.filter { it.id != packet.playerId } + newPlayer
                _connectedPlayers.value = updatedList

                // Reply to joiner with accept
                val acceptPacket = Packet.JoinAccepted(
                    assignedId = packet.playerId,
                    trackId = _raceSettings.value.trackId,
                    totalLaps = _raceSettings.value.totalLaps,
                    playersString = Packet.encodePlayers(updatedList)
                )
                sendToClient(address, port, acceptPacket.serialize())

                // Broadcast updated player list to all clients
                broadcastPlayerList()
            }

            is Packet.Telemetry -> {
                val pId = packet.telemetry.playerId
                clients[pId]?.lastSeenMs = System.currentTimeMillis()
                remoteCarTelemetry[pId] = packet.telemetry

                // Forward telemetry to all other clients
                forwardToOtherClients(pId, message)
            }

            is Packet.CollisionEvent -> {
                onRemoteBump?.invoke(packet)
                forwardToOtherClients(packet.id1, message)
            }

            is Packet.Ping -> {
                // Reply with Pong immediately
                val pong = Packet.Pong(packet.sendTime)
                sendToClient(address, port, pong.serialize())
            }

            is Packet.Leave -> {
                clients.remove(packet.playerId)
                remoteCarTelemetry.remove(packet.playerId)
                _connectedPlayers.value = _connectedPlayers.value.filter { it.id != packet.playerId }
                broadcastPlayerList()
            }

            else -> {}
        }
    }

    private fun broadcastPlayerList() {
        val encoded = Packet.encodePlayers(_connectedPlayers.value)
        val packet = Packet.PlayerListUpdate(encoded)
        sendToAll(packet.serialize())
    }

    private fun sendToClient(address: InetAddress, port: Int, message: String) {
        scope.launch {
            try {
                val bytes = message.toByteArray()
                val packet = DatagramPacket(bytes, bytes.size, address, port)
                gameSocket?.send(packet)
            } catch (_: Exception) {}
        }
    }

    private fun sendToAll(message: String) {
        val bytes = message.toByteArray()
        for ((_, endpoint) in clients) {
            try {
                val packet = DatagramPacket(bytes, bytes.size, endpoint.address, endpoint.port)
                gameSocket?.send(packet)
            } catch (_: Exception) {}
        }
    }

    private fun forwardToOtherClients(sourcePlayerId: String, message: String) {
        val bytes = message.toByteArray()
        for ((pId, endpoint) in clients) {
            if (pId != sourcePlayerId) {
                try {
                    val packet = DatagramPacket(bytes, bytes.size, endpoint.address, endpoint.port)
                    gameSocket?.send(packet)
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun runDiscoveryBroadcaster() = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            discoverySocket = socket

            while (isRunning && !socket.isClosed) {
                val beacon = Packet.DiscoveryBeacon(
                    roomId = hostPlayer.id,
                    roomName = "${hostPlayer.name}'s Race",
                    hostName = hostPlayer.name,
                    hostIp = localIp,
                    port = NetworkConfig.GAME_PORT,
                    trackId = _raceSettings.value.trackId,
                    playerCount = _connectedPlayers.value.size,
                    maxPlayers = _raceSettings.value.maxPlayers
                )
                val bytes = beacon.serialize().toByteArray()

                // Send to subnet broadcast address
                try {
                    val targetAddr = InetAddress.getByName(broadcastIp)
                    val packet = DatagramPacket(bytes, bytes.size, targetAddr, NetworkConfig.DISCOVERY_PORT)
                    socket.send(packet)
                } catch (_: Exception) {}

                // Also send to global broadcast fallback
                try {
                    val globalAddr = InetAddress.getByName("255.255.255.255")
                    val packet2 = DatagramPacket(bytes, bytes.size, globalAddr, NetworkConfig.DISCOVERY_PORT)
                    socket.send(packet2)
                } catch (_: Exception) {}

                delay(NetworkConfig.HEARTBEAT_INTERVAL_MS)
            }
        } catch (_: Exception) {}
    }

    private suspend fun runClientPruningLoop() {
        while (isRunning) {
            delay(2000L)
            val now = System.currentTimeMillis()
            var modified = false
            for ((pId, client) in clients) {
                if (now - client.lastSeenMs > NetworkConfig.CLIENT_TIMEOUT_MS) {
                    clients.remove(pId)
                    remoteCarTelemetry.remove(pId)
                    modified = true
                }
            }
            if (modified) {
                val remainingIds = clients.keys + hostPlayer.id
                _connectedPlayers.value = _connectedPlayers.value.filter { it.id in remainingIds }
                broadcastPlayerList()
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            gameSocket?.close()
        } catch (_: Exception) {}
        try {
            discoverySocket?.close()
        } catch (_: Exception) {}
        scope.cancel()
    }
}
