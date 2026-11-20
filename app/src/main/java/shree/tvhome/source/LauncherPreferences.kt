package shree.tvhome.source

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class LauncherPreferences(context: Context) {

    data class OptionItem(val key: String, val label: String)
    data class AccentOption(val label: String, val color: Int)

    companion object {
        const val PREFS = "launcher_preferences"
        const val PREF_ACCENT = "accent_color"
        const val PREF_TILE_SIZE = "tile_size"
        const val PREF_TILE_SHAPE = "tile_shape"
        const val PREF_TILE_COLOR = "tile_color"
        const val PREF_BADGE = "icon_badge"
        const val PREF_COLUMNS = "grid_columns"
        const val PREF_PANEL_WIDTH = "panel_width_pct"
        const val PREF_WALLPAPER = "custom_wallpaper_uri"
        const val PREF_FAVORITES = "favorites"
        const val PREF_FAVORITES_ORDER = "favorites_ordered_list"
        const val PREF_HIDDEN_APPS = "hidden_apps_set"
        const val PREF_USER_NAME = "launcher_user_name"
        const val PREF_GREETING_PHRASE = "launcher_greeting_phrase"

        const val MOTION_ELASTIC = "elastic"
        const val MOTION_JELLY = "jelly"
        const val MOTION_FLIP = "flip"
        const val MOTION_BOOK = "book"
        const val MOTION_FLOAT = "float"
        const val MOTION_SILK = "silk"
        const val MOTION_SWING = "swing"

        val MOTION_OPTIONS = listOf(
            OptionItem(MOTION_ELASTIC, "Elastic"),
            OptionItem(MOTION_JELLY, "Jelly"),
            OptionItem(MOTION_FLIP, "3D Flip"),
            OptionItem(MOTION_BOOK, "Book Open"),
            OptionItem(MOTION_FLOAT, "Levitate"),
            OptionItem(MOTION_SILK, "Silk Glide"),
            OptionItem(MOTION_SWING, "Swing")
        )

        val TILE_SIZE_OPTIONS = listOf(
            OptionItem("small", "Small"),
            OptionItem("medium", "Medium"),
            OptionItem("large", "Large")
        )

        val PANEL_WIDTH_OPTIONS = listOf(
            OptionItem("20", "20%"),
            OptionItem("25", "25%"),
            OptionItem("30", "30%"),
            OptionItem("35", "35%")
        )

        val TILE_SHAPE_OPTIONS = listOf(
            OptionItem("squircle", "◐ Squircle"),
            OptionItem("circle", "● Circle"),
            OptionItem("heart", "♥ Heart"),
            OptionItem("classic", "■ Classic")
        )

        val BADGE_OPTIONS = listOf(
            OptionItem("accent", "● Dot"),
            OptionItem("heart", "♥ Heart"),
            OptionItem("star", "★ Star"),
            OptionItem("none", "Off")
        )

        val ACCENT_OPTIONS = listOf(
            AccentOption("Sky", Color.rgb(92, 184, 255)),
            AccentOption("Mint", Color.rgb(46, 232, 138)),
            AccentOption("Rose", Color.rgb(255, 110, 145)),
            AccentOption("Violet", Color.rgb(185, 135, 255)),
            AccentOption("Amber", Color.rgb(255, 195, 75)),
            AccentOption("Aqua", Color.rgb(75, 235, 245))
        )
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var accentColor: Int
        get() = prefs.getInt(PREF_ACCENT, ACCENT_OPTIONS.first().color)
        set(value) = prefs.edit().putInt(PREF_ACCENT, value).apply()

    var tileSize: String
        get() = prefs.getString(PREF_TILE_SIZE, TILE_SIZE_OPTIONS.getOrNull(1)?.key ?: "medium") ?: "medium"
        set(value) = prefs.edit().putString(PREF_TILE_SIZE, value).apply()

    var tileShape: String
        get() = prefs.getString(PREF_TILE_SHAPE, TILE_SHAPE_OPTIONS.first().key) ?: "squircle"
        set(value) = prefs.edit().putString(PREF_TILE_SHAPE, value).apply()

    var tileColor: String
        get() = prefs.getString(PREF_TILE_COLOR, TileThemePalette.KEYS.first()) ?: TileThemePalette.KEYS.first()
        set(value) = prefs.edit().putString(PREF_TILE_COLOR, value).apply()

    var badgeMode: String
        get() = prefs.getString(PREF_BADGE, BADGE_OPTIONS.getOrNull(2)?.key ?: "star") ?: "star"
        set(value) = prefs.edit().putString(PREF_BADGE, value).apply()

    var columns: Int
        get() = prefs.getInt(PREF_COLUMNS, 6).coerceIn(4, 8)
        set(value) = prefs.edit().putInt(PREF_COLUMNS, value.coerceIn(4, 8)).apply()

    var panelWidthPct: Int
        get() = prefs.getInt(PREF_PANEL_WIDTH, 30).coerceIn(20, 40)
        set(value) = prefs.edit().putInt(PREF_PANEL_WIDTH, value.coerceIn(20, 40)).apply()

    var userName: String
        get() = prefs.getString(PREF_USER_NAME, "Shree") ?: "Shree"
        set(value) = prefs.edit().putString(PREF_USER_NAME, value.ifBlank { "Shree" }).apply()

    var greetingPhrase: String
        get() = prefs.getString(PREF_GREETING_PHRASE, "") ?: ""
        set(value) = prefs.edit().putString(PREF_GREETING_PHRASE, value).apply()

    var sortOrder: String
        get() = prefs.getString("sort_order", "az") ?: "az"
        set(value) = prefs.edit().putString("sort_order", value).apply()

    var liveWallpaperMode: String
        get() = prefs.getString("live_wp_mode", "smart") ?: "smart"
        set(value) = prefs.edit().putString("live_wp_mode", value).apply()

    var photoWeatherAura: Boolean
        get() = prefs.getBoolean("photo_weather_aura", true)
        set(value) = prefs.edit().putBoolean("photo_weather_aura", value).apply()

    var cachedWeatherCode: Int
        get() = prefs.getInt("cached_weather_code", 0)
        set(value) = prefs.edit().putInt("cached_weather_code", value).apply()

    var motionStyle: String
        get() = prefs.getString("motion_style", MOTION_OPTIONS.first().key) ?: MOTION_ELASTIC
        set(value) = prefs.edit().putString("motion_style", value).apply()

    var weatherCity: String
        get() = prefs.getString("weather_city", "") ?: ""
        set(value) = prefs.edit().putString("weather_city", value).apply()

    fun getOrderedFavorites(): MutableList<String> {
        val raw = prefs.getString(PREF_FAVORITES_ORDER, null)
        if (!raw.isNullOrEmpty()) {
            return raw.split(",").filter { it.isNotBlank() }.toMutableList()
        }
        val legacy = prefs.getStringSet(PREF_FAVORITES, emptySet()) ?: emptySet()
        return legacy.toMutableList()
    }

    fun getFavorites(): Set<String> = getOrderedFavorites().toSet()

    fun setFavorites(favs: Collection<String>) {
        val list = favs.distinct()
        prefs.edit()
            .putString(PREF_FAVORITES_ORDER, list.joinToString(","))
            .putStringSet(PREF_FAVORITES, list.toSet())
            .apply()
    }

    fun moveFavorite(pkg: String, delta: Int): Boolean {
        val list = getOrderedFavorites()
        val idx = list.indexOf(pkg)
        val target = idx + delta
        if (idx < 0 || target !in list.indices) return false
        list.removeAt(idx)
        list.add(target, pkg)
        setFavorites(list)
        return true
    }

    fun getHiddenApps(): MutableSet<String> {
        return HashSet(prefs.getStringSet(PREF_HIDDEN_APPS, emptySet()) ?: emptySet())
    }

    fun setHiddenApps(hidden: Set<String>) {
        prefs.edit().putStringSet(PREF_HIDDEN_APPS, HashSet(hidden)).apply()
    }
}
