package shree.tvhome.panels

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.TileThemePalette
import java.util.Locale

object WifiPanelBuilder {

    fun build(ctx: Context, panel: LinearLayout, lp: LauncherPreferences) {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val enabled = wm?.isWifiEnabled == true
        val ssid = TileThemePalette.getConnectedSsid(ctx)

        addHeader(ctx, panel, lp.accentColor, "NETWORK STATUS")
        val statusCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            background = cardBg(ctx, lp.accentColor, Color.rgb(92, 184, 255), false)
        }
        statusCard.addView(TextView(ctx).apply {
            text = when {
                !enabled -> "Wi-Fi is OFF"
                ssid != null -> "Connected: $ssid"
                else -> "Wi-Fi is ON • Not connected"
            }
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        if (enabled && wm != null) {
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            val link = info?.linkSpeed ?: -1
            val ipInt = info?.ipAddress ?: 0
            val ipStr = if (ipInt != 0) {
                "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt ushr 24 and 0xff}"
            } else "Unavailable"
            val meta = buildString {
                if (link > 0) append("${link} Mbps")
                if (ipStr != "Unavailable") append(if (isNotEmpty()) " • IP: $ipStr" else "IP: $ipStr")
            }.ifEmpty { "Ready for network scan" }
            statusCard.addView(TextView(ctx).apply {
                text = meta
                textSize = 9f
                setTextColor(Color.argb(195, 210, 230, 252))
                setPadding(0, dp(ctx, 3), 0, 0)
            })
        }
        panel.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        val toggleBtn = TextView(ctx).apply {
            text = if (enabled) "Turn Wi-Fi OFF" else "Turn Wi-Fi ON"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = cardBg(ctx, lp.accentColor, if (enabled) Color.rgb(255, 110, 145) else Color.rgb(46, 232, 138), false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f)
                v.background = cardBg(ctx, lp.accentColor, if (enabled) Color.rgb(255, 110, 145) else Color.rgb(46, 232, 138), f)
            }
            setOnClickListener {
                try {
                    @Suppress("DEPRECATION")
                    wm?.isWifiEnabled = !enabled
                    Toast.makeText(ctx, if (!enabled) "Enabling Wi-Fi..." else "Disabling Wi-Fi...", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    try {
                        ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
        }
        panel.addView(toggleBtn, LinearLayout.LayoutParams(-1, dp(ctx, 38)).apply { bottomMargin = dp(ctx, 8) })

        SystemCapabilities.getAvailableSettings(ctx, setOf(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_SETTINGS)).forEach { item ->
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
                isFocusable = true
                isClickable = true
                background = cardBg(ctx, lp.accentColor, Color.rgb(92, 184, 255), false)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 3f)
                    v.background = cardBg(ctx, lp.accentColor, Color.rgb(92, 184, 255), f)
                }
                setOnClickListener {
                    try { ctx.startActivity(item.intent) } catch (_: Exception) {}
                }
            }
            card.addView(TextView(ctx).apply { text = item.icon; textSize = 13f; setTextColor(Color.WHITE); setPadding(0, 0, dp(ctx, 10), 0) })
            val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            mid.addView(TextView(ctx).apply { text = item.title; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            mid.addView(TextView(ctx).apply { text = item.subtitle; textSize = 8.5f; setTextColor(Color.argb(195, 210, 230, 252)) })
            card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(TextView(ctx).apply { text = "›"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }

        if (enabled && wm != null) {
            addHeader(ctx, panel, lp.accentColor, "DISCOVERED NETWORKS")
            val results = try {
                @Suppress("DEPRECATION")
                wm.scanResults.orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            val unique = results
                .filter { !it.SSID.isNullOrBlank() }
                .distinctBy { it.SSID }
                .take(8)

            if (unique.isEmpty()) {
                panel.addView(TextView(ctx).apply {
                    text = "No nearby SSIDs reported yet. Use Network Settings to connect."
                    textSize = 9.5f
                    setTextColor(Color.argb(190, 210, 225, 245))
                })
            } else {
                for (ap in unique) {
                    val netName = ap.SSID
                    val levelPct = WifiManager.calculateSignalLevel(ap.level, 100).coerceIn(0, 100)
                    val row = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(ctx, 11), dp(ctx, 8), dp(ctx, 11), dp(ctx, 8))
                        isFocusable = true
                        isClickable = true
                        background = cardBg(ctx, lp.accentColor, Color.rgb(75, 235, 245), false)
                        setOnFocusChangeListener { v, f ->
                            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 3f)
                            v.background = cardBg(ctx, lp.accentColor, Color.rgb(75, 235, 245), f)
                        }
                        setOnClickListener {
                            try { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (_: Exception) {}
                        }
                    }
                    row.addView(TextView(ctx).apply {
                        text = netName
                        textSize = 10f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.WHITE)
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                    row.addView(TextView(ctx).apply {
                        text = "$levelPct%"
                        textSize = 9.5f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.rgb(75, 235, 245))
                    })
                    panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 5) })
                }
            }
        }
    }

    private fun cardBg(ctx: Context, accent: Int, tint: Int, focused: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (focused) intArrayOf(LauncherUtils.withAlpha(tint, 155), LauncherUtils.withAlpha(accent, 90))
        else intArrayOf(LauncherUtils.withAlpha(tint, 70), Color.argb(36, 20, 30, 66))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (focused) 2 else 1), if (focused) Color.WHITE else LauncherUtils.withAlpha(tint, 130))
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
