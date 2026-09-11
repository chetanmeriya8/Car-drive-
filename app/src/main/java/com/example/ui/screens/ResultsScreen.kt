package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RaceResult
import com.example.ui.components.formatLapTime
import com.example.ui.theme.*

@Composable
fun ResultsScreen(
    results: List<RaceResult>,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winner = results.firstOrNull()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkAsphalt)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onMainMenu,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("results_menu_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MENU", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurboOrange),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("results_replay_button")
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RACE AGAIN", fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        },
        containerColor = DarkAsphalt
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Trophy icon & Title
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF21262D))
                    .border(2.dp, TrophyGold, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = TrophyGold,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "RACE FINISHED!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextLight,
                letterSpacing = 1.5.sp
            )

            if (winner != null) {
                Text(
                    text = "${winner.playerName} Takes 1st Place!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }

            // Results Leaderboard Table
            Text(
                text = "FINAL STANDINGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = TextMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(results) { res ->
                    val rankColor = when (res.rank) {
                        1 -> TrophyGold
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> TextLight
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.isLocalPlayer) Color(0xFF1F2937) else CardAsphalt
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (res.isLocalPlayer) 1.5.dp else 1.dp,
                                color = if (res.isLocalPlayer) TurboOrange else BorderSlate,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0D1117))
                                ) {
                                    Text(
                                        text = "${res.rank}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = rankColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = res.playerName,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = TextLight
                                        )
                                        if (res.isLocalPlayer) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(YOU)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TurboOrange
                                            )
                                        }
                                    }
                                    Text(
                                        text = res.carType.displayName,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatLapTime(res.totalTimeMs),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextLight
                                )
                                Text(
                                    text = "Best: ${formatLapTime(res.bestLapMs)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
