package com.example

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.model.*
import com.example.network.DiscoveredRoom
import com.example.network.HotspotClient
import com.example.network.HotspotServer
import com.example.network.NetworkUtils
import com.example.ui.screens.*
import com.example.ui.theme.DarkAsphalt
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    MAIN_MENU,
    GARAGE,
    LOBBY,
    GAME,
    RESULTS
}

class MainActivity : ComponentActivity() {
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("HotspotRacersLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkAsphalt
                ) {
                    HotspotRacersApp(context = this)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun HotspotRacersApp(context: Context) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }

    // Player customization state
    var playerCar by remember {
        mutableStateOf(
            PlayerCar(
                id = "player_" + System.currentTimeMillis().toString().takeLast(5),
                name = "ApexRacer",
                carType = CarType.PHANTOM_GT,
                colorHex = 0xFFFF6D00
            )
        )
    }

    // Networking & Session state
    var isHost by remember { mutableStateOf(false) }
    var isMultiplayer by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf<HotspotServer?>(null) }
    var client by remember { mutableStateOf<HotspotClient?>(null) }
    var raceSettings by remember { mutableStateOf(RaceSettings()) }
    var raceResults by remember { mutableStateOf<List<RaceResult>>(emptyList()) }

    // Collected states from server / client
    val discoveredRooms by client?.discoveredRooms?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val clientLobbyPlayers by client?.lobbyPlayers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val serverLobbyPlayers by server?.connectedPlayers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val isClientConnected by client?.isConnected?.collectAsState() ?: remember { mutableStateOf(false) }
    val pingMs by client?.pingMs?.collectAsState() ?: remember { mutableStateOf(0) }

    val currentLobbyPlayers = remember(isHost, serverLobbyPlayers, clientLobbyPlayers, playerCar) {
        if (isHost) {
            serverLobbyPlayers
        } else if (isClientConnected) {
            clientLobbyPlayers
        } else {
            listOf(playerCar)
        }
    }

    val networkInfo = remember { NetworkUtils.getLocalNetworkInfo(context) }
    val localIp = networkInfo.ipAddress
    val networkStatus = if (networkInfo.isHotspotOrWifi) "${networkInfo.connectionType}: $localIp" else "Local Wireless Mode"

    // Cleanup helper
    fun stopNetwork() {
        server?.stop()
        server = null
        client?.disconnect()
        client = null
        isMultiplayer = false
        isHost = false
    }

    // Screen Routing
    when (currentScreen) {
        AppScreen.MAIN_MENU -> {
            MainMenuScreen(
                playerCar = playerCar,
                networkStatus = networkStatus,
                onQuickRace = {
                    stopNetwork()
                    isMultiplayer = false
                    isHost = false
                    raceSettings = RaceSettings(trackId = "grand_prix", totalLaps = 3)
                    currentScreen = AppScreen.GAME
                },
                onHostGame = {
                    stopNetwork()
                    isMultiplayer = true
                    isHost = true
                    val hostPlayer = playerCar.copy(isHost = true)
                    val srv = HotspotServer(
                        localIp = networkInfo.ipAddress,
                        broadcastIp = networkInfo.broadcastAddress,
                        hostPlayer = hostPlayer,
                        initialSettings = raceSettings
                    )
                    srv.start()
                    server = srv
                    currentScreen = AppScreen.LOBBY
                },
                onJoinGame = {
                    stopNetwork()
                    isMultiplayer = true
                    isHost = false
                    val cl = HotspotClient(localPlayer = playerCar)
                    cl.onCountdownStarted = { _, trackId, laps ->
                        raceSettings = raceSettings.copy(trackId = trackId, totalLaps = laps)
                        currentScreen = AppScreen.GAME
                    }
                    cl.startDiscovery()
                    client = cl
                    currentScreen = AppScreen.LOBBY
                },
                onOpenGarage = {
                    currentScreen = AppScreen.GARAGE
                }
            )
        }

        AppScreen.GARAGE -> {
            GarageScreen(
                currentCar = playerCar,
                onSaveCar = { updated -> playerCar = updated },
                onBack = { currentScreen = AppScreen.MAIN_MENU }
            )
        }

        AppScreen.LOBBY -> {
            LobbyScreen(
                isHost = isHost,
                localIp = localIp,
                roomName = "${playerCar.name}'s Room",
                players = currentLobbyPlayers,
                discoveredRooms = discoveredRooms,
                isConnectedToHost = isClientConnected,
                settings = raceSettings,
                pingMs = pingMs,
                onUpdateSettings = { newSettings ->
                    raceSettings = newSettings
                    server?.updateSettings(newSettings)
                },
                onJoinRoom = { room ->
                    client?.connectToHost(room.hostIp, room.port)
                },
                onDirectConnect = { ip ->
                    client?.connectToHost(ip)
                },
                onStartRace = {
                    if (isHost) {
                        server?.startRaceCountdown()
                        currentScreen = AppScreen.GAME
                    }
                },
                onLeave = {
                    stopNetwork()
                    currentScreen = AppScreen.MAIN_MENU
                }
            )
        }

        AppScreen.GAME -> {
            val track = TrackRepository.getById(raceSettings.trackId)
            GameScreen(
                track = track,
                totalLaps = raceSettings.totalLaps,
                localPlayer = playerCar,
                lobbyPlayers = currentLobbyPlayers,
                isHost = isHost,
                isMultiplayer = isMultiplayer,
                server = server,
                client = client,
                pingMs = pingMs,
                onRaceFinished = { results ->
                    raceResults = results
                    currentScreen = AppScreen.RESULTS
                },
                onExitGame = {
                    stopNetwork()
                    currentScreen = AppScreen.MAIN_MENU
                }
            )
        }

        AppScreen.RESULTS -> {
            ResultsScreen(
                results = raceResults,
                onPlayAgain = {
                    currentScreen = AppScreen.GAME
                },
                onMainMenu = {
                    stopNetwork()
                    currentScreen = AppScreen.MAIN_MENU
                }
            )
        }
    }
}

