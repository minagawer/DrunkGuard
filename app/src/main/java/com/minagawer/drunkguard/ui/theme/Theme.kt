package com.minagawer.drunkguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Background layers ────────────────────────────────────────────────────────
val BgBase = Color(0xFF07070F)   // deepest background
val BgSurface = Color(0xFF0C0C1A)   // screen background
val BgCard = Color(0xFF131325)   // card / container
val BgCardHigh = Color(0xFF1A1A32)   // elevated card
val BgBorder = Color(0xFF25253F)   // subtle border

// ── Green — active / success ─────────────────────────────────────────────────
val GreenActive = Color(0xFF22C55E)
val GreenDim = Color(0xFF0A2914)
val GreenBorder = Color(0xFF1A5E2E)
val GreenLight = Color(0xFF86EFAC)

// ── Red — danger ─────────────────────────────────────────────────────────────
val RedDanger = Color(0xFFF87171)
val RedDark = Color(0xFF991B1B)
val RedDim = Color(0xFF1A0707)
val RedBorder = Color(0xFF5C1A1A)

// ── Amber — warning ──────────────────────────────────────────────────────────
val OrangeWarn = Color(0xFFFBBF24)
val OrangeDim = Color(0xFF1F1508)
val OrangeBorder = Color(0xFF5C3A12)

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFF0EFFF)
val TextSecondary = Color(0xFF8E8EB8)
val TextMuted = Color(0xFF55557A)

private val DrunkGuardColorScheme = darkColorScheme(
    primary = GreenActive,
    onPrimary = Color.White,
    primaryContainer = GreenDim,
    onPrimaryContainer = TextPrimary,
    secondary = OrangeWarn,
    background = BgSurface,
    surface = BgCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = RedDanger,
    outline = BgBorder,
)

@Composable
fun DrunkGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DrunkGuardColorScheme,
        content = content
    )
}
