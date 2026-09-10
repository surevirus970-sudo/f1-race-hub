# Task 8 Brief: Design System & Reusable Components (Jetpack Compose)

## Files to Create
- `app/src/main/java/com/f1racehub/app/presentation/theme/Color.kt`
- `app/src/main/java/com/f1racehub/app/presentation/theme/Theme.kt`
- `app/src/main/java/com/f1racehub/app/presentation/components/F1PirelliTyreBadge.kt`
- `app/src/main/java/com/f1racehub/app/presentation/components/RaceControlBanner.kt`
- `app/src/main/java/com/f1racehub/app/presentation/components/CountdownTimerCard.kt`
- `app/src/test/java/com/f1racehub/app/presentation/theme/ColorPaletteTest.kt`

## Interfaces
- Consumes: Jetpack Compose Material 3 (`androidx.compose.material3.*`, `androidx.compose.ui.graphics.Color`).
- Produces:
  - F1 Design System tokens: `F1Background`, `F1Surface`, `F1SurfaceBorder`, `F1RedPrimary`, `F1TextWhite`, `F1TextMuted`
  - Pirelli Tyre colors: `TyreSoft`, `TyreMedium`, `TyreHard`, `TyreInter`, `TyreWet`
  - Flag colors: `FlagGreen`, `FlagYellow`, `FlagRed`
  - Theme: `@Composable fun F1Theme(content: @Composable () -> Unit)`
  - Components:
    - `@Composable fun F1PirelliTyreBadge(compound: String, lapsUsed: Int, modifier: Modifier = Modifier)`
    - `@Composable fun RaceControlBanner(status: String, message: String, modifier: Modifier = Modifier)`
    - `@Composable fun CountdownTimerCard(targetTime: java.time.Instant, sessionName: String, modifier: Modifier = Modifier)`

## Implementation Details

### 1. `Color.kt`
```kotlin
package com.f1racehub.app.presentation.theme

import androidx.compose.ui.graphics.Color

val F1Background = Color(0xFF101014)
val F1Surface = Color(0xFF1B1B22)
val F1SurfaceBorder = Color(0xFF2C2C38)
val F1RedPrimary = Color(0xFFE10600)
val F1TextWhite = Color(0xFFFFFFFF)
val F1TextMuted = Color(0xFF9E9EA8)

// Pirelli Tyre Compounds
val TyreSoft = Color(0xFFE8002D)
val TyreMedium = Color(0xFFFFF500)
val TyreHard = Color(0xFFFFFFFF)
val TyreInter = Color(0xFF39B54A)
val TyreWet = Color(0xFF00A3E0)

// Race Control Flags
val FlagGreen = Color(0xFF00D2BE)
val FlagYellow = Color(0xFFFFE600)
val FlagRed = Color(0xFFE10600)
```

### 2. `Theme.kt`
```kotlin
package com.f1racehub.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = F1RedPrimary,
    background = F1Background,
    surface = F1Surface,
    onPrimary = F1TextWhite,
    onBackground = F1TextWhite,
    onSurface = F1TextWhite
)

@Composable
fun F1Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
```

### 3. `F1PirelliTyreBadge.kt`
```kotlin
package com.f1racehub.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.presentation.theme.*

@Composable
fun F1PirelliTyreBadge(compound: String, lapsUsed: Int, modifier: Modifier = Modifier) {
    val (color, letter) = when (compound.uppercase()) {
        "SOFT" -> TyreSoft to "S"
        "MEDIUM" -> TyreMedium to "M"
        "HARD" -> TyreHard to "H"
        "INTERMEDIATE" -> TyreInter to "I"
        "WET" -> TyreWet to "W"
        else -> Color.Gray to "?"
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .background(Color.Black, CircleShape)
                .border(2.dp, color, CircleShape)
        ) {
            Text(text = letter, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "${lapsUsed}L", color = F1TextMuted, fontSize = 12.sp)
    }
}
```

### 4. `RaceControlBanner.kt`
```kotlin
package com.f1racehub.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.presentation.theme.*

@Composable
fun RaceControlBanner(status: String, message: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "GREEN" -> FlagGreen to Color.Black
        "YELLOW", "DOUBLE_YELLOW" -> FlagYellow to Color.Black
        "RED" -> FlagRed to Color.White
        else -> F1Surface to F1TextWhite
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}
```

### 5. `CountdownTimerCard.kt`
```kotlin
package com.f1racehub.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1racehub.app.core.time.CountdownRemaining
import com.f1racehub.app.core.time.DateTimeFormatterUtil
import com.f1racehub.app.presentation.theme.*
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun CountdownTimerCard(targetTime: Instant, sessionName: String, modifier: Modifier = Modifier) {
    var remaining by remember(targetTime) {
        mutableStateOf(DateTimeFormatterUtil.calculateRemainingDuration(targetTime))
    }

    LaunchedEffect(targetTime) {
        while (!remaining.isExpired) {
            delay(1000)
            remaining = DateTimeFormatterUtil.calculateRemainingDuration(targetTime)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = F1Surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, F1SurfaceBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = sessionName.uppercase(), color = F1RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (remaining.isExpired) {
                Text(text = "СЕССИЯ ИДЕТ", color = F1TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeUnitBox(value = remaining.days, label = "ДНЕЙ")
                    TimeUnitBox(value = remaining.hours, label = "ЧАСОВ")
                    TimeUnitBox(value = remaining.minutes, label = "МИН")
                    TimeUnitBox(value = remaining.seconds, label = "СЕК")
                }
            }
        }
    }
}

@Composable
private fun TimeUnitBox(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format("%02d", value),
            color = F1TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, color = F1TextMuted, fontSize = 10.sp)
    }
}
```

### 6. Unit Test `ColorPaletteTest.kt`
Unit test validating that hex codes and luminance values of all tokens strictly match spec definitions (`F1Background = #101014`, `F1RedPrimary = #E10600`, etc.).

### 7. Commit
Commit with message: `feat(ui): implement F1 theme, tyre badges and countdown timer card`
