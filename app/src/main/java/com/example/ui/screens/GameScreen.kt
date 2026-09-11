package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.haptics.HapticController
import com.example.model.*
import com.example.network.HotspotClient
import com.example.network.HotspotServer
import com.example.network.RemoteCarInterpolator
import com.example.physics.BotAi
import com.example.physics.CarPhysics
import com.example.physics.ParticleManager
import com.example.ui.components.GameCanvas
import com.example.ui.components.GameControls
import com.example.ui.components.GameHud
import com.example.ui.components.MiniMap
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.sqrt

@Composable
fun GameScreen(
    track: Track,
    totalLaps: Int,
    localPlayer: PlayerCar,
    lobbyPlayers: List<PlayerCar>,
    isHost: Boolean,
    isMultiplayer: Boolean,
    server: HotspotServer?,
    client: HotspotClient?,
    pingMs: Int,
    onRaceFinished: (List<RaceResult>) -> Unit,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = remember { HapticController(context) }
    val particleManager = remember { ParticleManager() }

    // Spawn positions for players based on grid slots
    val myGridSlot = lobbyPlayers.indexOfFirst { it.id == localPlayer.id }.coerceAtLeast(0)
    val startPos = track.gridSlots.getOrElse(myGridSlot) { track.startPosition }

    val playerPhysics = remember {
        CarPhysics(
            playerId = localPlayer.id,
            carType = localPlayer.carType,
            initialPosition = startPos,
            initialAngleDeg = track.startAngleDeg
        ).apply {
            onWallCollision = { force -> haptic.vibrateCollision(force) }
            onBoostPadHit = { haptic.vibrateNitro() }
            onLapCompleted = { _, _ -> haptic.vibrateLap() }
        }
    }

    // Remote cars interpolators (for real network opponents)
    val remoteCarMap = remember { mutableStateMapOf<String, RemoteCarInterpolator>() }

    // Bot AI drivers (used in Single Player or to fill slots)
    val botDrivers = remember {
        val bots = mutableListOf<BotAi>()
        if (!isMultiplayer) {
            val botCount = 3
            val botPresets = listOf(
                Triple("Rival Blaze", CarType.TURBO_VIPER, 0xFFFF1744),
                Triple("Shadow Apex", CarType.DRIFT_KING, 0xFF00E5FF),
                Triple("Cyber Ghost", CarType.CYBER_BEAST, 0xFF00E676)
            )
            for (i in 0 until botCount) {
                val preset = botPresets[i]
                val slotIdx = (myGridSlot + i + 1) % track.gridSlots.size
                val bPos = track.gridSlots[slotIdx]
                val bPhysics = CarPhysics(
                    playerId = "bot_$i",
                    carType = preset.second,
                    initialPosition = bPos,
                    initialAngleDeg = track.startAngleDeg
                )
                bots.add(BotAi(bPhysics, preset.first, preset.third))
                remoteCarMap["bot_$i"] = RemoteCarInterpolator("bot_$i", bPos.x, bPos.y, track.startAngleDeg)
            }
        }
        bots
    }

    // Combined players list including bots for UI/rendering
    val allRacerProfiles = remember {
        val list = lobbyPlayers.toMutableList()
        for (bot in botDrivers) {
            list.add(
                PlayerCar(
                    id = bot.physics.playerId,
                    name = bot.name,
                    carType = bot.physics.carType,
                    colorHex = bot.colorHex,
                    isBot = true
                )
            )
        }
        list
    }

    // Game states
    var isPaused by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(3) }
    var isCountdownActive by remember { mutableStateOf(true) }
    var isRaceActive by remember { mutableStateOf(false) }
    var playerRank by remember { mutableStateOf(1) }

    // Setup network remote cars
    LaunchedEffect(lobbyPlayers) {
        for (p in lobbyPlayers) {
            if (p.id != localPlayer.id && !remoteCarMap.containsKey(p.id)) {
                val slot = lobbyPlayers.indexOf(p).coerceAtLeast(0)
                val pos = track.gridSlots.getOrElse(slot) { track.startPosition }
                remoteCarMap[p.id] = RemoteCarInterpolator(p.id, pos.x, pos.y, track.startAngleDeg)
            }
        }
    }

    // Countdown sequence (3... 2... 1... GO!)
    LaunchedEffect(Unit) {
        haptic.vibrateTap()
        delay(800L)
        countdownValue = 2
        haptic.vibrateTap()
        delay(800L)
        countdownValue = 1
        haptic.vibrateTap()
        delay(800L)
        countdownValue = 0 // GO!
        haptic.vibrateLap()
        isRaceActive = true
        playerPhysics.lapStartTimeMs = System.currentTimeMillis()
        delay(700L)
        isCountdownActive = false
    }

    // Main 60 FPS Game Loop
    LaunchedEffect(isRaceActive, isPaused) {
        var lastTime = System.nanoTime()
        var telemetryAccumulator = 0f
        val telemetryInterval = 1f / 40f // 40 Hz network broadcast

        while (true) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastTime = now

            if (!isPaused) {
                // 1. Update local car physics
                playerPhysics.update(dt, track, totalLaps, particleManager, isRaceActive)

                // 2. Update Bot AI drivers
                for (bot in botDrivers) {
                    bot.step(dt, track, particleManager, isRaceActive)
                    remoteCarMap[bot.physics.playerId]?.let { interp ->
                        interp.currentX = bot.physics.x
                        interp.currentY = bot.physics.y
                        interp.currentAngle = bot.physics.angleDeg
                        interp.currentVx = bot.physics.vx
                        interp.currentVy = bot.physics.vy
                        interp.isDrifting = bot.physics.isDrifting
                        interp.isBoosting = bot.physics.isBoosting
                    }
                }

                // 3. Update remote network cars
                val remoteTel = if (isHost) server?.remoteCarTelemetry else client?.remoteCarTelemetry
                if (remoteTel != null) {
                    for ((pId, interp) in remoteCarMap) {
                        if (!pId.startsWith("bot_")) {
                            val tel = remoteTel[pId]
                            interp.step(dt, tel)
                        }
                    }
                }

                // 4. Update particles (smoke, sparks, skid marks)
                particleManager.update(dt)

                // 5. Car-to-Car collisions
                checkCarCollisions(playerPhysics, remoteCarMap, particleManager, haptic)

                // 6. Calculate real-time rank
                playerRank = computePlayerRank(playerPhysics, remoteCarMap, track)

                // 7. Network Telemetry Broadcast (40Hz)
                if (isMultiplayer) {
                    telemetryAccumulator += dt
                    if (telemetryAccumulator >= telemetryInterval) {
                        telemetryAccumulator = 0f
                        val currentTel = playerPhysics.telemetry
                        if (isHost) {
                            server?.broadcastHostTelemetry(currentTel)
                        } else {
                            client?.sendTelemetry(currentTel)
                        }
                    }
                }

                // 8. Check if local player finished race
                if (playerPhysics.isFinished) {
                    // Build final standings
                    val results = buildFinalResults(playerPhysics, localPlayer, remoteCarMap, allRacerProfiles, playerRank)
                    onRaceFinished(results)
                    break
                }
            }

            delay(16L) // ~60 FPS frame delay
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. Canvas Racing World
        GameCanvas(
            track = track,
            playerPhysics = playerPhysics,
            playerCar = localPlayer,
            remoteCars = remoteCarMap,
            lobbyPlayers = allRacerProfiles,
            particleManager = particleManager,
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD (Speedometer, Lap, Time, Rank, Pause)
        GameHud(
            speedPxSec = hypot(playerPhysics.vx, playerPhysics.vy),
            currentLap = playerPhysics.currentLap,
            totalLaps = totalLaps,
            rank = playerRank,
            lapTimeMs = playerPhysics.currentLapTimeMs,
            bestLapMs = playerPhysics.bestLapTimeMs,
            pingMs = pingMs,
            isMultiplayer = isMultiplayer,
            onPauseClick = { isPaused = true },
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
        )

        // 3. MiniMap Radar
        MiniMap(
            track = track,
            playerPhysics = playerPhysics,
            remoteCars = remoteCarMap,
            lobbyPlayers = allRacerProfiles,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 60.dp)
        )

        // 4. Low-latency Touch Controls
        GameControls(
            nitroAmount = playerPhysics.nitroGauge,
            isBoosting = playerPhysics.isBoosting,
            onSteerChange = { steer -> playerPhysics.steerInput = steer },
            onThrottleChange = { throttle -> playerPhysics.throttleInput = throttle },
            onDriftChange = { drift -> playerPhysics.isDrifting = drift },
            onNitroChange = { nitro -> playerPhysics.isBoosting = nitro && playerPhysics.nitroGauge > 0.05f },
            hapticController = haptic,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )

        // 5. Synchronized Countdown Overlay
        AnimatedVisibility(
            visible = isCountdownActive,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color(0xEE0D1117))
                    .border(3.dp, if (countdownValue == 0) NeonLime else TurboOrange, CircleShape)
            ) {
                Text(
                    text = if (countdownValue > 0) "$countdownValue" else "GO!",
                    fontSize = if (countdownValue > 0) 64.sp else 46.sp,
                    fontWeight = FontWeight.Black,
                    color = if (countdownValue == 0) NeonLime else TurboOrange
                )
            }
        }

        // 6. Pause Dialog
        if (isPaused) {
            AlertDialog(
                onDismissRequest = { isPaused = false },
                title = { Text("RACE PAUSED", fontWeight = FontWeight.Black) },
                text = {
                    Text("Game is paused. You can resume or return to the main menu.", color = TextMuted)
                },
                confirmButton = {
                    Button(
                        onClick = { isPaused = false },
                        colors = ButtonDefaults.buttonColors(containerColor = TurboOrange),
                        modifier = Modifier.testTag("resume_race_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESUME", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isPaused = false
                            onExitGame()
                        },
                        modifier = Modifier.testTag("quit_race_button")
                    ) {
                        Text("EXIT RACE", color = RacingRed, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CardAsphalt,
                textContentColor = TextLight,
                titleContentColor = TextLight
            )
        }
    }
}

private fun checkCarCollisions(
    playerPhysics: CarPhysics,
    remoteCars: Map<String, RemoteCarInterpolator>,
    particleManager: ParticleManager,
    haptic: HapticController
) {
    val collisionDist = 28f
    for ((_, remote) in remoteCars) {
        val dx = playerPhysics.x - remote.currentX
        val dy = playerPhysics.y - remote.currentY
        val dist = hypot(dx, dy)
        if (dist > 0 && dist < collisionDist) {
            val nx = dx / dist
            val ny = dy / dist
            val impulse = 120f
            playerPhysics.applyImpulse(nx * impulse, ny * impulse)
            remote.currentX -= nx * (collisionDist - dist) * 0.5f
            remote.currentY -= ny * (collisionDist - dist) * 0.5f

            particleManager.spawnSparks(
                androidx.compose.ui.geometry.Offset(playerPhysics.x, playerPhysics.y),
                nx,
                ny
            )
            haptic.vibrateCollision(90f)
        }
    }
}

private fun computePlayerRank(
    player: CarPhysics,
    remoteCars: Map<String, RemoteCarInterpolator>,
    track: Track
): Int {
    var rank = 1
    val playerScore = player.currentLap * 10000 + player.totalCheckpointsHit * 100

    for ((_, remote) in remoteCars) {
        // Approximate distance score for opponents
        val opponentScore = 1 * 10000 // base
        if (opponentScore > playerScore) {
            rank++
        }
    }
    return rank
}

private fun buildFinalResults(
    localPhysics: CarPhysics,
    localProfile: PlayerCar,
    remoteCars: Map<String, RemoteCarInterpolator>,
    lobbyPlayers: List<PlayerCar>,
    finalRank: Int
): List<RaceResult> {
    val results = mutableListOf<RaceResult>()

    // Local player result
    val localResult = RaceResult(
        rank = finalRank,
        playerId = localProfile.id,
        playerName = localProfile.name,
        carType = localProfile.carType,
        colorHex = localProfile.colorHex,
        totalTimeMs = localPhysics.finishTimeMs.coerceAtLeast(localPhysics.currentLapTimeMs),
        bestLapMs = localPhysics.bestLapTimeMs.coerceAtLeast(24000L),
        isLocalPlayer = true
    )
    results.add(localResult)

    // Opponent results
    var otherRank = if (finalRank == 1) 2 else 1
    for (p in lobbyPlayers) {
        if (p.id != localProfile.id) {
            val rankToAssign = if (otherRank == finalRank) otherRank + 1 else otherRank
            otherRank = rankToAssign + 1

            results.add(
                RaceResult(
                    rank = rankToAssign,
                    playerId = p.id,
                    playerName = p.name,
                    carType = p.carType,
                    colorHex = p.colorHex,
                    totalTimeMs = localResult.totalTimeMs + (rankToAssign - finalRank) * 2300L,
                    bestLapMs = (localResult.bestLapMs + (rankToAssign - finalRank) * 900L).coerceAtLeast(22000L),
                    isLocalPlayer = false
                )
            )
        }
    }

    return results.sortedBy { it.rank }
}
