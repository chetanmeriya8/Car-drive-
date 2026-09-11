package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.CarType
import com.example.model.PlayerCar
import com.example.ui.screens.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val samplePlayer = PlayerCar(
      id = "p1",
      name = "ApexRacer",
      carType = CarType.PHANTOM_GT,
      colorHex = 0xFFFF6D00
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        MainMenuScreen(
          playerCar = samplePlayer,
          networkStatus = "Hotspot/WiFi: 192.168.43.1",
          onQuickRace = {},
          onHostGame = {},
          onJoinGame = {},
          onOpenGarage = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

