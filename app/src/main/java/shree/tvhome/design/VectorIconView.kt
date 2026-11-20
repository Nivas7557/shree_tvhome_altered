package shree.tvhome.design

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class VectorIconView(context: Context) : View(context) {

    enum class IconKind {
        WIFI_ON, WIFI_OFF, WIFI_SCAN,
        BELL, POWER, SPARKLES, SLIDERS,
        TV_INPUT, DISPLAY, SOUND, BLUETOOTH, STORAGE,
        WALLPAPER, REFRESH, BACK
    }

    private var kind: IconKind = IconKind.SPARKLES
    private var tintColor: Int = Color.WHITE

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val path = Path()

    fun setIcon(iconKind: IconKind, color: Int = Color.WHITE) {
        kind = iconKind
        tintColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val s = min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val sw = (s * 0.095f).coerceAtLeast(1.8f)

        strokePaint.color = tintColor
        strokePaint.strokeWidth = sw
        fillPaint.color = tintColor
        path.reset()

        when (kind) {
            IconKind.WIFI_ON, IconKind.WIFI_SCAN -> {
                val baseY = cy + s * 0.24f
                for (i in 1..3) {
                    val r = s * (0.14f * i)
                    val arc = RectF(cx - r, baseY - r, cx + r, baseY + r)
                    canvas.drawArc(arc, -135f, 90f, false, strokePaint)
                }
                canvas.drawCircle(cx, baseY, sw * 0.85f, fillPaint)
            }
            IconKind.WIFI_OFF -> {
                val baseY = cy + s * 0.24f
                for (i in 1..2) {
                    val r = s * (0.16f * i)
                    val arc = RectF(cx - r, baseY - r, cx + r, baseY + r)
                    canvas.drawArc(arc, -135f, 90f, false, strokePaint)
                }
                canvas.drawCircle(cx, baseY, sw * 0.8f, fillPaint)
                canvas.drawLine(cx - s * 0.34f, cy - s * 0.30f, cx + s * 0.34f, cy + s * 0.30f, strokePaint)
            }
            IconKind.BELL -> {
                val bellW = s * 0.28f
                val topY = cy - s * 0.24f
                val botY = cy + s * 0.16f
                path.moveTo(cx - bellW, botY)
                path.quadTo(cx - bellW, topY, cx, topY)
                path.quadTo(cx + bellW, topY, cx + bellW, botY)
                path.lineTo(cx + bellW * 1.2f, botY + sw * 0.6f)
                path.lineTo(cx - bellW * 1.2f, botY + sw * 0.6f)
                path.close()
                canvas.drawPath(path, strokePaint)
                canvas.drawCircle(cx, botY + s * 0.14f, sw * 0.85f, fillPaint)
            }
            IconKind.POWER -> {
                val r = s * 0.30f
                canvas.drawArc(RectF(cx - r, cy - r + s * 0.04f, cx + r, cy + r + s * 0.04f), -55f, 290f, false, strokePaint)
                canvas.drawLine(cx, cy - s * 0.32f, cx, cy - s * 0.04f, strokePaint)
            }
            IconKind.SPARKLES -> {
                drawStar4(canvas, cx - s * 0.05f, cy, s * 0.34f, s * 0.11f)
                drawStar4(canvas, cx + s * 0.26f, cy - s * 0.22f, s * 0.14f, s * 0.05f)
            }
            IconKind.SLIDERS -> {
                val ys = floatArrayOf(cy - s * 0.22f, cy, cy + s * 0.22f)
                val xs = floatArrayOf(cx - s * 0.12f, cx + s * 0.14f, cx - s * 0.06f)
                for (i in 0..2) {
                    canvas.drawLine(cx - s * 0.34f, ys[i], cx + s * 0.34f, ys[i], strokePaint)
                    canvas.drawCircle(xs[i], ys[i], sw * 1.05f, fillPaint)
                }
            }
            IconKind.TV_INPUT -> {
                val box = RectF(cx - s * 0.34f, cy - s * 0.24f, cx + s * 0.34f, cy + s * 0.18f)
                canvas.drawRoundRect(box, sw, sw, strokePaint)
                canvas.drawLine(cx - s * 0.14f, cy + s * 0.30f, cx + s * 0.14f, cy + s * 0.30f, strokePaint)
                canvas.drawLine(cx, cy + s * 0.18f, cx, cy + s * 0.30f, strokePaint)
            }
            IconKind.DISPLAY -> {
                val r = s * 0.16f
                canvas.drawCircle(cx, cy, r, strokePaint)
                for (k in 0 until 8) {
                    val ang = (k * PI / 4.0).toFloat()
                    val x1 = cx + cos(ang) * (r + sw * 0.8f)
                    val y1 = cy + sin(ang) * (r + sw * 0.8f)
                    val x2 = cx + cos(ang) * (s * 0.34f)
                    val y2 = cy + sin(ang) * (s * 0.34f)
                    canvas.drawLine(x1, y1, x2, y2, strokePaint)
                }
            }
            IconKind.SOUND -> {
                path.moveTo(cx - s * 0.28f, cy - s * 0.12f)
                path.lineTo(cx - s * 0.10f, cy - s * 0.12f)
                path.lineTo(cx + s * 0.10f, cy - s * 0.28f)
                path.lineTo(cx + s * 0.10f, cy + s * 0.28f)
                path.lineTo(cx - s * 0.10f, cy + s * 0.12f)
                path.lineTo(cx - s * 0.28f, cy + s * 0.12f)
                path.close()
                canvas.drawPath(path, strokePaint)
                canvas.drawArc(RectF(cx + s * 0.04f, cy - s * 0.18f, cx + s * 0.32f, cy + s * 0.18f), -50f, 100f, false, strokePaint)
            }
            IconKind.BLUETOOTH -> {
                path.moveTo(cx - s * 0.16f, cy - s * 0.16f)
                path.lineTo(cx + s * 0.16f, cy + s * 0.14f)
                path.lineTo(cx, cy + s * 0.32f)
                path.lineTo(cx, cy - s * 0.32f)
                path.lineTo(cx + s * 0.16f, cy - s * 0.14f)
                path.lineTo(cx - s * 0.16f, cy + s * 0.16f)
                canvas.drawPath(path, strokePaint)
            }
            IconKind.STORAGE -> {
                for (i in 0..1) {
                    val topY = if (i == 0) cy - s * 0.28f else cy + s * 0.02f
                    canvas.drawRoundRect(
                        RectF(cx - s * 0.32f, topY, cx + s * 0.32f, topY + s * 0.24f),
                        sw, sw, strokePaint
                    )
                    canvas.drawCircle(cx + s * 0.18f, topY + s * 0.12f, sw * 0.65f, fillPaint)
                }
            }
            IconKind.WALLPAPER -> {
                canvas.drawRoundRect(RectF(cx - s * 0.32f, cy - s * 0.26f, cx + s * 0.32f, cy + s * 0.26f), sw, sw, strokePaint)
                canvas.drawCircle(cx - s * 0.12f, cy - s * 0.08f, sw * 0.85f, fillPaint)
                path.moveTo(cx - s * 0.26f, cy + s * 0.18f)
                path.lineTo(cx - s * 0.04f, cy + s * 0.02f)
                path.lineTo(cx + s * 0.10f, cy + s * 0.12f)
                path.lineTo(cx + s * 0.26f, cy - s * 0.04f)
                canvas.drawPath(path, strokePaint)
            }
            IconKind.REFRESH -> {
                val r = s * 0.28f
                canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 30f, 290f, false, strokePaint)
            }
            IconKind.BACK -> {
                path.moveTo(cx + s * 0.12f, cy - s * 0.24f)
                path.lineTo(cx - s * 0.14f, cy)
                path.lineTo(cx + s * 0.12f, cy + s * 0.24f)
                canvas.drawPath(path, strokePaint)
            }
        }
    }

    private fun drawStar4(canvas: Canvas, x: Float, y: Float, outerR: Float, innerR: Float) {
        val p = Path()
        for (i in 0 until 8) {
            val r = if (i % 2 == 0) outerR else innerR
            val ang = (i * PI / 4.0) - (PI / 2.0)
            val px = x + (cos(ang) * r).toFloat()
            val py = y + (sin(ang) * r).toFloat()
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        p.close()
        canvas.drawPath(p, fillPaint)
    }
}
