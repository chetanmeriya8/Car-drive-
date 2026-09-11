package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.model.PlayerCar
import com.example.model.Track
import com.example.network.RemoteCarInterpolator
import com.example.physics.CarPhysics

@Composable
fun MiniMap(
    track: Track,
    playerPhysics: CarPhysics,
    remoteCars: Map<String, RemoteCarInterpolator>,
    lobbyPlayers: List<PlayerCar>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0D1117))
            .border(1.dp, Color(0x6630363D), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val padding = 12f
            val mapW = size.width - padding * 2
            val mapH = size.height - padding * 2

            val scaleX = mapW / track.worldWidth
            val scaleY = mapH / track.worldHeight
            val scale = minOf(scaleX, scaleY)

            val offsetX = padding + (mapW - track.worldWidth * scale) * 0.5f
            val offsetY = padding + (mapH - track.worldHeight * scale) * 0.5f

            fun worldToMap(wX: Float, wY: Float): Offset {
                return Offset(offsetX + wX * scale, offsetY + wY * scale)
            }

            // Draw track path
            val waypoints = track.waypoints
            for (i in waypoints.indices) {
                val p1 = worldToMap(waypoints[i].x, waypoints[i].y)
                val p2 = worldToMap(waypoints[(i + 1) % waypoints.size].x, waypoints[(i + 1) % waypoints.size].y)
                drawLine(
                    color = Color(0x88484F58),
                    start = p1,
                    end = p2,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Start line indicator
            val startPt = worldToMap(track.startPosition.x, track.startPosition.y)
            drawCircle(Color.White, 3f, startPt)

            // Draw opponent blips
            for ((pId, remote) in remoteCars) {
                val pInfo = lobbyPlayers.find { it.id == pId }
                val color = Color(pInfo?.colorHex ?: 0xFFFF1744)
                val mPos = worldToMap(remote.currentX, remote.currentY)
                drawCircle(color, 4f, mPos)
            }

            // Draw local player blip
            val playerMapPos = worldToMap(playerPhysics.x, playerPhysics.y)
            drawCircle(Color(0xFF00E5FF), 5.5f, playerMapPos)
            drawCircle(Color(0xFFFF6D00), 3.5f, playerMapPos)
        }
    }
}
