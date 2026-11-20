package shree.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.design.LauncherUtils
import shree.tvhome.source.LauncherPreferences
import java.util.Calendar

class GreetingShineCard(
    context: Context,
    private val lp: LauncherPreferences
) : LinearLayout(context) {

    private val greetingTv: TextView
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipRect = RectF()
    private var shineProgress = 0.25f
    private var shineAnimator: ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setWillNotDraw(false)
        setPadding(dp(14), dp(6), dp(14), dp(6))
        background = cardBg()

        greetingTv = TextView(context).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isSingleLine = true
        }
        addView(greetingTv)
        updateGreeting()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShine()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        shineAnimator?.cancel()
        shineAnimator = null
    }

    private fun startShine() {
        shineAnimator?.cancel()
        shineAnimator = ValueAnimator.ofFloat(-0.4f, 1.4f).apply {
            duration = 3800L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                shineProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val autoGreet = when (hour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..20 -> "Good evening,"
            else -> "Good night,"
        }
        val phrase = lp.greetingPhrase.trim()
        val finalGreet = when {
            phrase.isEmpty() || phrase == "auto" -> autoGreet
            else -> phrase
        }
        val user = lp.userName.trim().ifEmpty { "Shree" }
        greetingTv.text = "$finalGreet $user"
        updateAccent()
    }

    fun updateAccent() {
        background = cardBg()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRect.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val bandW = w * 0.45f
        val startX = shineProgress * w
        shinePaint.shader = LinearGradient(
            startX - bandW, 0f, startX + bandW, h,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(46, 255, 255, 255),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(clipRect, dp(16).toFloat(), dp(16).toFloat(), shinePaint)
    }

    private fun cardBg() = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(
            LauncherUtils.withAlpha(lp.accentColor, 82),
            Color.argb(62, 24, 34, 72)
        )
    ).apply {
        cornerRadius = LauncherUtils.dpF(context, 16f)
        setStroke(dp(1), LauncherUtils.withAlpha(lp.accentColor, 160))
    }

    private fun dp(v: Int) = LauncherUtils.dp(context, v)
}
