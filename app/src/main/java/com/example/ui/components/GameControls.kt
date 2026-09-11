package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.haptics.HapticController
import com.example.ui.theme.*

@Composable
fun GameControls(
    nitroAmount: Float,
    isBoosting: Boolean,
    onSteerChange: (Float) -> Unit,
    onThrottleChange: (Float) -> Unit,
    onDriftChange: (Boolean) -> Unit,
    onNitroChange: (Boolean) -> Unit,
    hapticController: HapticController,
    modifier: Modifier = Modifier
) {
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }
    var gasPressed by remember { mutableStateOf(false) }
    var brakePressed by remember { mutableStateOf(false) }
    var driftPressed by remember { mutableStateOf(false) }
    var nitroPressed by remember { mutableStateOf(false) }

    // Update steer whenever buttons change
    LaunchedEffect(leftPressed, rightPressed) {
        val steer = when {
            leftPressed && !rightPressed -> -1f
            rightPressed && !leftPressed -> 1f
            else -> 0f
        }
        onSteerChange(steer)
    }

    // Update throttle whenever pedals change
    LaunchedEffect(gasPressed, brakePressed) {
        val throttle = when {
            gasPressed && !brakePressed -> 1f
            brakePressed && !gasPressed -> -1f
            else -> 0f
        }
        onThrottleChange(throttle)
    }

    // Update drift
    LaunchedEffect(driftPressed) {
        onDriftChange(driftPressed)
        if (driftPressed) hapticController.vibrateDrift()
    }

    // Update nitro
    LaunchedEffect(nitroPressed) {
        onNitroChange(nitroPressed)
        if (nitroPressed) hapticController.vibrateNitro()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // --- LEFT SIDE: STEERING BUTTONS ---
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Steering Left
            HoldButton(
                isPressed = leftPressed,
                onPressChange = { pressed ->
                    leftPressed = pressed
                    if (pressed) hapticController.vibrateTap()
                },
                testTag = "steer_left_button",
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Steer Left",
                    tint = if (leftPressed) Color.Black else TextLight,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Steering Right
            HoldButton(
                isPressed = rightPressed,
                onPressChange = { pressed ->
                    rightPressed = pressed
                    if (pressed) hapticController.vibrateTap()
                },
                testTag = "steer_right_button",
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Steer Right",
                    tint = if (rightPressed) Color.Black else TextLight,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // --- RIGHT SIDE: ACTIONS & PEDALS ---
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Drift / Handbrake Button
            HoldButton(
                isPressed = driftPressed,
                onPressChange = { pressed -> driftPressed = pressed },
                activeColor = NeonCyan,
                idleColor = CardAsphalt,
                testTag = "drift_button",
                modifier = Modifier.size(62.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Toys,
                        contentDescription = "Drift Handbrake",
                        tint = if (driftPressed) Color.Black else NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "DRIFT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (driftPressed) Color.Black else NeonCyan
                    )
                }
            }

            // Nitro Boost Button
            val canNitro = nitroAmount > 0.05f
            HoldButton(
                isPressed = nitroPressed && canNitro,
                onPressChange = { pressed -> nitroPressed = pressed },
                activeColor = TurboOrange,
                idleColor = if (canNitro) CardAsphalt else Color(0xFF1E232A),
                testTag = "nitro_button",
                modifier = Modifier.size(64.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Nitro Boost",
                        tint = if (nitroPressed && canNitro) Color.Black else if (canNitro) TurboOrange else TextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "${(nitroAmount * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (nitroPressed && canNitro) Color.Black else if (canNitro) TurboOrange else TextMuted
                    )
                }
            }

            // Brake / Reverse Pedal
            PedalButton(
                label = "BRAKE",
                subLabel = "REV",
                isPressed = brakePressed,
                activeColor = RacingRed,
                idleColor = CardAsphalt,
                testTag = "brake_pedal",
                onPressChange = { pressed ->
                    brakePressed = pressed
                    if (pressed) hapticController.vibrateTap()
                },
                modifier = Modifier
                    .width(58.dp)
                    .height(95.dp)
            )

            // Gas / Accelerate Pedal
            PedalButton(
                label = "GAS",
                subLabel = "RACE",
                isPressed = gasPressed,
                activeColor = NeonLime,
                idleColor = CardAsphalt,
                testTag = "gas_pedal",
                onPressChange = { pressed ->
                    gasPressed = pressed
                    if (pressed) hapticController.vibrateTap()
                },
                modifier = Modifier
                    .width(66.dp)
                    .height(115.dp)
            )
        }
    }
}

@Composable
fun HoldButton(
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    activeColor: Color = TurboOrange,
    idleColor: Color = CardAsphalt,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1.0f, label = "btnScale")
    val bg = if (isPressed) activeColor else idleColor
    val borderColor = if (isPressed) activeColor else BorderSlate

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(testTag)
            .scale(scale)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, borderColor, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressChange(true)
                    waitForUpOrCancellation()
                    onPressChange(false)
                }
            }
    ) {
        content()
    }
}

@Composable
fun PedalButton(
    label: String,
    subLabel: String,
    isPressed: Boolean,
    activeColor: Color,
    idleColor: Color,
    testTag: String,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1.0f, label = "pedalScale")
    val bg = if (isPressed) activeColor else idleColor
    val borderColor = if (isPressed) activeColor else BorderSlate

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(testTag)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressChange(true)
                    waitForUpOrCancellation()
                    onPressChange(false)
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Metallic pedal grooves
            for (i in 0 until 3) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .background(
                            if (isPressed) Color.Black.copy(alpha = 0.35f)
                            else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                        .padding(vertical = 1.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (isPressed) Color.Black else TextLight
            )
            Text(
                text = subLabel,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color.Black.copy(alpha = 0.8f) else TextMuted
            )
        }
    }
}
