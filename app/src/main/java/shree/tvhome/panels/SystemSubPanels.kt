package shree.tvhome.panels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.SystemCapabilities
import java.util.Locale

object SystemSubPanels {

    fun buildBluetoothPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        addHeader(ctx, panel, lp.accentColor, "BLUETOOTH & CONNECTED DEVICES")
        val btAdapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        val isEnabled = btAdapter?.isEnabled == true
        addCard(
            ctx, panel, lp, "🔵",
            if (isEnabled) "Bluetooth is ON" else "Bluetooth is OFF",
            if (isEnabled) "Discovering paired devices" else "Enable Bluetooth or open TV settings",
            Color.rgb(120, 155, 255)
        ) {
            try {
                if (!isEnabled) {
                    @Suppress("DEPRECATION")
                    btAdapter?.enable()
                }
                ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            } catch (_: Exception) {}
        }

        val bonded = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    btAdapter?.bondedDevices?.toList().orEmpty()
                } else emptyList()
            } else {
                btAdapter?.bondedDevices?.toList().orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (bonded.isEmpty()) {
            panel.addView(TextView(ctx).apply {
                text = "No paired Bluetooth devices reported."
                textSize = 9.5f
                setTextColor(Color.argb(190, 210, 225, 245))
                setPadding(dp(ctx, 4), dp(ctx, 4), dp(ctx, 4), dp(ctx, 8))
            })
        } else {
            for (dev in bonded) {
                val devName = try { dev.name ?: dev.address } catch (_: Exception) { "Bluetooth Device" }
                addCard(ctx, panel, lp, "🎧", devName, dev.address, Color.rgb(75, 235, 245)) {
                    Toast.makeText(ctx, devName, Toast.LENGTH_SHORT).show()
                }
            }
        }

        SystemCapabilities.getAvailableSettings(ctx, setOf(Settings.ACTION_BLUETOOTH_SETTINGS)).forEach { item ->
            addCard(ctx, panel, lp, item.icon, item.title, item.subtitle, Color.rgb(92, 184, 255)) {
                try { ctx.startActivity(item.intent) } catch (_: Exception) {}
            }
        }

        addBackButton(ctx, panel, lp, onBack)
    }

    fun buildStorageAndAppsPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        addHeader(ctx, panel, lp.accentColor, "INTERNAL STORAGE")
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val usedPct = if (totalBytes > 0L) ((usedBytes * 100f) / totalBytes).toInt().coerceIn(0, 100) else 0
            val freeGb = String.format(Locale.US, "%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0))
            val totalGb = String.format(Locale.US, "%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))

            addCard(
                ctx, panel, lp, "💾",
                "$usedPct% Used • $freeGb Free",
                "Total storage: $totalGb",
                Color.rgb(185, 135, 255)
            ) {
                try { ctx.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            addCard(ctx, panel, lp, "💾", "Storage Info", "Manage device storage & apps", Color.rgb(185, 135, 255)) {
                try { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
            }
        }

        addHeader(ctx, panel, lp.accentColor, "INSTALLED APPLICATIONS")
        val appCount = LauncherUtils.queryLaunchableApps(ctx.packageManager).size
        addCard(ctx, panel, lp, "📦", "$appCount Launchable Apps", "Open system app manager", Color.rgb(46, 232, 138)) {
            try { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) } catch (_: Exception) {}
        }

        SystemCapabilities.getAvailableSettings(ctx, setOf(Settings.ACTION_APPLICATION_SETTINGS, Settings.ACTION_SETTINGS)).forEach { item ->
            addCard(ctx, panel, lp, item.icon, item.title, item.subtitle, Color.rgb(92, 184, 255)) {
                try { ctx.startActivity(item.intent) } catch (_: Exception) {}
            }
        }

        addBackButton(ctx, panel, lp, onBack)
    }

    fun buildDisplayTimeoutAndSystemOptions(ctx: Context, panel: LinearLayout, lp: LauncherPreferences) {
        addHeader(ctx, panel, lp.accentColor, "SYSTEM DISPLAY & POWER SHORTCUTS")
        val curTimeoutMs = try {
            Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000)
        } catch (_: Exception) {
            60000
        }
        val curTimeoutSec = (curTimeoutMs / 1000).coerceAtLeast(1)
        panel.addView(TextView(ctx).apply {
            text = "Current Sleep Timeout: ${curTimeoutSec}s"
            textSize = 9.5f
            setTextColor(Color.argb(200, 215, 232, 250))
            setPadding(dp(ctx, 2), 0, 0, dp(ctx, 6))
        })

        SystemCapabilities.getAvailableSettings(
            ctx,
            setOf(Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SOUND_SETTINGS, Settings.ACTION_SETTINGS)
        ).forEach { item ->
            addCard(ctx, panel, lp, item.icon, item.title, item.subtitle, Color.rgb(255, 195, 75)) {
                try { ctx.startActivity(item.intent) } catch (_: Exception) {}
            }
        }
    }

    private fun addCard(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        icon: String,
        title: String,
        sub: String,
        tint: Int,
        onClick: () -> Unit
    ) {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
            isFocusable = true
            isClickable = true
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(LauncherUtils.withAlpha(tint, 72), Color.argb(36, 20, 30, 66))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
                setStroke(dp(ctx, 1), LauncherUtils.withAlpha(tint, 130))
            }
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 3f) }
            setOnClickListener { onClick() }
        }
        card.addView(TextView(ctx).apply { text = icon; textSize = 14f; setTextColor(tint); setPadding(0, 0, dp(ctx, 10), 0) })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(TextView(ctx).apply { text = title; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        mid.addView(TextView(ctx).apply { text = sub; textSize = 8.5f; setTextColor(Color.argb(195, 215, 232, 250)) })
        card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(TextView(ctx).apply { text = "›"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
    }

    private fun addBackButton(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        val btn = TextView(ctx).apply {
            text = "‹  Back"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(LauncherUtils.withAlpha(lp.accentColor, 215), LauncherUtils.withAlpha(lp.accentColor, 135))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 12f)
                setStroke(dp(ctx, 2), Color.WHITE)
            }
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f) }
            setOnClickListener { onBack() }
        }
        panel.addView(btn, LinearLayout.LayoutParams(-1, dp(ctx, 38)).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 16) })
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
