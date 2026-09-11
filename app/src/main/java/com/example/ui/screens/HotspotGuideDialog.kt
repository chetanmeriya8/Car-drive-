package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun HotspotGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardAsphalt),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSlate, RoundedCornerShape(20.dp))
                .testTag("hotspot_guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = TurboOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hotspot Multiplayer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextLight
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_guide_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Play together anywhere without internet or mobile data!",
                    fontSize = 12.sp,
                    color = NeonCyan,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                GuideStep(
                    number = 1,
                    icon = Icons.Default.PhoneAndroid,
                    title = "Turn on Personal Hotspot",
                    description = "One player enables 'Portable Hotspot' in their phone's Android Settings."
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideStep(
                    number = 2,
                    icon = Icons.Default.Wifi,
                    title = "Friends Connect to Hotspot",
                    description = "Other players open Wi-Fi settings and connect to that phone's hotspot."
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideStep(
                    number = 3,
                    icon = Icons.Default.SportsScore,
                    title = "Host & Join In 1-Tap",
                    description = "The hotspot owner taps 'Host Game'. Friends tap 'Join Game' — auto-discovery finds the lobby instantly!"
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideStep(
                    number = 4,
                    icon = Icons.Default.Bolt,
                    title = "Ultra Low-Latency Sync",
                    description = "Multiplayer runs over local UDP sockets directly on the device with 2ms-8ms ultra-fast response."
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TurboOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("got_it_button")
                ) {
                    Text("Got It! Let's Race", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun GuideStep(
    number: Int,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkAsphalt)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF21262D))
        ) {
            Text(
                text = "$number",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = TurboOrange
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextLight
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )
        }
    }
}
