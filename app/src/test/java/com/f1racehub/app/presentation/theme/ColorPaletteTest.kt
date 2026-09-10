package com.f1racehub.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColorPaletteTest {

    private fun Color.toHexRgb(): String {
        val argb = (this.value.toLong() ushr 32) and 0xFFFFFFFFL
        val rgb = (argb and 0x00FFFFFFL).toInt()
        return String.format("#%06X", rgb)
    }

    @Test
    fun `f1Background matches expected hex and dark luminance constraints`() {
        assertEquals("#101014", F1Background.toHexRgb())
        assertEquals(1.0f, F1Background.alpha, 0.0001f)
        assertEquals(0.0053f, F1Background.luminance(), 0.001f)
        assertTrue(F1Background.luminance() < 0.01f)
    }

    @Test
    fun `f1Surface and f1SurfaceBorder match expected hex values and surface hierarchy`() {
        assertEquals("#1B1B22", F1Surface.toHexRgb())
        assertEquals("#2C2C38", F1SurfaceBorder.toHexRgb())
        assertEquals(1.0f, F1Surface.alpha, 0.0001f)
        assertEquals(1.0f, F1SurfaceBorder.alpha, 0.0001f)

        assertEquals(0.0113f, F1Surface.luminance(), 0.001f)
        assertEquals(0.0262f, F1SurfaceBorder.luminance(), 0.001f)

        // Verifies stepped surface elevation hierarchy
        assertTrue(F1Background.luminance() < F1Surface.luminance())
        assertTrue(F1Surface.luminance() < F1SurfaceBorder.luminance())
    }

    @Test
    fun `f1RedPrimary matches official Formula 1 red hex code and luminance`() {
        assertEquals("#E10600", F1RedPrimary.toHexRgb())
        assertEquals(1.0f, F1RedPrimary.alpha, 0.0001f)
        assertEquals(0.1614f, F1RedPrimary.luminance(), 0.001f)
    }

    @Test
    fun `f1TextWhite and f1TextMuted match specifications and high contrast ratio`() {
        assertEquals("#FFFFFF", F1TextWhite.toHexRgb())
        assertEquals("#9E9EA8", F1TextMuted.toHexRgb())
        assertEquals(1.0f, F1TextWhite.alpha, 0.0001f)
        assertEquals(1.0f, F1TextMuted.alpha, 0.0001f)

        assertEquals(1.0f, F1TextWhite.luminance(), 0.0001f)
        assertEquals(0.3455f, F1TextMuted.luminance(), 0.001f)

        // WCAG contrast check: (L1 + 0.05) / (L2 + 0.05) >= 15 for white text on F1 background
        val contrastRatio = (F1TextWhite.luminance() + 0.05f) / (F1Background.luminance() + 0.05f)
        assertTrue(contrastRatio >= 15.0f, "Contrast ratio must exceed 15:1")
    }

    @Test
    fun `pirelli tyre compound colors match official FIA color palette`() {
        assertEquals("#E8002D", TyreSoft.toHexRgb())
        assertEquals("#FFF500", TyreMedium.toHexRgb())
        assertEquals("#FFFFFF", TyreHard.toHexRgb())
        assertEquals("#39B54A", TyreInter.toHexRgb())
        assertEquals("#00A3E0", TyreWet.toHexRgb())

        assertEquals(0.1735f, TyreSoft.luminance(), 0.001f)
        assertEquals(0.8656f, TyreMedium.luminance(), 0.001f)
        assertEquals(1.0000f, TyreHard.luminance(), 0.001f)
        assertEquals(0.3441f, TyreInter.luminance(), 0.001f)
        assertEquals(0.3158f, TyreWet.luminance(), 0.001f)
    }

    @Test
    fun `pirelli tyre compound tokens are all visually distinct`() {
        val tyreColors = listOf(TyreSoft, TyreMedium, TyreHard, TyreInter, TyreWet)
        val distinctHexes = tyreColors.map { it.toHexRgb() }.toSet()
        assertEquals(5, distinctHexes.size, "All five tyre compounds must map to distinct hex colors")
    }

    @Test
    fun `race control flag tokens match FIA signaling color specifications`() {
        assertEquals("#00D2BE", FlagGreen.toHexRgb())
        assertEquals("#FFE600", FlagYellow.toHexRgb())
        assertEquals("#E10600", FlagRed.toHexRgb())

        assertEquals(0.4981f, FlagGreen.luminance(), 0.001f)
        assertEquals(0.7785f, FlagYellow.luminance(), 0.001f)
        assertEquals(0.1614f, FlagRed.luminance(), 0.001f)

        assertNotEquals(FlagGreen, FlagYellow)
        assertNotEquals(FlagYellow, FlagRed)
        assertEquals(F1RedPrimary, FlagRed)
    }

    @Test
    fun `darkColorScheme incorporates expected F1 design tokens`() {
        val themeClass = Class.forName("com.f1racehub.app.presentation.theme.ThemeKt")
        val field = themeClass.getDeclaredField("DarkColorScheme")
        field.isAccessible = true
        val colorScheme = field.get(null) as ColorScheme

        assertEquals(F1RedPrimary, colorScheme.primary)
        assertEquals(F1Background, colorScheme.background)
        assertEquals(F1Surface, colorScheme.surface)
        assertEquals(F1TextWhite, colorScheme.onPrimary)
        assertEquals(F1TextWhite, colorScheme.onBackground)
        assertEquals(F1TextWhite, colorScheme.onSurface)
    }
}
