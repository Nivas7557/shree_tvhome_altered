package shree.tvhome

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.design.VectorIconView
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.TileThemePalette
import shree.tvhome.source.TvNotificationService

class TopNavBar(
    context: Context,
    private val lp: LauncherPreferences,
    private val onAvatarClick: () -> Unit,
    private val onToggleWifiPanel: () -> Unit,
    private val onToggleAlertsPanel: () -> Unit,
    private val onToggleCustomize: () -> Unit,
    private val onToggleQuickMenu: () -> Unit,
    private val onTogglePowerMenu: () -> Unit
) : LinearLayout(context) {

    private val avatarBox: FrameLayout
    private val avatarImg: ImageView
    private val avatarFallbackTv: TextView
    private val greetingCard: GreetingShineCard
    private val ramPill: LinearLayout
    private val ramTv: TextView
    private val wifiBtn: FrameLayout
    private val wifiIcon: VectorIconView
    private val wifiLabel: TextView
    private val alertsBtn: FrameLayout
    private val alertsIcon: VectorIconView
    private val alertsBadge: TextView
    private val customizeBtn: FrameLayout
    private val customizeIcon: VectorIconView
    private val quickBtn: FrameLayout
    private val quickIcon: VectorIconView
    private val powerBtn: FrameLayout
    private val powerIcon: VectorIconView

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateRamInfo()
            updateWifiVisualState()
            updateAlertsBadge()
            refreshHandler.postDelayed(this, 8000L)
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(dp(4), dp(2), dp(4), dp(4))

        val avatarSize = dp(44)
        avatarBox = FrameLayout(context).apply {
            tag = "nav_avatar"
            isFocusable = true
            isClickable = true
            background = avatarFrameBg(false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.09f, 4f)
                v.background = avatarFrameBg(f)
            }
            setOnClickListener { onAvatarClick() }
        }

        avatarImg = ImageView(context).apply {
            scaleType = ImageView.ScaleType.MATRIX
        }
        avatarFallbackTv = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        avatarBox.addView(avatarImg, FrameLayout.LayoutParams(-1, -1))
        avatarBox.addView(avatarFallbackTv, FrameLayout.LayoutParams(-1, -1))
        addView(avatarBox, LayoutParams(avatarSize, avatarSize).apply { rightMargin = dp(10) })

        greetingCard = GreetingShineCard(context, lp)
        addView(greetingCard, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            rightMargin = dp(10)
        })

        ramPill = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = false
            isClickable = false
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = pillBg(Color.rgb(92, 184, 255), false)
        }
        ramPill.addView(TextView(context).apply {
            text = "RAM "
            textSize = 9f
            setTextColor(Color.argb(190, 210, 230, 255))
        })
        ramTv = TextView(context).apply {
            text = "--%"
            textSize = 10f
            setTextColor(Color.WHITE)
        }
        ramPill.addView(ramTv)
        addView(ramPill, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32)).apply { rightMargin = dp(8) })

        addView(View(context), LayoutParams(0, 1, 1f))

        val wifiPair = makeWifiPillButton()
        wifiBtn = wifiPair.first
        wifiIcon = wifiPair.second
        wifiLabel = wifiPair.third
        addView(wifiBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(36)).apply { rightMargin = dp(8) })

        val alertsPair = makeAlertsButton()
        alertsBtn = alertsPair.first
        alertsIcon = alertsPair.second
        alertsBadge = alertsPair.third
        addView(alertsBtn, LayoutParams(dp(40), dp(36)).apply { rightMargin = dp(8) })

        val custPair = makeIconActionButton("nav_customize", VectorIconView.IconKind.SPARKLES, Color.rgb(185, 135, 255)) {
            onToggleCustomize()
        }
        customizeBtn = custPair.first
        customizeIcon = custPair.second
        addView(customizeBtn, LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(8) })

        val quickPair = makeIconActionButton("nav_quick", VectorIconView.IconKind.SLIDERS, Color.rgb(46, 232, 138)) {
            onToggleQuickMenu()
        }
        quickBtn = quickPair.first
        quickIcon = quickPair.second
        addView(quickBtn, LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(8) })

        val powerPair = makeIconActionButton("nav_power", VectorIconView.IconKind.POWER, Color.rgb(255, 110, 145)) {
            onTogglePowerMenu()
        }
        powerBtn = powerPair.first
        powerIcon = powerPair.second
        addView(powerBtn, LayoutParams(dp(36), dp(36)))

        refreshProfileAvatar()
        updateGreeting()
        updateRamInfo()
        updateWifiVisualState()
        updateAlertsBadge()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, 8000L)
        updateRamInfo()
        updateWifiVisualState()
        updateAlertsBadge()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    fun updateGreeting() {
        greetingCard.updateGreeting()
        refreshProfileAvatar()
    }

    fun refreshProfileAvatar() {
        val fallbackChar = lp.userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
        avatarFallbackTv.text = fallbackChar

        val uriStr = lp.prefs.getString("profile_pic_uri", null)
        if (!uriStr.isNullOrEmpty()) {
            val zoom = lp.prefs.getFloat("profile_pic_zoom", 1.0f).coerceIn(1.0f, 3.0f)
            val panX = lp.prefs.getFloat("profile_pic_pan_x", 0f).coerceIn(-1f, 1f)
            val panY = lp.prefs.getFloat("profile_pic_pan_y", 0f).coerceIn(-1f, 1f)

            val circBmp = decodeCircularProfileBitmap(context, Uri.parse(uriStr), dp(40), zoom, panX, panY)
            if (circBmp != null) {
                avatarImg.scaleX = 1f
                avatarImg.scaleY = 1f
                avatarImg.translationX = 0f
                avatarImg.translationY = 0f
                avatarImg.setImageBitmap(circBmp)
                avatarImg.visibility = View.VISIBLE
                avatarFallbackTv.visibility = View.GONE
                return
            }
        }
        avatarImg.setImageDrawable(null)
        avatarImg.visibility = View.GONE
        avatarFallbackTv.visibility = View.VISIBLE
    }

    private fun decodeCircularProfileBitmap(
        ctx: Context,
        uri: Uri,
        outSizePx: Int,
        zoom: Float,
        panX: Float,
        panY: Float
    ): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxDim / sample > outSizePx * 2) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val src = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            val target = Bitmap.createBitmap(outSizePx, outSizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(target)

            val baseScale = maxOf(outSizePx.toFloat() / src.width, outSizePx.toFloat() / src.height)
            val effScale = baseScale * zoom
            val scaledW = src.width * effScale
            val scaledH = src.height * effScale

            val maxShiftX = maxOf(0f, (scaledW - outSizePx) * 0.5f)
            val maxShiftY = maxOf(0f, (scaledH - outSizePx) * 0.5f)

            val dx = (outSizePx - scaledW) * 0.5f + (panX * maxShiftX)
            val dy = (outSizePx - scaledH) * 0.5f + (panY * maxShiftY)

            val matrix = Matrix().apply {
                postScale(effScale, effScale)
                postTranslate(dx, dy)
            }

            val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rectF = RectF(0f, 0f, outSizePx.toFloat(), outSizePx.toFloat())
            canvas.drawOval(rectF, clipPaint)

            val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(src, matrix, srcPaint)
            if (src !== target && !src.isRecycled) src.recycle()
            target
        } catch (_: Exception) {
            null
        }
    }

    fun updateThemeAccent() {
        greetingCard.updateAccent()
        ramPill.background = pillBg(Color.rgb(92, 184, 255), false)
        updateWifiVisualState()
        updateAlertsBadge()
        avatarBox.background = avatarFrameBg(avatarBox.hasFocus())
        customizeBtn.background = pillBg(Color.rgb(185, 135, 255), customizeBtn.hasFocus())
        quickBtn.background = pillBg(Color.rgb(46, 232, 138), quickBtn.hasFocus())
        powerBtn.background = pillBg(Color.rgb(255, 110, 145), powerBtn.hasFocus())
    }

    fun updateRamInfo() {
        try {
            val mi = ActivityManager.MemoryInfo()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getMemoryInfo(mi)
            val usedPct = if (mi.totalMem > 0L) {
                (((mi.totalMem - mi.availMem) * 100f) / mi.totalMem).toInt().coerceIn(1, 99)
            } else 0
            ramTv.text = "$usedPct%"
        } catch (_: Exception) {
            ramTv.text = "--"
        }
    }

    fun updateWifiVisualState() {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val enabled = wm?.isWifiEnabled == true
        val ssid = TileThemePalette.getConnectedSsid(context)
        val focused = wifiBtn.hasFocus()

        if (!enabled) {
            wifiIcon.setIcon(VectorIconView.IconKind.WIFI_OFF, Color.rgb(255, 120, 145))
            wifiLabel.text = "Off"
            wifiBtn.background = pillBg(Color.rgb(255, 110, 145), focused)
        } else if (ssid != null) {
            wifiIcon.setIcon(VectorIconView.IconKind.WIFI_ON, Color.rgb(75, 235, 245))
            wifiLabel.text = ssid
            wifiBtn.background = pillBg(Color.rgb(75, 235, 245), focused)
        } else {
            wifiIcon.setIcon(VectorIconView.IconKind.WIFI_SCAN, Color.rgb(92, 184, 255))
            wifiLabel.text = "Wi-Fi"
            wifiBtn.background = pillBg(Color.rgb(92, 184, 255), focused)
        }
    }

    fun updateAlertsBadge() {
        val count = if (TvNotificationService.isPermissionGranted(context)) {
            TvNotificationService.getAlerts().size
        } else 0
        if (count > 0) {
            alertsBadge.visibility = View.VISIBLE
            alertsBadge.text = if (count > 9) "9+" else count.toString()
        } else {
            alertsBadge.visibility = View.GONE
        }
    }

    private fun makeWifiPillButton(): Triple<FrameLayout, VectorIconView, TextView> {
        val box = FrameLayout(context).apply {
            tag = "nav_wifi"
            isFocusable = true
            isClickable = true
            clipChildren = false
            clipToPadding = false
            setPadding(dp(10), dp(4), dp(12), dp(4))
            background = pillBg(Color.rgb(92, 184, 255), false)
        }
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = VectorIconView(context).apply {
            setIcon(VectorIconView.IconKind.WIFI_ON, Color.rgb(75, 235, 245))
        }
        val label = TextView(context).apply {
            text = "Wi-Fi"
            textSize = 10f
            setTextColor(Color.WHITE)
            isSingleLine = true
            maxWidth = dp(110)
        }
        row.addView(icon, LayoutParams(dp(16), dp(16)).apply { rightMargin = dp(6) })
        row.addView(label)
        box.addView(row, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))

        box.setOnFocusChangeListener { v, f ->
            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.08f, 4f)
            updateWifiVisualState()
            v.bringToFront()
        }
        box.setOnClickListener {
            LauncherUtils.pulseSelection(box)
            onToggleWifiPanel()
        }
        return Triple(box, icon, label)
    }

    private fun makeAlertsButton(): Triple<FrameLayout, VectorIconView, TextView> {
        val tint = Color.rgb(255, 195, 75)
        val box = FrameLayout(context).apply {
            tag = "nav_alerts"
            isFocusable = true
            isClickable = true
            clipChildren = false
            clipToPadding = false
            background = pillBg(tint, false)
        }
        val icon = VectorIconView(context).apply {
            setIcon(VectorIconView.IconKind.BELL, tint)
        }
        box.addView(icon, FrameLayout.LayoutParams(dp(17), dp(17), Gravity.CENTER))

        val badge = TextView(context).apply {
            textSize = 8f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(255, 75, 110))
                setStroke(dp(1), Color.WHITE)
            }
        }
        box.addView(badge, FrameLayout.LayoutParams(dp(15), dp(15), Gravity.TOP or Gravity.END))

        box.setOnFocusChangeListener { v, f ->
            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.08f, 4f)
            v.background = pillBg(tint, f)
            v.bringToFront()
        }
        box.setOnClickListener {
            LauncherUtils.pulseSelection(box)
            onToggleAlertsPanel()
        }
        return Triple(box, icon, badge)
    }

    private fun makeIconActionButton(
        tagName: String,
        kind: VectorIconView.IconKind,
        tint: Int,
        onClick: () -> Unit
    ): Pair<FrameLayout, VectorIconView> {
        val box = FrameLayout(context).apply {
            tag = tagName
            isFocusable = true
            isClickable = true
            clipChildren = false
            clipToPadding = false
            background = pillBg(tint, false)
        }
        val icon = VectorIconView(context).apply {
            setIcon(kind, tint)
        }
        box.addView(icon, FrameLayout.LayoutParams(dp(17), dp(17), Gravity.CENTER))
        box.setOnFocusChangeListener { v, f ->
            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.08f, 4f)
            v.background = pillBg(tint, f)
            v.bringToFront()
        }
        box.setOnClickListener {
            LauncherUtils.pulseSelection(box)
            onClick()
        }
        return box to icon
    }

    private fun avatarFrameBg(focused: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (focused) LauncherUtils.withAlpha(lp.accentColor, 180) else Color.argb(95, 36, 52, 98))
        setStroke(dp(if (focused) 2 else 1), if (focused) Color.WHITE else LauncherUtils.withAlpha(lp.accentColor, 200))
    }

    private fun pillBg(tint: Int, focused: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (focused) intArrayOf(LauncherUtils.withAlpha(tint, 175), LauncherUtils.withAlpha(lp.accentColor, 115))
        else intArrayOf(LauncherUtils.withAlpha(tint, 65), Color.argb(46, 22, 32, 68))
    ).apply {
        cornerRadius = LauncherUtils.dpF(context, 18f)
        setStroke(dp(if (focused) 2 else 1), if (focused) Color.WHITE else LauncherUtils.withAlpha(tint, 145))
    }

    private fun dp(v: Int) = LauncherUtils.dp(context, v)
}
