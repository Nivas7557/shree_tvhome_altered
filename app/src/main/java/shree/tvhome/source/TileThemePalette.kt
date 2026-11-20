package shree.tvhome.source

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import shree.tvhome.design.LauncherUtils

object TileThemePalette {

    data class PaletteSpec(
        val key: String,
        val name: String,
        val subtitle: String,
        val colors: IntArray,
        val topColor: Int,
        val bottomColor: Int
    )

    val PALETTES = listOf(
        PaletteSpec(
            "frost", "Frost Glass", "Translucent icy glass • Live Wi-Fi",
            intArrayOf(Color.argb(68, 220, 240, 255), Color.argb(46, 175, 215, 250), Color.argb(32, 130, 185, 240)),
            Color.argb(85, 235, 248, 255), Color.argb(38, 155, 200, 245)
        ),
        PaletteSpec(
            "ocean", "Deep Ocean", "Oceanic aqua & midnight glass",
            intArrayOf(Color.argb(86, 18, 128, 172), Color.argb(72, 12, 74, 122), Color.argb(64, 9, 36, 74)),
            Color.argb(102, 42, 175, 215), Color.argb(68, 12, 46, 88)
        ),
        PaletteSpec(
            "cyber", "Neon Cyber", "Electric violet, pink & cyan glass",
            intArrayOf(Color.argb(86, 132, 48, 185), Color.argb(72, 185, 42, 122), Color.argb(64, 24, 92, 168)),
            Color.argb(104, 168, 68, 215), Color.argb(68, 26, 72, 148)
        ),
        PaletteSpec(
            "sunset", "Sunset Ember", "Warm coral, amber & rose glass",
            intArrayOf(Color.argb(86, 215, 88, 62), Color.argb(72, 198, 124, 42), Color.argb(64, 128, 36, 88)),
            Color.argb(104, 235, 118, 72), Color.argb(68, 112, 34, 78)
        ),
        PaletteSpec(
            "emerald", "Emerald Mint", "Fresh mint, jade & teal glass",
            intArrayOf(Color.argb(86, 24, 165, 115), Color.argb(72, 16, 118, 112), Color.argb(64, 14, 66, 88)),
            Color.argb(102, 42, 205, 138), Color.argb(68, 14, 58, 78)
        ),
        PaletteSpec(
            "dark", "Charcoal Slate", "Sleek dark slate glass",
            intArrayOf(Color.argb(86, 52, 62, 86), Color.argb(72, 32, 39, 58), Color.argb(64, 18, 22, 36)),
            Color.argb(100, 74, 86, 116), Color.argb(68, 16, 20, 32)
        ),
        PaletteSpec(
            "crystal", "Crystal Prism", "Prismatic sky, lavender & gold glass",
            intArrayOf(Color.argb(86, 92, 184, 255), Color.argb(72, 168, 124, 255), Color.argb(64, 245, 188, 86)),
            Color.argb(104, 128, 204, 255), Color.argb(68, 198, 148, 78)
        ),
        PaletteSpec(
            "obsidian", "Obsidian Gold", "Deep night glass with warm gold tint",
            intArrayOf(Color.argb(88, 28, 32, 48), Color.argb(74, 18, 21, 34), Color.argb(68, 10, 12, 20)),
            Color.argb(102, 62, 58, 46), Color.argb(70, 10, 12, 20)
        )
    )

    val NAMES: List<String> get() = PALETTES.map { it.name }
    val SUBTITLES: List<String> get() = PALETTES.map { it.subtitle }
    val KEYS: List<String> get() = PALETTES.map { it.key }

    private fun specFor(themeKey: String?): PaletteSpec =
        PALETTES.firstOrNull { it.key == themeKey } ?: PALETTES.first()

    fun getColors(themeKey: String?): IntArray = specFor(themeKey).colors

    fun getTopBottomColors(themeKey: String?): Pair<Int, Int> {
        val spec = specFor(themeKey)
        return spec.topColor to spec.bottomColor
    }

    fun getConnectedSsid(ctx: Context): String? {
        return try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wm?.isWifiEnabled != true) return null
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo ?: return null
            val raw = info.ssid ?: return null
            val clean = raw.removePrefix("\"").removeSuffix("\"").trim()
            if (clean.isEmpty() || clean.equals("<unknown ssid>", ignoreCase = true)) null else clean
        } catch (_: Exception) {
            null
        }
    }

    fun buildGlassDrawable(ctx: Context, themeKey: String?, focused: Boolean, accentColor: Int, radiusDp: Float = 20f): GradientDrawable {
        val (topCol, botCol) = getTopBottomColors(themeKey)
        val strokeW = LauncherUtils.dpF(ctx, if (focused) 2.5f else 1.2f).toInt().coerceAtLeast(1)
        val strokeCol = if (focused) Color.WHITE else LauncherUtils.withAlpha(accentColor, 130)
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topCol, botCol)
        ).apply {
            cornerRadius = LauncherUtils.dpF(ctx, radiusDp)
            setStroke(strokeW, strokeCol)
        }
    }
}
