package shree.tvhome.panels

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.design.LauncherBackground
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.source.LauncherPreferences
import java.util.Locale

object WallpaperEditorPanel {

    fun build(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        onWallpaperRefresh: () -> Unit,
        onPickWallpaper: () -> Unit,
        onBack: () -> Unit
    ) {
        val hasCustomPhoto = !lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null).isNullOrEmpty()

        addHeader(ctx, panel, lp.accentColor, "LIVE SCENE & AUTO MODE")
        val modeOptions = listOf(
            LauncherBackground.MODE_SMART to "✨ Smart",
            LauncherBackground.MODE_AUTO_TIME to "🕒 Time"
        ) + LauncherBackground.SCENES.map { it.key to "${it.icon} ${it.name}" }

        modeOptions.chunked(3).forEach { chunk ->
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEachIndexed { idx, (key, lbl) ->
                val active = lp.liveWallpaperMode == key
                val btn = TextView(ctx).apply {
                    text = lbl
                    textSize = 9.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    isFocusable = true
                    isClickable = true
                    background = chipBg(ctx, lp.accentColor, active)
                    setOnFocusChangeListener { v, f ->
                        MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 3f)
                        v.background = chipBg(ctx, lp.accentColor, f || lp.liveWallpaperMode == key)
                    }
                    setOnClickListener {
                        lp.liveWallpaperMode = key
                        onWallpaperRefresh()
                        Toast.makeText(ctx, "Scene: $lbl", Toast.LENGTH_SHORT).show()
                    }
                }
                row.addView(btn, LinearLayout.LayoutParams(0, dp(ctx, 34), 1f).apply {
                    if (idx < chunk.size - 1) rightMargin = dp(ctx, 5)
                })
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 5) })
        }

        addHeader(ctx, panel, lp.accentColor, "CUSTOM GALLERY PHOTO")
        val pickBtn = TextView(ctx).apply {
            text = if (hasCustomPhoto) "🖼  Change Gallery Wallpaper" else "🖼  Choose Photo from Gallery"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = chipBg(ctx, lp.accentColor, true)
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f) }
            setOnClickListener { onPickWallpaper() }
        }
        panel.addView(pickBtn, LinearLayout.LayoutParams(-1, dp(ctx, 38)).apply { bottomMargin = dp(ctx, 6) })

        if (hasCustomPhoto) {
            val auraToggle = TextView(ctx).apply {
                text = if (lp.photoWeatherAura) "🌧  Weather Aura on Photo: ON" else "🌧  Weather Aura on Photo: OFF"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                background = chipBg(ctx, lp.accentColor, lp.photoWeatherAura)
                setOnClickListener {
                    lp.photoWeatherAura = !lp.photoWeatherAura
                    text = if (lp.photoWeatherAura) "🌧  Weather Aura on Photo: ON" else "🌧  Weather Aura on Photo: OFF"
                    background = chipBg(ctx, lp.accentColor, lp.photoWeatherAura)
                    onWallpaperRefresh()
                }
            }
            panel.addView(auraToggle, LinearLayout.LayoutParams(-1, dp(ctx, 36)).apply { bottomMargin = dp(ctx, 6) })

            val curZoom = lp.prefs.getFloat("wp_zoom", 1.08f)
            val zoomTv = TextView(ctx).apply {
                text = "Photo Zoom: ${(curZoom * 100).toInt()}%"
                textSize = 9.5f
                setTextColor(Color.WHITE)
            }
            panel.addView(zoomTv)
            panel.addView(SeekBar(ctx).apply {
                max = 140
                progress = ((curZoom - 0.6f) * 100f).toInt().coerceIn(0, 140)
                isFocusable = true
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val z = (0.6f + p / 100f).coerceIn(0.6f, 2.0f)
                        zoomTv.text = "Photo Zoom: ${(z * 100).toInt()}%"
                        if (fromUser) {
                            lp.prefs.edit().putFloat("wp_zoom", z).apply()
                            onWallpaperRefresh()
                        }
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

            val clearBtn = TextView(ctx).apply {
                text = "🗑  Remove Photo (Use Live Canvas)"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                background = chipBg(ctx, Color.rgb(255, 110, 145), false)
                setOnClickListener {
                    lp.prefs.edit().remove(LauncherPreferences.PREF_WALLPAPER).apply()
                    onWallpaperRefresh()
                }
            }
            panel.addView(clearBtn, LinearLayout.LayoutParams(-1, dp(ctx, 36)).apply { bottomMargin = dp(ctx, 8) })
        }

        val backBtn = TextView(ctx).apply {
            text = "‹  Back"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = chipBg(ctx, lp.accentColor, true)
            setOnClickListener { onBack() }
        }
        panel.addView(backBtn, LinearLayout.LayoutParams(-1, dp(ctx, 38)).apply { topMargin = dp(ctx, 6) })
    }

    private fun chipBg(ctx: Context, accent: Int, active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(accent, 215), LauncherUtils.withAlpha(accent, 135))
        else intArrayOf(Color.argb(62, 255, 255, 255), Color.argb(30, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 12f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) Color.WHITE else LauncherUtils.withAlpha(accent, 150))
    }

    private fun addHeader(ctx: Context, panel: LinearLayout, accent: Int, label: String) {
        panel.addView(TextView(ctx).apply {
            text = label.uppercase(Locale.ROOT)
            textSize = 8.8f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 6); bottomMargin = dp(ctx, 4) })
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
