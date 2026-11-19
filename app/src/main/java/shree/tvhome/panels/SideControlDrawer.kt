package shree.tvhome.panels

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.LauncherPreferences
import shree.tvhome.LauncherUtils
import shree.tvhome.MotionEngine
import shree.tvhome.homescreen.AppGridManager
import shree.tvhome.services.TvNotificationService
import java.util.Locale

class SideControlDrawer(
    private val context: Context,
    private val drawerLayout: LinearLayout,
    private val scrim: View,
    private val mainPage: View,
    private val lp: LauncherPreferences,
    private val onLiveRefresh: () -> Unit,
    private val onWallpaperRefresh: () -> Unit,
    private val onPickWallpaper: () -> Unit,
    private val onPickProfilePic: () -> Unit,
    private val onOpenHiddenRibbon: () -> Unit
) {
    companion object {
        const val MODE_CUSTOMIZE = "customize"
        const val MODE_QUICK = "quick"
        const val MODE_WIFI = "wifi"
        const val MODE_ALERTS = "alerts"
        const val MODE_POWER = "power"
        const val MODE_WALLPAPER_EDIT = "wp_edit"
        const val MODE_SUB_INPUTS = "sub_inputs"
        const val MODE_SUB_PICTURE = "sub_picture"
        const val MODE_SUB_SOUND = "sub_sound"
        const val MODE_SUB_BLUETOOTH = "sub_bluetooth"
        const val MODE_SUB_STORAGE = "sub_storage"
    }

    var isOpen = false
        private set
    private var currentMode = ""
    private var returnMode: String? = null
    private var activeScroll: ScrollView? = null
    private var activePanel: LinearLayout? = null
    private var brightnessPct = getCurrentSystemBrightnessPct()
    private var stageWidget: View? = null

    fun attachStageWidget(widget: View) {
        stageWidget = widget
    }

    fun getParentNavTag(): String = when (currentMode) {
        MODE_WIFI -> "nav_wifi"
        MODE_ALERTS -> "nav_alerts"
        MODE_CUSTOMIZE, MODE_WALLPAPER_EDIT -> "nav_customize"
        MODE_POWER -> "nav_power"
        else -> "nav_quick"
    }

    fun handleBackPress(): Boolean {
        val backTarget = returnMode
        return if (backTarget != null) {
            returnMode = null
            open(backTarget, preserveReturn = false)
            true
        } else {
            false
        }
    }

    fun toggle(mode: String) {
        if (isOpen && mode == currentMode) close() else open(mode, preserveReturn = false)
    }

    fun open(mode: String, preserveReturn: Boolean = false) {
        val wasOpen = isOpen
        if (preserveReturn && currentMode.isNotEmpty() && currentMode != mode) {
            returnMode = currentMode
        } else if (!preserveReturn) {
            returnMode = null
        }

        currentMode = mode
        isOpen = true

        val screenW = context.resources.displayMetrics.widthPixels
        val dynamicDrawerW = maxOf(LauncherUtils.dp(context, 250), (screenW * (lp.panelWidthPct / 100f)).toInt())
        drawerLayout.layoutParams.width = dynamicDrawerW
        drawerLayout.clipChildren = false
        drawerLayout.clipToPadding = false
        drawerLayout.requestLayout()

        drawerLayout.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb(245, 28, 40, 84),
                Color.argb(250, 17, 25, 58),
                Color.argb(252, 11, 16, 42)
            )
        ).apply {
            cornerRadius = LauncherUtils.dpF(context, 26f)
            setStroke(dp(2f), Color.argb(165, 255, 255, 255))
        }
        drawerLayout.outlineProvider = null

        scrim.setBackgroundColor(Color.TRANSPARENT)
        scrim.visibility = View.VISIBLE
        drawerLayout.visibility = View.VISIBLE

        setupDrawerSkeleton()
        populateActivePanel(true)

        if (!wasOpen) {
            MotionEngine.resetTransform(drawerLayout)
            MotionEngine.animatePanelEnter(drawerLayout, stageWidget, dynamicDrawerW, lp.motionStyle) {}
        } else {
            activePanel?.let { MotionEngine.playDemoPreview(it, lp.motionStyle) }
        }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        currentMode = ""
        returnMode = null
        val out = maxOf(dp(340f), drawerLayout.width + dp(30f))

        MotionEngine.animatePanelExit(drawerLayout, stageWidget, out, lp.motionStyle) {
            scrim.visibility = View.GONE
            drawerLayout.visibility = View.GONE
        }
        drawerLayout.post { onLiveRefresh() }
    }

    private fun setupDrawerSkeleton() {
        drawerLayout.removeAllViews()
        drawerLayout.setPadding(dp(14f), dp(14f), dp(14f), dp(14f))

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(6f), dp(4f), dp(6f), dp(10f))
        }

        val (icon, title) = when (currentMode) {
            MODE_CUSTOMIZE -> "✦  " to "Customize Launcher"
            MODE_WIFI -> "📶  " to "Network & Internet"
            MODE_ALERTS -> "🔔  " to "Alerts & Notifications"
            MODE_POWER -> "⏻  " to "Power & System"
            MODE_WALLPAPER_EDIT -> "🖼  " to "Wallpaper Studio"
            MODE_SUB_INPUTS -> "📺  " to "Discovered TV Inputs"
            MODE_SUB_PICTURE -> "☀  " to "Display & System Controls"
            MODE_SUB_SOUND -> "🔊  " to "Discovered Audio Outputs"
            MODE_SUB_BLUETOOTH -> "🔵  " to "Bluetooth & Devices"
            MODE_SUB_STORAGE -> "💾  " to "Storage & Installed Apps"
            else -> "⚡  " to "Control Center"
        }
        topRow.addView(text("$icon$title", 13.5f, Color.WHITE, true))
        topRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))

        val closeX = TextView(context).apply {
            text = "✕"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = xBtnBg(false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.12f, 4f)
                v.background = xBtnBg(f)
            }
            setOnClickListener { close() }
        }
        topRow.addView(closeX, LinearLayout.LayoutParams(dp(28f), dp(28f)))
        drawerLayout.addView(topRow)

        val divider = View(context).apply { setBackgroundColor(Color.argb(65, 255, 255, 255)) }
        drawerLayout.addView(divider, LinearLayout.LayoutParams(-1, dp(1f)).apply { bottomMargin = dp(8f) })

        activeScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
            clipChildren = false
            clipToPadding = false
        }
        activePanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(6f), dp(6f), dp(6f), dp(20f))
        }
        drawerLayout.addView(activeScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        activeScroll?.addView(activePanel)
    }

    private fun populateActivePanel(isInitialOpen: Boolean) {
        val panel = activePanel ?: return
        panel.removeAllViews()

        panel.alpha = 0f
        panel.scaleY = 0.95f
        panel.animate()
            .alpha(1f)
            .scaleY(1f)
            .setDuration(260L)
            .setInterpolator(LauncherUtils.FLUID_SPRING)
            .start()

        when (currentMode) {
            MODE_CUSTOMIZE -> CustomizeMenuBuilder.build(
                context, panel, lp, onLiveRefresh,
                ::showProfileEditDialog,
                { open(MODE_WALLPAPER_EDIT, preserveReturn = true) },
                onOpenHiddenRibbon,
                ::close
            )
            MODE_WIFI -> WifiPanelBuilder.build(context, panel, lp)
            MODE_ALERTS -> buildRealAlertsPanel(panel)
            MODE_POWER -> buildPowerPanel(panel)
            MODE_WALLPAPER_EDIT -> WallpaperEditorPanel.build(context, panel, lp, onWallpaperRefresh, onPickWallpaper) {
                open(returnMode ?: MODE_CUSTOMIZE, preserveReturn = false)
            }
            MODE_SUB_INPUTS -> buildInputsSubPanel(panel)
            MODE_SUB_PICTURE -> buildPictureSubPanel(panel)
            MODE_SUB_SOUND -> buildSoundSubPanel(panel)
            MODE_SUB_BLUETOOTH -> SystemSubPanels.buildBluetoothPanel(context, panel, lp) {
                open(returnMode ?: MODE_QUICK, preserveReturn = false)
            }
            MODE_SUB_STORAGE -> SystemSubPanels.buildStorageAndAppsPanel(context, panel, lp) {
                open(returnMode ?: MODE_QUICK, preserveReturn = false)
            }
            else -> buildControlCenter(panel)
        }
        if (isInitialOpen) panel.post { panel.requestFocus() }
    }

    private fun buildControlCenter(panel: LinearLayout) {
        addLabel(panel, "DISCOVERED SYSTEM & TV CONTROLS")

        val r1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false; clipToPadding = false }
        r1.addView(
            makeGridTile("☀", "Display", "$brightnessPct% • Live", Color.rgb(255, 195, 75)) {
                open(MODE_SUB_PICTURE, preserveReturn = true)
            },
            LinearLayout.LayoutParams(0, dp(56f), 1f).apply { rightMargin = dp(6f) }
        )
        r1.addView(
            makeGridTile("🔊", "Sound", getActiveAudioDeviceName().removeSuffix(" ›"), Color.rgb(46, 232, 138)) {
                open(MODE_SUB_SOUND, preserveReturn = true)
            },
            LinearLayout.LayoutParams(0, dp(56f), 1f)
        )
        panel.addView(r1, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6f) })

        val r2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false; clipToPadding = false }
        r2.addView(
            makeGridTile("📶", "Wi-Fi", "Scan & Connect", Color.rgb(92, 184, 255)) {
                open(MODE_WIFI, preserveReturn = true)
            },
            LinearLayout.LayoutParams(0, dp(56f), 1f).apply { rightMargin = dp(6f) }
        )
        r2.addView(
            makeGridTile("🔵", "Bluetooth", "Bonded Devices", Color.rgb(120, 155, 255)) {
                open(MODE_SUB_BLUETOOTH, preserveReturn = true)
            },
            LinearLayout.LayoutParams(0, dp(56f), 1f)
        )
        panel.addView(r2, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6f) })

        val r3 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false; clipToPadding = false }
        r3.addView(
            makeGridTile("📺", "TV Inputs", "Hardware Inputs", Color.rgb(255, 110, 145)) {
                open(MODE_SUB_INPUTS, preserveReturn = true)
            },
            LinearLayout.LayoutParams(0, dp(56f), 1f).apply { rightMargin = dp(6f) }
        )
        r3.addView(
            makeGridTile("💾", "Storage & Apps", "Space & App Info", Color.rgb(185, 135, 255)) {
                open(MODE_SUB_STORAGE, preserveReturn = true)
            },
            LinearLayout.LayoutParams(0, dp(56f), 1f)
        )
        panel.addView(r3, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8f) })

        addLabel(panel, "LIVE HARDWARE SLIDERS")
        addSliderCard(panel, "☀  Screen Brightness", brightnessPct, Color.rgb(255, 195, 75)) { p ->
            brightnessPct = p
            applyRealBrightness(p)
        }

        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val curVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        val volPct = ((curVol * 100f) / maxOf(1, maxVol)).toInt()
        addSliderCard(panel, "🔊  Media Volume", volPct, Color.rgb(46, 232, 138)) { p ->
            am?.setStreamVolume(AudioManager.STREAM_MUSIC, ((p / 100f) * maxVol).toInt(), AudioManager.FLAG_SHOW_UI)
        }

        val ramCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(9f), dp(12f), dp(9f))
            isFocusable = true
            isClickable = true
            background = tintedCardBg(Color.rgb(92, 184, 255), false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                v.background = tintedCardBg(Color.rgb(92, 184, 255), f)
            }
        }
        ramCard.addView(text("🚀", 14f, Color.rgb(92, 184, 255), true).apply { setPadding(0, 0, dp(10f), 0) })
        val ramMid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        ramMid.addView(text("Boost Memory (RAM)", 10.5f, Color.WHITE, true))
        val ramSubTv = text(getRamStatusText(), 8.5f, Color.argb(195, 215, 232, 250), false)
        ramMid.addView(ramSubTv)
        ramCard.addView(ramMid, LinearLayout.LayoutParams(0, -2, 1f))
        ramCard.addView(text("›", 15f, Color.argb(200, 255, 255, 255), true))
        ramCard.setOnClickListener {
            Runtime.getRuntime().gc()
            ramSubTv.text = getRamStatusText()
            Toast.makeText(context, "Memory optimized!", Toast.LENGTH_SHORT).show()
        }
        panel.addView(ramCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4f); bottomMargin = dp(6f) })
    }

    private fun getCurrentSystemBrightnessPct(): Int = try {
        val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 200)
        ((raw / 255f) * 100f).toInt().coerceIn(10, 100)
    } catch (_: Exception) { 85 }

    private fun applyRealBrightness(pct: Int) {
        val clamped = pct.coerceIn(10, 100)
        (context as? Activity)?.window?.let { win ->
            val attr = win.attributes
            attr.screenBrightness = clamped / 100f
            win.attributes = attr
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, ((clamped / 100f) * 255).toInt())
            } catch (_: Exception) {}
        }
    }

    private fun buildInputsSubPanel(panel: LinearLayout) {
        addLabel(panel, "DISCOVERED TV & HARDWARE INPUTS")
        val tvMgr = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        val inputs = try { tvMgr?.tvInputList ?: emptyList() } catch (_: Exception) { emptyList() }
        if (inputs.isEmpty()) {
            panel.addView(text("No TV input sources reported by this device hardware.", 9.5f, Color.LTGRAY, false).apply {
                setPadding(dp(4f), dp(8f), dp(4f), dp(12f))
            })
        } else {
            for (inp in inputs) {
                val lbl = try { inp.loadLabel(context).toString() } catch (_: Exception) { inp.id }
                addActionButton(panel, "📺", lbl, inp.id, Color.rgb(255, 110, 145)) {
                    try {
                        val setupIntent = inp.createSetupIntent()
                        if (setupIntent != null) {
                            context.startActivity(setupIntent)
                        } else {
                            Toast.makeText(context, lbl, Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, lbl, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        addBackSubButton(panel)
    }

    private fun buildPictureSubPanel(panel: LinearLayout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            addActionButton(panel, "🔓", "Enable System Write Access", "Required to modify system brightness & sleep timeout", Color.rgb(255, 195, 75)) {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
                } catch (_: Exception) {}
            }
        }
        addLabel(panel, "LIVE BRIGHTNESS CONTROL")
        addSliderCard(panel, "☀  Screen Brightness", brightnessPct, Color.rgb(255, 195, 75)) { p ->
            brightnessPct = p
            applyRealBrightness(p)
        }
        SystemSubPanels.buildDisplayTimeoutAndSystemOptions(context, panel, lp)
        addBackSubButton(panel)
    }

    private fun buildSoundSubPanel(panel: LinearLayout) {
        addLabel(panel, "DISCOVERED AUDIO OUTPUTS")
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList() ?: emptyList()
        } else {
            emptyList()
        }
        if (devices.isEmpty()) {
            panel.addView(text("No audio output devices reported by system.", 9.5f, Color.LTGRAY, false).apply {
                setPadding(dp(4f), dp(8f), dp(4f), dp(12f))
            })
        } else {
            devices.distinctBy { it.type }.forEach { dev ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val prod = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: mapAudioType(dev.type)
                    addActionButton(panel, "🎧", mapAudioType(dev.type), prod, Color.rgb(46, 232, 138)) {
                        Toast.makeText(context, "$mapAudioType(dev.type): $prod", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        addLabel(panel, "LIVE VOLUME & MUTE")
        val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val curVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        val volPct = ((curVol * 100f) / maxOf(1, maxVol)).toInt()
        addSliderCard(panel, "🔊  Media Volume", volPct, Color.rgb(46, 232, 138)) { p ->
            am?.setStreamVolume(AudioManager.STREAM_MUSIC, ((p / 100f) * maxVol).toInt(), AudioManager.FLAG_SHOW_UI)
        }

        val initialMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am?.isStreamMute(AudioManager.STREAM_MUSIC) == true else false
        val muteCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(9f), dp(12f), dp(9f))
            isFocusable = true
            isClickable = true
            background = tintedCardBg(Color.rgb(92, 184, 255), false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                v.background = tintedCardBg(Color.rgb(92, 184, 255), f)
            }
        }
        val muteIconTv = text(if (initialMuted) "🔇" else "🔈", 14f, Color.rgb(92, 184, 255), true).apply { setPadding(0, 0, dp(10f), 0) }
        val muteTitleTv = text(if (initialMuted) "Unmute Media Audio" else "Mute Media Audio", 10.5f, Co
