package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerCar
import com.example.model.RaceSettings
import com.example.model.TrackRepository
import com.example.network.DiscoveredRoom
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    isHost: Boolean,
    localIp: String,
    roomName: String,
    players: List<PlayerCar>,
    discoveredRooms: List<DiscoveredRoom>,
    isConnectedToHost: Boolean,
    settings: RaceSettings,
    pingMs: Int,
    onUpdateSettings: (RaceSettings) -> Unit,
    onJoinRoom: (DiscoveredRoom) -> Unit,
    onDirectConnect: (String) -> Unit,
    onStartRace: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manualIpInput by remember { mutableStateOf("192.168.43.1") }
    var showDirectConnectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHost) "HOSTING MULTIPLAYER" else if (isConnectedToHost) "RACE LOBBY" else "JOIN MULTIPLAYER",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isHost) "Local IP: $localIp (Port 8889)" else if (isConnectedToHost) "Connected to Host (${pingMs}ms)" else "Local Wi-Fi / Hotspot Radar",
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onLeave, modifier = Modifier.testTag("lobby_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
                    }
                },
                actions = {
                    if (!isHost && !isConnectedToHost) {
                        IconButton(
                            onClick = { showDirectConnectDialog = true },
                            modifier = Modifier.testTag("direct_ip_button")
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Direct IP", tint = TurboOrange)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkAsphalt,
                    titleContentColor = TextLight
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkAsphalt)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                if (isHost) {
                    Button(
                        onClick = onStartRace,
                        colors = ButtonDefaults.buttonColors(containerColor = TurboOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_race_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START RACE (${players.size} RACERS)", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                    }
                } else if (isConnectedToHost) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Waiting for Host to start race...", fontSize = 13.sp, color = TextLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = DarkAsphalt
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // If Client is not yet connected: Show Discovered Rooms Scanner & Connect Options
            if (!isHost && !isConnectedToHost) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = TurboOrange,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Scanning for Hotspot & Wi-Fi Games...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextLight)
                                Text("Make sure you are connected to the Host's hotspot or Wi-Fi.", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }

                item {
                    Text("DISCOVERED LOBBIES", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.sp)
                }

                if (discoveredRooms.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.WifiFind, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No rooms detected yet", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Ask your friend to tap 'Host Game', or connect via direct IP below.", color = TextMuted, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { showDirectConnectDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TurboOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("enter_ip_manually_button")
                                ) {
                                    Text("Enter Host IP Manually")
                                }
                            }
                        }
                    }
                } else {
                    items(discoveredRooms) { room ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NeonCyan, RoundedCornerShape(14.dp))
                                .clickable { onJoinRoom(room) }
                                .testTag("discovered_room_${room.roomId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(room.roomName, fontWeight = FontWeight.Black, fontSize = 15.sp, color = TextLight)
                                    Text("Host: ${room.hostName} • IP: ${room.hostIp}", fontSize = 11.sp, color = NeonCyan)
                                }
                                Button(
                                    onClick = { onJoinRoom(room) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("JOIN", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Connected Room or Host View

                // Host Settings: Track Selector
                if (isHost) {
                    item {
                        Text("SELECT TRACK", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.sp)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TrackRepository.ALL_TRACKS.forEach { track ->
                                val isSelected = track.id == settings.trackId
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) TurboOrange else CardAsphalt)
                                        .border(1.dp, if (isSelected) TurboOrange else BorderSlate, RoundedCornerShape(10.dp))
                                        .clickable { onUpdateSettings(settings.copy(trackId = track.id)) }
                                        .padding(vertical = 12.dp, horizontal = 6.dp)
                                        .testTag("track_${track.id}")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = track.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.Black else TextLight,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = track.difficulty,
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color.Black.copy(alpha = 0.7f) else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Laps selector
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RACE LAPS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 2, 3, 5).forEach { laps ->
                                    val isLapsSelected = settings.totalLaps == laps
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isLapsSelected) NeonCyan else CardAsphalt)
                                            .border(1.dp, if (isLapsSelected) NeonCyan else BorderSlate, RoundedCornerShape(8.dp))
                                            .clickable { onUpdateSettings(settings.copy(totalLaps = laps)) }
                                            .testTag("laps_$laps")
                                    ) {
                                        Text(
                                            text = "$laps",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = if (isLapsSelected) Color.Black else TextLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Connected Players List
                item {
                    Text("RACERS IN LOBBY (${players.size})", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.sp)
                }

                items(players) { p ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(p.colorHex))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(p.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = TextLight)
                                        if (p.isHost) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TurboOrange)
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("HOST", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                            }
                                        }
                                    }
                                    Text("${p.carType.displayName}", fontSize = 11.sp, color = TextMuted)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = NeonLime, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct IP Connect Dialog
    if (showDirectConnectDialog) {
        AlertDialog(
            onDismissRequest = { showDirectConnectDialog = false },
            title = { Text("Direct IP Join", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Enter the Host's IP address (Android hotspot default is 192.168.43.1):", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it },
                        label = { Text("Host IP Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = TurboOrange,
                            cursorColor = TurboOrange
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("manual_ip_field")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { manualIpInput = "192.168.43.1" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Hotspot Default (192.168.43.1)", fontSize = 11.sp, color = NeonCyan)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDirectConnectDialog = false
                        onDirectConnect(manualIpInput.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurboOrange),
                    modifier = Modifier.testTag("connect_ip_confirm_button")
                ) {
                    Text("CONNECT", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectConnectDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = CardAsphalt,
            textContentColor = TextLight,
            titleContentColor = TextLight
        )
    }
}
