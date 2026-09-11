package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Speed
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
import com.example.model.CarColor
import com.example.model.CarType
import com.example.model.PlayerCar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    currentCar: PlayerCar,
    onSaveCar: (PlayerCar) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(currentCar.carType) }
    var selectedColorHex by remember { mutableStateOf(currentCar.colorHex) }
    var playerName by remember { mutableStateOf(currentCar.name) }
    var isEditingName by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GARAGE & TUNING", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("garage_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
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
                Button(
                    onClick = {
                        onSaveCar(
                            currentCar.copy(
                                name = playerName.ifBlank { "Racer" },
                                carType = selectedType,
                                colorHex = selectedColorHex
                            )
                        )
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurboOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_car_button")
                ) {
                    Text("APPLY & SAVE CAR", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player Name Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RACER NICKNAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { if (it.length <= 14) playerName = it },
                        singleLine = true,
                        placeholder = { Text("Enter nickname...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSlate,
                            cursorColor = TurboOrange
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("racer_name_field")
                    )
                }
            }

            // Car Model Selector
            Text("SELECT VEHICLE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CarType.values().forEach { car ->
                    val isSelected = car == selectedType
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) TurboOrange else CardAsphalt)
                            .border(1.dp, if (isSelected) TurboOrange else BorderSlate, RoundedCornerShape(10.dp))
                            .clickable { selectedType = car }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("car_type_${car.name.lowercase()}")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DriveEta,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else TextLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = car.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.Black else TextLight,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Car Stats Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardAsphalt),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(selectedType.displayName, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TurboOrange)
                    Text(selectedType.description, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 12.dp))

                    StatBar("Top Speed", selectedType.topSpeed / 540f, TurboOrange)
                    StatBar("Acceleration", selectedType.acceleration / 340f, NeonCyan)
                    StatBar("Handling & Drift", selectedType.driftFactor, NeonLime)
                    StatBar("Nitro Thruster", (selectedType.boostPower - 1.2f) / 0.5f, TrophyGold)
                }
            }

            // Color Palette Picker
            Text("PAINT JOB & LIVERY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CarColor.values().forEach { c ->
                    val isColorSelected = c.hex == selectedColorHex
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(c.composeColor)
                            .border(
                                width = if (isColorSelected) 3.dp else 1.dp,
                                color = if (isColorSelected) Color.White else Color(0x66000000),
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = c.hex }
                            .testTag("color_${c.name.lowercase()}")
                    ) {
                        if (isColorSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, progress: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextLight)
            Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = color,
            trackColor = Color(0xFF21262D),
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}
