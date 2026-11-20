package shree.tvhome.source

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import shree.tvhome.design.LauncherUtils
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object LauncherBackground {

    data class SceneSpec(
        val key: String,
        val name: String,
        val icon: String,
        val swatches: IntArray
    )

    const val MODE_SMART = "smart"
    const val MODE_AUTO_TIME = "auto"
    const val MODE_MORNING = "morning"
    const val MODE_PASTEL = "pastel"
    const val MODE_SUNSET = "sunset"
    const val MODE_AQUA = "aqua"
    const val MODE_RAIN = "rain"
    const val MODE_COSMOS = "cosmos"

    val SCENES = listOf(
        SceneSpec(MODE_MORNING, "Morning", "🌅", intArrayOf(Color.rgb(255, 195, 110), Color.rgb(110, 195, 255), Color.rgb(255, 235, 195))),
        SceneSpec(MODE_PASTEL, "Pastel", "🌸", intArrayOf(Color.rgb(255, 165, 205), Color.rgb(185, 165, 255), Color.rgb(165, 235, 245))),
        SceneSpec(MODE_SUNSET, "Sunset", "🌇", intArrayOf(Color.rgb(255, 105, 85), Color.rgb(245, 155, 55), Color.rgb(135, 55, 145))),
        SceneSpec(MODE_AQUA, "Aqua", "🌊", intArrayOf(Color.rgb(25, 185, 215), Color.rgb(55, 235, 185), Color.rgb(20, 95, 165))),
        SceneSpec(MODE_RAIN, "Rain", "🌧", intArrayOf(Color.rgb(75, 105, 145), Color.rgb(125, 165, 205), Color.rgb(45, 65, 95))),
        SceneSpec(MODE_COSMOS, "Cosmos", "🌌", intArrayOf(Color.rgb(125, 85, 255), Color.rgb(55, 195, 255), Color.rgb(255, 95, 185)))
    )

    val MANUAL_NAMES: List<String> get() = SCENES.map { it.name }
    val MANUAL_KEYS: List<String> get() = SCENES.map { it.key }
    val MANUAL_ICONS: List<String> get() = SCENES.map { it.icon }
    val MANUAL_SWATCHES: List<IntArray> get() = SCENES.map { it.swatches }

    private var ambientAnimator: ValueAnimator? = null
    private var activeCanvas: AnimatedCanvasView? = null
    private var activePhotoAura: WeatherAuraView? = null
    private var activeWallpaperView: ImageView? = null
    private val autoSyncHandler = Handler(Looper.getMainLooper())
    private var autoSyncRunnable: Runnable? = null

    fun apply(ctx: Context, root: FrameLayout, lp: LauncherPreferences) {
        stopAll()
        val uriStr = lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null)
        if (!uriStr.isNullOrEmpty()) {
            val bmp = decodeSampledBitmapFromUri(ctx, Uri.parse(uriStr), 1920, 1080)
            if (bmp != null) {
                val img = ImageView(ctx).apply {
                    tag = "launcher_bg_layer"
                    setImageBitmap(bmp)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                applyPhotoTransform(ctx, img, lp)
                root.addView(img, 0, FrameLayout.LayoutParams(-1, -1))
                activeWallpaperView = img

                if (lp.photoWeatherAura) {
                    val aura = WeatherAuraView(ctx, lp.accentColor, resolveEffectiveMode(lp)).apply {
                        tag = "launcher_bg_aura"
                    }
                    root.addView(aura, 1, FrameLayout.LayoutParams(-1, -1))
                    activePhotoAura = aura
                    startAnimationLoop(null, aura)
                }
                scheduleAutoModeWatcher(ctx, root, lp)
                return
            }
        }
        applyAnimatedCanvas(ctx, root, lp)
    }

    fun applyPhotoTransform(ctx: Context, img: ImageView, lp: LauncherPreferences) {
        val scaleMode = lp.prefs.getString("wp_scale_mode", "fit") ?: "fit"
        val zoom = lp.prefs.getFloat("wp_zoom", 1.08f).coerceIn(0.6f, 2.5f)
        val offX = LauncherUtils.dpF(ctx, lp.prefs.getInt("wp_offset_x", 0).toFloat())
        val offY = LauncherUtils.dpF(ctx, lp.prefs.getInt("wp_offset_y", 0).toFloat())
        val rotateDeg = lp.prefs.getInt("wp_rotate", 0).toFloat()

        img.scaleType = if (scaleMode == "fit") ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
        img.post {
            img.pivotX = img.width * 0.5f
            img.pivotY = img.height * 0.5f
            img.scaleX = zoom
            img.scaleY = zoom
            img.translationX = offX
            img.translationY = offY
            img.rotation = rotateDeg
        }
    }

    fun updateThemeAndAccent(root: FrameLayout, lp: LauncherPreferences) {
        val uriStr = lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null)
        if (!uriStr.isNullOrEmpty() && activeWallpaperView != null) {
            activeWallpaperView?.let { applyPhotoTransform(root.context, it, lp) }
            val effectiveMode = resolveEffectiveMode(lp)
            if (lp.photoWeatherAura) {
                if (activePhotoAura == null) {
                    val aura = WeatherAuraView(root.context, lp.accentColor, effectiveMode).apply {
                        tag = "launcher_bg_aura"
                    }
                    root.addView(aura, 1, FrameLayout.LayoutParams(-1, -1))
                    activePhotoAura = aura
                    startAnimationLoop(null, aura)
                } else {
                    activePhotoAura?.updateStyle(lp.accentColor, effectiveMode)
                }
            } else {
                activePhotoAura?.let { root.removeView(it) }
                activePhotoAura = null
                stopAll()
            }
            return
        }

        val canvasView = activeCanvas
        if (canvasView != null) {
            canvasView.updatePalette(lp.accentColor, resolveEffectiveMode(lp))
        } else {
            applyAnimatedCanvas(root.context, root, lp)
        }
    }

    fun onScrollParallax(scrollY: Int) {
        activeWallpaperView?.translationY = -(scrollY * 0.14f)
        activeCanvas?.setScrollOffset(scrollY.toFloat())
        activePhotoAura?.setScrollOffset(scrollY.toFloat())
    }

    private fun scheduleAutoModeWatcher(ctx: Context, root: FrameLayout, lp: LauncherPreferences) {
        autoSyncRunnable?.let { autoSyncHandler.removeCallbacks(it) }
        val r = object : Runnable {
            override fun run() {
                if (lp.liveWallpaperMode == MODE_AUTO_TIME || lp.liveWallpaperMode == MODE_SMART) {
                    updateThemeAndAccent(root, lp)
                }
                autoSyncHandler.postDelayed(this, 60_000L)
            }
        }
        autoSyncRunnable = r
        autoSyncHandler.postDelayed(r, 60_000L)
    }

    private fun applyAnimatedCanvas(ctx: Context, root: FrameLayout, lp: LauncherPreferences) {
        val view = AnimatedCanvasView(ctx, lp.accentColor, resolveEffectiveMode(lp)).apply {
            tag = "launcher_bg_layer"
        }
        root.addView(view, 0, FrameLayout.LayoutParams(-1, -1))
        activeCanvas = view
        startAnimationLoop(view, null)
        scheduleAutoModeWatcher(ctx, root, lp)
    }

    private fun startAnimationLoop(canvasView: AnimatedCanvasView?, auraView: WeatherAuraView?) {
        ambientAnimator?.cancel()
        ambientAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val phase = anim.animatedValue as Float
                canvasView?.setPhase(phase)
                auraView?.setPhase(phase)
            }
            start()
        }
    }

    fun stopAll() {
        ambientAnimator?.cancel()
        ambientAnimator = null
        autoSyncRunnable?.let { autoSyncHandler.removeCallbacks(it) }
        autoSyncRunnable = null
        activeCanvas = null
        activePhotoAura = null
        activeWallpaperView = null
    }

    fun resolveEffectiveMode(lp: LauncherPreferences): String {
        return when (lp.liveWallpaperMode) {
            MODE_AUTO_TIME -> getTimeBasedMode()
            MODE_SMART -> {
                val code = lp.cachedWeatherCode
                when {
                    code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99) -> MODE_RAIN
                    else -> getTimeBasedMode()
                }
            }
            else -> lp.liveWallpaperMode
        }
    }

    private fun getTimeBasedMode(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> MODE_MORNING
            in 11..16 -> MODE_AQUA
            in 17..19 -> MODE_SUNSET
            else -> MODE_COSMOS
        }
    }

    private fun getSkyPalette(mode: String, accent: Int): IntArray {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (mode) {
            MODE_MORNING -> intArrayOf(Color.rgb(28, 46, 86), Color.rgb(66, 108, 158), Color.rgb(176, 138, 104), Color.rgb(22, 30, 56))
            MODE_PASTEL -> intArrayOf(Color.rgb(58, 42, 86), Color.rgb(108, 72, 132), Color.rgb(148, 92, 136), Color.rgb(34, 26, 58))
            MODE_SUNSET -> intArrayOf(Color.rgb(48, 22, 62), Color.rgb(138, 52, 76), Color.rgb(214, 104, 58), Color.rgb(24, 16, 42))
            MODE_AQUA -> intArrayOf(Color.rgb(10, 44, 74), Color.rgb(18, 96, 126), Color.rgb(34, 148, 156), Color.rgb(8, 26, 48))
            MODE_RAIN -> intArrayOf(Color.rgb(22, 32, 48), Color.rgb(42, 60, 84), Color.rgb(64, 86, 112), Color.rgb(14, 20, 32))
            MODE_COSMOS -> intArrayOf(Color.rgb(10, 14, 34), Color.rgb(24, 20, 62), Color.rgb(52, 24, 88), Color.rgb(8, 11, 26))
            else -> {
                val r = (Color.red(accent) * 0.24f).toInt().coerceIn(8, 55)
                val g = (Color.green(accent) * 0.24f).toInt().coerceIn(10, 60)
                val b = (Color.blue(accent) * 0.34f).toInt().coerceIn(22, 85)
                val top = if (hour in 6..17) Color.rgb(18, 36, 68) else Color.rgb(10, 15, 34)
                intArrayOf(top, Color.rgb(r + 12, g + 16, b + 28), Color.rgb(r, g, b), Color.rgb(8, 11, 24))
            }
        }
    }

    private fun decodeSampledBitmapFromUri(ctx: Context, uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
            var sample = 1
            while ((opts.outHeight / sample) > reqH || (opts.outWidth / sample) > reqW) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
        } catch (_: Exception) {
            null
        }
    }

    private class WeatherAuraView(
        ctx: Context,
        private var accentColor: Int,
        private var mode: String
    ) : View(ctx) {
        private var phase = 0f
        private var scrollY = 0f
        private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }

        fun setPhase(p: Float) {
            phase = p
            invalidate()
        }

        fun setScrollOffset(sy: Float) {
            scrollY = sy
            invalidate()
        }

        fun updateStyle(accent: Int, newMode: String) {
            accentColor = accent
            mode = newMode
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            val angle = (phase * 2.0 * PI).toFloat()
            val cx1 = w * (0.25f + 0.14f * cos(angle))
            val cy1 = h * (0.25f + 0.12f * sin(angle)) - scrollY * 0.1f
            val r1 = maxOf(w, h) * 0.48f
            orbPaint.shader = RadialGradient(
                cx1, cy1, r1,
                intArrayOf(LauncherUtils.withAlpha(accentColor, 58), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, orbPaint)

            if (mode == MODE_RAIN) {
                rainPaint.color = Color.argb(68, 195, 225, 255)
                val dropCount = 36
                for (i in 0 until dropCount) {
                    val seed = i * 97
                    val speed = ((phase * 1.8f + (seed % 100) * 0.01f) % 1f)
                    val x = (((seed * 73) % 1000) / 1000f) * w - speed * 28f
                    val y = speed * (h + 60f) - 30f
                    canvas.drawLine(x, y, x - 6f, y + 18f, rainPaint)
                }
            }
        }
    }

    private class AnimatedCanvasView(
        ctx: Context,
        private var accentColor: Int,
        private var mode: String
    ) : View(ctx) {
        private var phase = 0f
        private var scrollY = 0f
        private val bgPaint = Paint()
        private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2.2f
            strokeCap = Paint.Cap.ROUND
        }

        fun setPhase(p: Float) {
            phase = p
            invalidate()
        }

        fun setScrollOffset(sy: Float) {
            scrollY = sy
            invalidate()
        }

        fun updatePalette(accent: Int, newMode: String) {
            accentColor = accent
            mode = newMode
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            val pal = getSkyPalette(mode, accentColor)
            val parallaxShift = (scrollY * 0.16f).coerceAtMost(h * 0.25f)

            bgPaint.shader = LinearGradient(
                0f, -parallaxShift, w * 0.85f, h,
                pal, null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            val angle = (phase * 2.0 * PI).toFloat()
            val angle2 = angle + 2.1f

            val c1x = w * (0.22f + 0.18f * cos(angle))
            val c1y = h * (0.24f + 0.15f * sin(angle)) - parallaxShift * 0.6f
            val r1 = maxOf(w, h) * 0.52f
            blobPaint.shader = RadialGradient(
                c1x, c1y, r1,
                intArrayOf(LauncherUtils.withAlpha(accentColor, 72), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, blobPaint)

            val secondaryTint = when (mode) {
                MODE_SUNSET -> Color.rgb(255, 145, 65)
                MODE_MORNING -> Color.rgb(255, 215, 130)
                MODE_AQUA -> Color.rgb(45, 225, 195)
                MODE_PASTEL -> Color.rgb(235, 155, 215)
                MODE_RAIN -> Color.rgb(120, 165, 210)
                else -> Color.rgb(125, 95, 255)
            }
            val c2x = w * (0.78f + 0.16f * cos(angle2))
            val c2y = h * (0.72f + 0.16f * sin(angle2)) - parallaxShift * 0.4f
            val r2 = maxOf(w, h) * 0.46f
            blobPaint.shader = RadialGradient(
                c2x, c2y, r2,
                intArrayOf(LauncherUtils.withAlpha(secondaryTint, 56), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, blobPaint)

            if (mode == MODE_COSMOS) {
                for (i in 0 until 28) {
                    val sx = (((i * 197) % 1000) / 1000f) * w
                    val sy = (((i * 313) % 1000) / 1000f) * h
                    val twinkle = ((sin((angle * 2f) + i) + 1f) * 0.5f)
                    starPaint.color = Color.argb((45 + twinkle * 150).toInt(), 235, 242, 255)
                    canvas.drawCircle(sx, sy, if (i % 4 == 0) 2.2f else 1.3f, starPaint)
                }
            } else if (mode == MODE_RAIN) {
                rainPaint.color = Color.argb(72, 190, 220, 250)
                for (i in 0 until 42) {
                    val seed = i * 83
                    val prog = ((phase * 2.0f + (seed % 100) * 0.01f) % 1f)
                    val rx = (((seed * 67) % 1000) / 1000f) * w - prog * 34f
                    val ry = prog * (h + 60f) - 30f
                    canvas.drawLine(rx, ry, rx - 7f, ry + 20f, rainPaint)
                }
            }
        }
    }

    fun getDrawable(ctx: Context, lp: LauncherPreferences): Drawable? = null
}
