package shree.tvhome.panels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.LauncherPreferences
import shree.tvhome.LauncherUtils
import shree.tvhome.MotionEngine
import java.util.Locale

object SystemSubPanels {

    private data class SystemIntentOption(
        val icon: String,
        val title: String,
        val subtitle: String,
        val intent: Intent
    )

    private fun resolveAvailableSystemIntents(ctx: Context): List<SystemIntentOption> {
        val pm = ctx.packageManager
        val candidates = listOf(
            SystemIntentOption("⚙", "TV Settings", "System & device preferences", Intent(Settings.ACTION_SETTINGS)),
            SystemIntentOption("📺", "Display Settings", "Screen & resolution options", Intent(Settings.ACTION_DISPLAY_SETTINGS)),
            SystemIntentOption("🔊", "Sound Settings", "Audio & volume preferences", Intent(Settings.ACTION_SOUND_SETTINGS)),
            SystemIntentOption("📶", "Network Settings", "Wi-Fi & connectivity", Intent(Settings.ACTION_WIFI_SETTINGS)),
            SystemIntentOption("🔵", "Bluetooth Settings", "Pair or manage devices", Intent(Settings.ACTION_BLUETOOTH_SETTINGS)),
            SystemIntentOption("📦", "All Apps Settings", "Manage installed applications", Intent(Settings.ACTION_APPLICATION_SETTINGS)),
            SystemIntentOption("🔔", "Notification Access", "Listener & alert permissions", Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        )
        return candidates.filter { opt ->
            opt.intent.resolveActivity(pm) != null
        }
    }

    @SuppressLint("MissingPermission")
    fun buildBluetoothPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()

        if (adapter == null) {
            panel.addView(text(ctx, "Bluetooth hardware not reported by this device.", 10.5f, Color.LTGRAY, false))
            resolveAvailableSystemIntents(ctx)
                .filter { it.intent.action == Settings.ACTION_BLUETOOTH_SETTINGS || it.intent.action == Settings.ACTION_SETTINGS }
                .forEach { opt ->
                    panel.addView(
                        makeActionRow(ctx, lp, opt.icon, opt.title, opt.subtitle) {
                            try { ctx.startActivity(opt.intent) } catch (_: Exception) {}
                        },
                        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 6) }
                    )
                }
            addBackBtn(ctx, panel, lp, onBack)
            return
        }

        val hasBtPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

        var isBtOn = try { adapter.isEnabled } catch (_: Exception) { false }

        val statusCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            isFocusable = true
            isClickable = true
            background = cardBg(ctx, lp.accentColor, isBtOn, Color.rgb(65, 150, 255))
        }
        val titleTv = text(ctx, if (isBtOn) "Bluetooth Enabled" else "Bluetooth Turned Off", 11.5f, Color.WHITE, true)
        val subTv = text(ctx, "Live hardware state • Tap to toggle", 9f, Color.argb(215, 225, 240, 255), false)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleTv)
            addView(subTv)
        }
        val pill = TextView(ctx).apply {
            text = if (isBtOn) "ON" else "OFF"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(ctx, 12), dp(ctx, 5), dp(ctx, 12), dp(ctx, 5))
            background = GradientDrawable().apply {
                setColor(if (isBtOn) Color.rgb(55, 145, 255) else Color.argb(85, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
            }
        }
        statusCard.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        statusCard.addView(pill)

        statusCard.setOnFocusChangeListener { v, f ->
            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
            val cur = try { adapter.isEnabled } catch (_: Exception) { isBtOn }
            v.background = cardBg(ctx, lp.accentColor, f || cur, Color.rgb(65, 150, 255))
        }
        statusCard.setOnClickListener {
            if (!hasBtPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx as? Activity)?.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 202)
                return@setOnClickListener
            }
            try {
                @Suppress("DEPRECATION")
                if (isBtOn) adapter.disable() else adapter.enable()
                isBtOn = !isBtOn
                titleTv.text = if (isBtOn) "Bluetooth Enabled" else "Bluetooth Turned Off"
                pill.text = if (isBtOn) "ON" else "OFF"
                pill.background = GradientDrawable().apply {
                    setColor(if (isBtOn) Color.rgb(55, 145, 255) else Color.argb(85, 255, 255, 255))
                    cornerRadius = LauncherUtils.dpF(ctx, 14f)
                }
                statusCard.background = cardBg(ctx, lp.accentColor, true, Color.rgb(65, 150, 255))
            } catch (_: Exception) {
                Toast.makeText(ctx, "Bluetooth radio managed by system", Toast.LENGTH_SHORT).show()
            }
        }
        panel.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 10) })

        val btSettingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        if (btSettingsIntent.resolveActivity(ctx.packageManager) != null) {
            panel.addView(
                makeActionRow(ctx, lp, "📡", "System Bluetooth Pairing", "Open TV Bluetooth scan & pair screen", Color.rgb(92, 184, 255)) {
                    try { ctx.startActivity(btSettingsIntent) } catch (_: Exception) {}
                },
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) }
            )
        }

        addHeader(ctx, panel, lp, "PAIRED SYSTEM DEVICES")
        if (!hasBtPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permBtn = makeActionRow(ctx, lp, "🔓", "Allow Bluetooth Access", "Grant permission to list bonded devices", Color.rgb(255, 195, 75)) {
                (ctx as? Activity)?.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 202)
            }
            panel.addView(permBtn, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })
        } else {
            val bonded = try { adapter.bondedDevices?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (bonded.isEmpty()) {
                panel.addView(text(ctx, "No bonded Bluetooth devices found.", 9.5f, Color.LTGRAY, false).apply {
                    setPadding(dp(ctx, 4), dp(ctx, 6), dp(ctx, 4), dp(ctx, 10))
                })
            } else {
                for (dev in bonded) {
                    val dName = try { dev.name ?: dev.address } catch (_: Exception) { "Bluetooth Device" }
                    val row = makeActionRow(ctx, lp, "🎧", dName, "Bonded • ${dev.address}", Color.rgb(46, 232, 138)) {
                        try {
                            if (btSettingsIntent.resolveActivity(ctx.packageManager) != null) {
                                ctx.startActivity(btSettingsIntent)
                            } else {
                                Toast.makeText(ctx, "$dName (${dev.address})", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {}
                    }
                    panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
                }
            }
        }
        addBackBtn(ctx, panel, lp, onBack)
    }

    fun buildStorageAndAppsPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        addHeader(ctx, panel, lp, "STORAGE TELEMETRY (LIVE)")
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usedPct = ((usedBytes.toDouble() / totalBytes.coerceAtLeast(1L)) * 100).toInt()

        val totalGb = String.format(Locale.US, "%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))
        val freeGb = String.format(Locale.US, "%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0))
        val usedGb = String.format(Locale.US, "%.1f GB", usedBytes / (1024.0 * 1024.0 * 1024.0))

        val storageCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            background = cardBg(ctx, lp.accentColor, true, Color.rgb(46, 210, 165))
        }
        val topRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        topRow.addView(text(ctx, "💾  Used: $usedGb / $totalGb", 10.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        topRow.addView(text(ctx, "$usedPct%", 10.5f, lp.accentColor, true))
        storageCard.addView(topRow)

        val barBg = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.argb(65, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 6f)
            }
        }
        val fillView = View(ctx).apply {
            background = GradientDrawable().apply {
                setColor(lp.accentColor)
                cornerRadius = LauncherUtils.dpF(ctx, 6f)
            }
        }
        barBg.addView(fillView, LinearLayout.LayoutParams(0, dp(ctx, 8), usedPct.coerceIn(5, 100).toFloat()))
        barBg.addView(View(ctx), LinearLayout.LayoutParams(0, dp(ctx, 8), (100 - usedPct).coerceIn(0, 95).toFloat()))
        storageCard.addView(barBg, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 6) })
        storageCard.addView(text(ctx, "Available Space: $freeGb Free", 9f, Color.rgb(125, 240, 175), true))
        panel.addView(storageCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 10) })

        val sysAppsIntent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
        if (sysAppsIntent.resolveActivity(ctx.packageManager) != null) {
            panel.addView(
                makeActionRow(ctx, lp, "⚙", "System App Manager", "Open system installed apps list", Color.rgb(185, 135, 255)) {
                    try { ctx.startActivity(sysAppsIntent) } catch (_: Exception) {}
                },
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) }
            )
        }

        addHeader(ctx, panel, lp, "DISCOVERED APPS ON DEVICE")
        val pm = ctx.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launchIntent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.ROOT) }

        for (info in apps) {
            val pkg = info.activityInfo.packageName
            val lbl = info.loadLabel(pm).toString()
            val row = makeActionRow(ctx, lp, "📦", lbl, pkg, lp.accentColor) {
                try {
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    ctx.startActivity(i)
                } catch (_: Exception) {
                    Toast.makeText(ctx, lbl, Toast.LENGTH_SHORT).show()
                }
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }
        addBackBtn(ctx, panel, lp, onBack)
    }

    fun buildDisplayTimeoutAndSystemOptions(ctx: Context, panel: LinearLayout, lp: LauncherPreferences) {
        addHeader(ctx, panel, lp, "SYSTEM SCREEN TIMEOUT OPTIONS")
        val rawTimeouts = listOf(300_000, 900_000, 1_800_000, 2_147_483_647)
        var currentTimeout = try {
            Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 900_000)
        } catch (_: Exception) { 900_000 }

        val cards = ArrayList<Pair<LinearLayout, TextView>>()
        rawTimeouts.forEach { ms ->
            val label = if (ms > 100_000_000) {
                "Never Sleep"
            } else {
                val mins = ms / 60_000
                "$mins Minutes"
            }
            val isSel = kotlin.math.abs(currentTimeout - ms) < 60_000 || (ms > 100_000_000 && currentTimeout > 100_000_000)
            val tv = text(ctx, if (isSel) "✓  $label" else "○  $label", 10.5f, if (isSel) lp.accentColor else Color.WHITE, true)
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
                isFocusable = true
                isClickable = true
                background = cardBg(ctx, lp.accentColor, isSel, Color.rgb(255, 195, 75))
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 3f)
                    v.background = cardBg(ctx, lp.accentColor, f || currentTimeout == ms, Color.rgb(255, 195, 75))
                }
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(ctx)) {
                        try {
                            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
                            currentTimeout = ms
                            cards.forEachIndexed { idx, (cView, tView) ->
                                val targetMs = rawTimeouts[idx]
                                val tLabel = if (targetMs > 100_000_000) "Never Sleep" else "${targetMs / 60_000} Minutes"
                                val active = targetMs == ms
                                tView.text = if (active) "✓  $tLabel" else "○  $tLabel"
                                tView.setTextColor(if (active) lp.accentColor else Color.WHITE)
                                cView.background = cardBg(ctx, lp.accentColor, cView.hasFocus() || active, Color.rgb(255, 195, 75))
                            }
                            Toast.makeText(ctx, "Screen timeout set to $label", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {}
                    } else {
                        Toast.makeText(ctx, "Enable Write Settings permission above first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            card.addView(tv, LinearLayout.LayoutParams(0, -2, 1f))
            cards.add(card to tv)
            panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }

        val availableSys = resolveAvailableSystemIntents(ctx)
        if (availableSys.isNotEmpty()) {
            addHeader(ctx, panel, lp, "SUPPORTED DEVICE SETTINGS")
            for (opt in availableSys) {
                panel.addView(
                    makeActionRow(ctx, lp, opt.icon, opt.title, opt.subtitle, lp.accentColor) {
                        try { ctx.startActivity(opt.intent) } catch (_: Exception) {}
                    },
                    LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) }
                )
            }
        }
    }

    private fun makeActionRow(
        ctx: Context,
        lp: LauncherPreferences,
        icon: String,
        title: String,
        sub: String,
        tint: Int = lp.accentColor,
        onClick: () -> Unit
    ) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
        isFocusable = true
        isClickable = true
        background = cardBg(ctx, lp.accentColor, false, tint)
        setOnFocusChangeListener { v, f ->
            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
            v.background = cardBg(ctx, lp.accentColor, f, tint)
        }
        setOnClickListener { onClick() }
        addView(text(ctx, icon, 14f, tint, true).apply { setPadding(0, 0, dp(ctx, 10), 0) })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(text(ctx, title, 10.5f, Color.WHITE, true).apply { isSingleLine = true; ellipsize = TextUtils.TruncateAt.END })
        mid.addView(text(ctx, sub, 8.5f, Color.argb(195, 215, 232, 250), false).apply { isSingleLine = true; ellipsize = TextUtils.TruncateAt.END })
        addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        addView(text(ctx, "›", 15f, Color.argb(200, 255, 255, 255), true))
    }

    private fun addBackBtn(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        val btn = TextView(ctx).apply {
            text = "‹  Back to Control Center"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = cardBg(ctx, lp.accentColor, true, lp.accentColor)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f)
                v.background = cardBg(ctx, lp.accentColor, true, lp.accentColor)
            }
            setOnClickListener { onBack() }
        }
        panel.addView(btn, LinearLayout.LayoutParams(-1, dp(ctx, 40)).apply { topMargin = dp(ctx, 10); bottomMargin = dp(ctx, 20) })
    }

    private fun addHeader(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, label: String) {
        panel.addView(
            text(ctx, label.uppercase(Locale.ROOT), 9f, lp.accentColor, true).apply { letterSpacing = 0.08f },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 5) }
        )
    }

    private fun cardBg(ctx: Context, accent: Int, active: Boolean, glowTint: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(glowTint, 150), LauncherUtils.withAlpha(accent, 75))
        else intArrayOf(LauncherUtils.withAlpha(glowTint, 68), Color.argb(34, 20, 30, 65))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 15f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) Color.WHITE else LauncherUtils.withAlpha(glowTint, 125))
    }

    private fun text(ctx: Context, v: String, sz: Float, col: Int, bld: Boolean) = TextView(ctx).apply {
        text = v
        textSize = sz
        setTextColor(col)
        if (bld) typeface = Typeface.DEFAULT_BOLD
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
