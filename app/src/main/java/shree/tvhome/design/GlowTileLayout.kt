package shree.tvhome.design

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.TileThemePalette
import kotlin.math.min

class GlowTileLayout(context: Context) : FrameLayout(context) {

    private var accentColor: Int = Color.rgb(92, 184, 255)
    private var iconShape: String = "squircle"
    private var glassThemeKey: String = TileThemePalette.KEYS.first()
    private var appDominantColor: Int = Color.rgb(92, 184, 255)
    private var isFocusedState: Boolean = false
    private var glowPulse: Float = 0f

    private val shapePath = Path()
    private val auraPath = Path()
    private val glossPath = Path()
    private val borderPath = Path()

    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var cachedWidth = -1
    private var cachedHeight = -1
    private var cachedShape = ""

    init {
        setWillNotDraw(false)
        clipChildren = true
        clipToPadding = false
        outlineProvider = null
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setStyle(accent: Int, shape: String, glassKey: String, appColor: Int) {
        val shapeChanged = this.iconShape != shape
        this.accentColor = accent
        this.iconShape = shape
        this.glassThemeKey = glassKey
        this.appDominantColor = appColor
        if (shapeChanged) {
            cachedWidth = -1
            cachedHeight = -1
        }
        updateShaders()
        invalidate()
    }

    fun setFocusState(focused: Boolean) {
        this.isFocusedState = focused
        if (!focused) this.glowPulse = 0f
        updateShaders()
        invalidate()
    }

    fun updateGlowPulse(pulse01: Float) {
        this.glowPulse = pulse01.coerceIn(0f, 1f)
        updateShaders()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPathsIfNeeded(w, h)
        updateShaders()
    }

    private fun rebuildPathsIfNeeded(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        if (w == cachedWidth && h == cachedHeight && iconShape == cachedShape) return

        cachedWidth = w
        cachedHeight = h
        cachedShape = iconShape

        val pad = LauncherUtils.dpF(context, 4f)
        val left = pad
        val top = pad
        val right = w - pad
        val bottom = h - pad
        val boxW = (right - left).coerceAtLeast(1f)
        val boxH = (bottom - top).coerceAtLeast(1f)
        val s = min(boxW, boxH)
        val cx = (left + right) * 0.5f
        val cy = (top + bottom) * 0.5f

        buildShapeIntoPath(shapePath, cx, cy, s, left, top, right, bottom)
        buildShapeIntoPath(glossPath, cx, cy, s * 0.92f, left + s * 0.04f, top + s * 0.04f, right - s * 0.04f, top + boxH * 0.52f)
        buildShapeIntoPath(borderPath, cx, cy, s, left, top, right, bottom)

        auraPath.reset()
        auraPath.addCircle(cx, cy, s * 0.62f, Path.Direction.CW)
    }

    private fun buildShapeIntoPath(
        target: Path,
        cx: Float,
        cy: Float,
        s: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        target.reset()
        val rect = RectF(left, top, right, bottom)
        val validKeys = LauncherPreferences.TILE_SHAPE_OPTIONS.map { it.key }
        val resolvedShape = if (iconShape in validKeys) iconShape else validKeys.first()
        when (resolvedShape) {
            "circle" -> target.addOval(rect, Path.Direction.CW)
            "classic" -> target.addRoundRect(rect, s * 0.14f, s * 0.14f, Path.Direction.CW)
            "heart" -> {
                val topDipY = top + s * 0.22f
                val lobeTopY = top + s * 0.02f
                val bottomTipY = bottom - s * 0.02f
                target.moveTo(cx, bottomTipY)
                target.cubicTo(
                    left - s * 0.08f, top + s * 0.64f,
                    left - s * 0.02f, lobeTopY,
                    cx, topDipY
                )
                target.cubicTo(
                    right + s * 0.02f, lobeTopY,
                    right + s * 0.08f, top + s * 0.64f,
                    cx, bottomTipY
                )
                target.close()
            }
            else -> target.addRoundRect(rect, s * 0.30f, s * 0.30f, Path.Direction.CW)
        }
    }

    private fun updateShaders() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val (topCol, botCol) = TileThemePalette.getTopBottomColors(glassThemeKey)
        val alphaBoost = if (isFocusedState) 38 else 0
        val gTop = LauncherUtils.withAlpha(topCol, (Color.alpha(topCol) + alphaBoost).coerceAtMost(255))
        val gBot = LauncherUtils.withAlpha(botCol, (Color.alpha(botCol) + alphaBoost).coerceAtMost(255))

        glassPaint.shader = LinearGradient(0f, 0f, 0f, h, gTop, gBot, Shader.TileMode.CLAMP)
        glossPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.55f,
            Color.argb(if (isFocusedState) 82 else 48, 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        val strokeWidthPx = LauncherUtils.dpF(context, if (isFocusedState) 3.0f else 1.4f)
        borderPaint.strokeWidth = strokeWidthPx
        borderPaint.color = if (isFocusedState) {
            Color.WHITE
        } else {
            LauncherUtils.withAlpha(accentColor, 155)
        }

        val auraAlpha = if (isFocusedState) (110 + (glowPulse * 85f)).toInt() else 0
        val glowCol = if (glowPulse > 0.5f) appDominantColor else accentColor
        auraPaint.shader = if (auraAlpha > 0) {
            RadialGradient(
                w * 0.5f, h * 0.5f, min(w, h) * 0.64f,
                intArrayOf(LauncherUtils.withAlpha(glowCol, auraAlpha), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        } else null
    }

    override fun dispatchDraw(canvas: Canvas) {
        rebuildPathsIfNeeded(width, height)
        if (shapePath.isEmpty) {
            super.dispatchDraw(canvas)
            return
        }

        if (isFocusedState && auraPaint.shader != null) {
            canvas.drawPath(auraPath, auraPaint)
        }

        val saveCount = canvas.save()
        canvas.clipPath(shapePath)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glassPaint)
        super.dispatchDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat() * 0.55f, glossPaint)
        canvas.restoreToCount(saveCount)

        canvas.drawPath(borderPath, borderPaint)
    }
}
