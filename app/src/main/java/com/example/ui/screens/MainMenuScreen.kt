package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerCar
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    playerCar: PlayerCar,
    networkStatus: String,
    onQuickRace: () -> Unit,
    onHostGame: () -> Unit,
    onJoinGame: () -> Unit,
    onOpenGarage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showGuide by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF161B22),
                        DarkAsphalt,
                        Color(0xFF070A0E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Network connection badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC161B22))
                            .border(1.dp, BorderSlate, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = NeonLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = networkStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime
                        )
                    }

                    // Help / Guide button
                    IconButton(
                        onClick = { showGuide = true },
                        modifier = Modifier.testTag("hotspot_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Hotspot Guide",
                            tint = NeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // App Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsScore,
                        contentDescription = null,
                        tint = TurboOrange,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HOTSPOT RACERS",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = TextLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "REAL-TIME OFFLINE MULTIPLAYER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TurboOrange,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            // MIDDLE: RACER PROFILE & ACTION CARDS
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Profile Banner (Car preview & Garage quick-link)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                        .clickable { onOpenGarage() }
                        .testTag("garage_profile_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(playerCar.colorHex))
                            ) {
                                Icon(Icons.Default.DriveEta, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(playerCar.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = TextLight)
                                Text(playerCar.carType.displayName, fontSize = 12.sp, color = NeonCyan)
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenGarage,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TurboOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("tune_car_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TUNING", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Action Card 1: Quick Race (Single Player)
                MenuActionCard(
                    title = "QUICK RACE (SOLO & AI)",
                    subtitle = "Instant circuit action with smart AI bot rivals",
                    icon = Icons.Default.Bolt,
                    accentColor = NeonCyan,
                    testTag = "quick_race_card",
                    onClick = onQuickRace
                )

                // Action Card 2: Host Hotspot Game
                MenuActionCard(
                    title = "HOST HOTSPOT RACE",
                    subtitle = "Create local room for nearby friends to join",
                    icon = Icons.Default.WifiTethering,
                    accentColor = TurboOrange,
                    testTag = "host_game_card",
                    onClick = onHostGame
                )

                // Action Card 3: Join Hotspot Game
                MenuActionCard(
                    title = "JOIN HOTSPOT RACE",
                    subtitle = "Auto-scan LAN lobbies or connect via direct IP",
                    icon = Icons.Default.Sensors,
                    accentColor = NeonLime,
                    testTag = "join_game_card",
                    onClick = onJoinGame
                )
            }

            // FOOTER: Info & Guide Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGuide = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Help, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "How to play via Hotspot without internet",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showGuide) {
        HotspotGuideDialog(onDismiss = { showGuide = false })
    }
}

@Composable
private fun MenuActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardAsphalt),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = TextLight, letterSpacing = 0.5.sp)
                Text(subtitle, fontSize = 11.sp, color = TextMuted)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
