package com.example.network

import com.example.model.CarTelemetry
import com.example.model.PlayerCar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class HotspotClient(
    val localPlayer: PlayerCar
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var gameSocket: DatagramSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private var isRunning = false
    private var isDiscoveryRunning = false

    private var hostAddress: InetAddress? = null
    private var hostPort: Int = NetworkConfig.GAME_PORT

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lobbyPlayers = MutableStateFlow<List<PlayerCar>>(emptyList())
    val lobbyPlayers: StateFlow<List<PlayerCar>> = _lobbyPlayers.asStateFlow()

    private val _selectedTrackId = MutableStateFlow("grand_prix")
    val selectedTrackId: StateFlow<String> = _selectedTrackId.asStateFlow()

    private val _totalLaps = MutableStateFlow(3)
    val totalLaps: StateFlow<Int> = _totalLaps.asStateFlow()

    private val _pingMs = MutableStateFlow(0)
    val pingMs: StateFlow<Int> = _pingMs.asStateFlow()

    // Remote cars telemetry map
    val remoteCarTelemetry = ConcurrentHashMap<String, CarTelemetry>()

    var onCountdownStarted: ((Long, String, Int) -> Unit)? = null
    var onRemoteBump: ((Packet.CollisionEvent) -> Unit)? = null

    fun startDiscovery() {
        if (isDiscoveryRunning) return
        isDiscoveryRunning = true

        scope.launch {
            try {
                val socket = DatagramSocket(NetworkConfig.DISCOVERY_PORT)
                socket.broadcast = true
                discoverySocket = socket
                val buffer = ByteArray(1024)

                while (isDiscoveryRunning && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val message = String(packet.data, 0, packet.length).trim()
                    val parsed = Packet.parse(message)
                    if (parsed is Packet.DiscoveryBeacon) {
                        val room = DiscoveredRoom(
                            roomId = parsed.roomId,
                            roomName = parsed.roomName,
                            hostName = parsed.hostName,
                            hostIp = packet.address.hostAddress ?: parsed.hostIp,
                            port = parsed.port,
                            trackId = parsed.trackId,
                            playerCount = parsed.playerCount,
                            maxPlayers = parsed.maxPlayers
                        )

                        val current = _discoveredRooms.value.filter {
                            it.roomId != room.roomId && (System.currentTimeMillis() - it.lastSeenMs < 5000L)
                        }
                        _discoveredRooms.value = current + room
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun stopDiscovery() {
        isDiscoveryRunning = false
        try {
            discoverySocket?.close()
        } catch (_: Exception) {}
    }

    fun connectToHost(hostIp: String, port: Int = NetworkConfig.GAME_PORT) {
        stopDiscovery()
        isRunning = true

        scope.launch {
            try {
                val addr = InetAddress.getByName(hostIp)
                hostAddress = addr
                hostPort = port

                val socket = DatagramSocket()
                gameSocket = socket

                // Start receiver loop
                launch {
                    val buffer = ByteArray(1024)
                    while (isRunning && !socket.isClosed) {
                        val recvPacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(recvPacket)
                        val message = String(recvPacket.data, 0, recvPacket.length).trim()
                        handleIncomingMessage(message)
                    }
                }

                // Send Join Request
                val joinPacket = Packet.JoinRequest(
                    playerId = localPlayer.id,
                    playerName = localPlayer.name,
                    carType = localPlayer.carType,
                    colorHex = localPlayer.colorHex
                )
                sendToHost(joinPacket.serialize())

                // Start ping loop
                launch {
                    while (isRunning) {
                        val now = System.currentTimeMillis()
                        sendToHost(Packet.Ping(now).serialize())
                        delay(1000L)
                    }
                }
            } catch (_: Exception) {
                _isConnected.value = false
            }
        }
    }

    private fun handleIncomingMessage(message: String) {
        val packet = Packet.parse(message) ?: return

        when (packet) {
            is Packet.JoinAccepted -> {
                _isConnected.value = true
                _selectedTrackId.value = packet.trackId
                _totalLaps.value = packet.totalLaps
                _lobbyPlayers.value = Packet.decodePlayers(packet.playersString)
            }

            is Packet.PlayerListUpdate -> {
                _lobbyPlayers.value = Packet.decodePlayers(packet.playersString)
            }

            is Packet.CountdownStart -> {
                _selectedTrackId.value = packet.trackId
                _totalLaps.value = packet.totalLaps
                onCountdownStarted?.invoke(packet.startTimeMillis, packet.trackId, packet.totalLaps)
            }

            is Packet.Telemetry -> {
                val tel = packet.telemetry
                if (tel.playerId != localPlayer.id) {
                    remoteCarTelemetry[tel.playerId] = tel
                }
            }

            is Packet.CollisionEvent -> {
                onRemoteBump?.invoke(packet)
            }

            is Packet.Pong -> {
                val rtt = (System.currentTimeMillis() - packet.originalSendTime).toInt().coerceAtLeast(1)
                _pingMs.value = rtt
            }

            is Packet.Leave -> {
                remoteCarTelemetry.remove(packet.playerId)
                _lobbyPlayers.value = _lobbyPlayers.value.filter { it.id != packet.playerId }
            }

            else -> {}
        }
    }

    fun sendTelemetry(telemetry: CarTelemetry) {
        if (!isRunning) return
        val packet = Packet.Telemetry(telemetry)
        sendToHost(packet.serialize())
    }

    fun sendCollision(bump: Packet.CollisionEvent) {
        if (!isRunning) return
        sendToHost(bump.serialize())
    }

    private fun sendToHost(message: String) {
        val addr = hostAddress ?: return
        scope.launch {
            try {
                val bytes = message.toByteArray()
                val packet = DatagramPacket(bytes, bytes.size, addr, hostPort)
                gameSocket?.send(packet)
            } catch (_: Exception) {}
        }
    }

    fun disconnect() {
        if (isRunning) {
            sendToHost(Packet.Leave(localPlayer.id).serialize())
        }
        isRunning = false
        _isConnected.value = false
        try {
            gameSocket?.close()
        } catch (_: Exception) {}
        stopDiscovery()
        scope.cancel()
    }
}
