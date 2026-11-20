package shree.tvhome.design

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import shree.tvhome.source.LauncherPreferences

object LauncherUtils {
    val FLUID_SPRING = PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f)
    val SILKY_EASE = PathInterpolator(0.25f, 0.85f, 0.30f, 1.0f)

    fun dp(ctx: Context, value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), ctx.resources.displayMetrics)
            .toInt().coerceAtLeast(1)

    fun dpF(ctx: Context, value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, ctx.resources.displayMetrics)

    fun calculateAdaptiveScale(ctx: Context): Float {
        val dm = ctx.resources.displayMetrics
        val minDimPx = minOf(dm.widthPixels, dm.heightPixels)
        val scale = if (minDimPx > 0) (minDimPx / 1080f) else 1.0f
        return scale.coerceIn(0.65f, 1.45f)
    }

    fun queryLaunchableApps(pm: PackageManager): List<ResolveInfo> {
        val map = LinkedHashMap<String, ResolveInfo>()
        val intents = listOf(
            Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        )
        for (intent in intents) {
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            for (ri in resolved) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (!map.containsKey(pkg)) map[pkg] = ri
            }
        }
        return map.values.toList()
    }

    fun glassPanelDrawable(ctx: Context, focused: Boolean, accentColor: Int): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (focused) intArrayOf(Color.argb(125, 95, 130, 190), Color.argb(95, 45, 65, 115))
            else intArrayOf(Color.argb(72, 60, 82, 130), Color.argb(52, 30, 42, 74))
        ).apply {
            cornerRadius = dpF(ctx, 18f)
            setStroke(dp(ctx, if (focused) 2 else 1), if (focused) Color.WHITE else withAlpha(accentColor, 145))
        }

    fun glossOverlayDrawable(ctx: Context, cornerRadiusDp: Float = 20f): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(46, 255, 255, 255), Color.argb(10, 255, 255, 255), Color.TRANSPARENT)
        ).apply {
            cornerRadius = dpF(ctx, cornerRadiusDp)
        }

    fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    fun applyFocusSpring(v: View, focused: Boolean, lp: LauncherPreferences, maxScale: Float = 1.08f, liftDp: Float = 6f) {
        MotionEngine.animateFocus(v, focused, lp.motionStyle, maxScale, liftDp)
    }

    fun pulseSelection(v: View) {
        val sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.12f, 1.0f)
        val sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.12f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(v, sx, sy).apply {
            duration = 280L
            interpolator = OvershootInterpolator(2.2f)
            start()
        }
    }
}
