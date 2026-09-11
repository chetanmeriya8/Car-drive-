package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.ui.theme.*
import kotlin.math.hypot

@Composable
fun GameHud(
    speedPxSec: Float,
    currentLap: Int,
    totalLaps: Int,
    rank: Int,
    lapTimeMs: Long,
    bestLapMs: Long,
    pingMs: Int,
    isMultiplayer: Boolean,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedKmh = (speedPxSec * 0.45f).toInt()

    val speedColor = when {
        speedKmh > 200 -> RacingRed
        speedKmh > 140 -> TurboOrange
        speedKmh > 80 -> NeonCyan
        else -> TextLight
    }

    val rankSuffix = when (rank) {
        1 -> "ST"
        2 -> "ND"
        3 -> "RD"
        else -> "TH"
    }

    val rankColor = when (rank) {
        1 -> TrophyGold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> TextLight
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // TOP ROW: Rank, Lap, Pause, Connection status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC0D1117))
                    .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Position",
                    tint = rankColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$rank$rankSuffix",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = rankColor
                )
            }

            // Lap Counter Pill
            val isFinalLap = currentLap == totalLaps
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isFinalLap) RacingRedDark else CardAsphalt)
                    .border(1.dp, if (isFinalLap) RacingRed else BorderSlate, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isFinalLap) "FINAL LAP" else "LAP $currentLap / $totalLaps",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = TextLight,
                    letterSpacing = 1.sp
                )
            }

            // Right side: Network indicator & Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Network badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xBB161B22))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isMultiplayer) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Network",
                        tint = if (isMultiplayer) NeonLime else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMultiplayer) "${pingMs}ms" else "SOLO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMultiplayer) NeonLime else TextMuted
                    )
                }

                // Pause Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CardAsphalt)
                        .border(1.dp, BorderSlate, CircleShape)
                        .clickable { onPauseClick() }
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = TextLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECOND ROW: Speedometer & Lap Times
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Speedometer
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0D1117))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$speedKmh",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = speedColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "KM/H",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            // Lap Time readouts
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0D1117))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "TIME ${formatLapTime(lapTimeMs)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextLight
                )
                if (bestLapMs > 0L) {
                    Text(
                        text = "BEST ${formatLapTime(bestLapMs)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}

fun formatLapTime(timeMs: Long): String {
    val totalSec = timeMs / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val tenths = (timeMs % 1000) / 100
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
