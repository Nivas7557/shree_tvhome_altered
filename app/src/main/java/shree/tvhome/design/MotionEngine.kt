package shree.tvhome.design

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import shree.tvhome.source.LauncherPreferences

object MotionEngine {

    val OPTIONS: List<Pair<String, String>>
        get() = LauncherPreferences.MOTION_OPTIONS.map { it.key to it.label }

    fun getInterpolator(style: String): Interpolator = when (style) {
        LauncherPreferences.MOTION_JELLY -> OvershootInterpolator(2.3f)
        LauncherPreferences.MOTION_FLIP -> PathInterpolator(0.2f, 0.9f, 0.3f, 1.0f)
        LauncherPreferences.MOTION_BOOK -> PathInterpolator(0.25f, 1.0f, 0.35f, 1.0f)
        LauncherPreferences.MOTION_FLOAT -> PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f)
        LauncherPreferences.MOTION_SILK -> AccelerateDecelerateInterpolator()
        LauncherPreferences.MOTION_SWING -> OvershootInterpolator(1.6f)
        else -> OvershootInterpolator(1.85f)
    }

    fun getDuration(style: String, isFocusIn: Boolean): Long = when (style) {
        LauncherPreferences.MOTION_JELLY -> if (isFocusIn) 340L else 220L
        LauncherPreferences.MOTION_FLIP -> if (isFocusIn) 360L else 240L
        LauncherPreferences.MOTION_BOOK -> if (isFocusIn) 350L else 230L
        LauncherPreferences.MOTION_FLOAT -> if (isFocusIn) 320L else 210L
        LauncherPreferences.MOTION_SILK -> if (isFocusIn) 300L else 220L
        LauncherPreferences.MOTION_SWING -> if (isFocusIn) 350L else 220L
        else -> if (isFocusIn) 290L else 190L
    }

    fun animateFocus(
        v: View,
        focused: Boolean,
        style: String,
        maxScale: Float = 1.08f,
        liftDp: Float = 6f,
        dirX: Float = 0f,
        dirY: Float = 0f
    ) {
        v.animate().cancel()
        val liftPx = LauncherUtils.dpF(v.context, liftDp)
        val dur = getDuration(style, focused)
        val interp = getInterpolator(style)

        if (focused) {
            v.bringToFront()
            v.parent?.requestLayout()
            when (style) {
                LauncherPreferences.MOTION_FLIP -> {
                    v.pivotX = v.width * 0.5f
                    v.pivotY = v.height * 0.5f
                    val tiltY = (-dirX * 14f).coerceIn(-16f, 16f)
                    val tiltX = (dirY * 10f).coerceIn(-12f, 12f)
                    v.animate()
                        .scaleX(maxScale).scaleY(maxScale)
                        .rotationY(tiltY).rotationX(tiltX)
                        .translationY(-liftPx * 0.65f)
                        .translationZ(liftPx)
                        .setDuration(dur).setInterpolator(interp).start()
                }
                LauncherPreferences.MOTION_BOOK -> {
                    v.pivotX = if (dirX < 0f) v.width.toFloat() else 0f
                    v.pivotY = v.height * 0.5f
                    val pageAngle = if (dirX < 0f) 11f else -11f
                    v.animate()
                        .scaleX(maxScale).scaleY(maxScale)
                        .rotationY(pageAngle)
                        .translationY(-liftPx * 0.6f)
                        .translationZ(liftPx)
                        .setDuration(dur).setInterpolator(interp).start()
                }
                LauncherPreferences.MOTION_JELLY -> {
                    v.pivotX = v.width * 0.5f
                    v.pivotY = v.height * 0.5f
                    val stretchX = maxScale * (1f + 0.08f * kotlin.math.abs(dirX) - 0.05f * kotlin.math.abs(dirY))
                    val stretchY = maxScale * (1f + 0.08f * kotlin.math.abs(dirY) - 0.05f * kotlin.math.abs(dirX))
                    v.animate()
                        .scaleX(stretchX).scaleY(stretchY)
                        .rotation(dirX * 3.5f)
                        .translationY(-liftPx * 0.85f)
                        .translationZ(liftPx)
                        .setDuration(dur).setInterpolator(interp).start()
                }
                LauncherPreferences.MOTION_FLOAT -> {
                    v.pivotX = v.width * 0.5f
                    v.pivotY = v.height * 0.5f
                    v.animate()
                        .scaleX(maxScale * 1.02f).scaleY(maxScale * 1.02f)
                        .translationY(-liftPx * 1.25f)
                        .translationX(dirX * liftPx * 0.25f)
                        .rotation(0f).rotationX(0f).rotationY(0f)
                        .translationZ(liftPx * 1.3f)
                        .setDuration(dur).setInterpolator(interp).start()
                }
                LauncherPreferences.MOTION_SWING -> {
                    v.pivotX = v.width * 0.5f
                    v.pivotY = 0f
                    val swingAngle = (-dirX * 7.5f).coerceIn(-8.5f, 8.5f)
                    v.animate()
                        .scaleX(maxScale).scaleY(maxScale)
                        .rotation(swingAngle)
                        .translationY(-liftPx * 0.55f)
                        .translationZ(liftPx)
                        .setDuration(dur).setInterpolator(interp).start()
                }
                LauncherPreferences.MOTION_SILK -> {
                    v.pivotX = v.width * 0.5f
                    v.pivotY = v.height * 0.5f
                    v.animate()
                        .scaleX(maxScale).scaleY(maxScale)
                        .translationY(-liftPx * 0.7f)
                        .rotation(0f).rotationX(0f).rotationY(0f)
                        .translationZ(liftPx)
                        .setDuration(dur).setInterpolator(interp).start()
                }
                else -> {
                    v.pivotX = v.width * 0.5f
                    v.pivotY = v.height * 0.5f
                    v.animate()
                        .scaleX(maxScale).scaleY(maxScale)
                        .translationY(-liftPx * 0.8f)
                        .rotation(0f).rotationX(0f).rotationY(0f)
                        .translationZ(liftPx)
                        .setDuration(dur).setInterpolator(interp).start()
                }
            }
        } else {
            v.animate()
                .scaleX(1f).scaleY(1f)
                .translationX(0f).translationY(0f)
                .rotation(0f).rotationX(0f).rotationY(0f)
                .translationZ(0f)
                .setDuration(dur).setInterpolator(DecelerateInterpolator(1.6f)).start()
        }
    }

    fun animatePanelEnter(panel: View, stageWidget: View?, drawerWidthPx: Int, style: String, onEnd: () -> Unit) {
        panel.animate().cancel()
        stageWidget?.animate()?.cancel()
        val slideDist = (drawerWidthPx + LauncherUtils.dp(panel.context, 24)).toFloat()

        panel.alpha = 1f
        panel.translationX = slideDist
        panel.translationY = 0f
        panel.rotation = 0f
        panel.rotationX = 0f
        panel.rotationY = 0f
        panel.scaleX = 1f
        panel.scaleY = 1f
        panel.pivotX = panel.width * 0.5f
        panel.pivotY = panel.height * 0.5f

        val panelAnim = panel.animate()
            .translationX(0f)
            .setDuration(300L)
            .setInterpolator(PathInterpolator(0.2f, 0.9f, 0.3f, 1.0f))

        if (stageWidget != null) {
            stageWidget.alpha = 1f
            stageWidget.animate()
                .alpha(0f)
                .translationX(slideDist * 0.35f)
                .setDuration(220L)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }

        panelAnim.withEndAction { onEnd() }.start()
    }

    fun animatePanelExit(panel: View, stageWidget: View?, outDistPx: Int, style: String, onEnd: () -> Unit) {
        panel.animate().cancel()
        stageWidget?.animate()?.cancel()
        val slideOut = outDistPx.toFloat().coerceAtLeast(320f)

        panel.animate()
            .translationX(slideOut)
            .setDuration(240L)
            .setInterpolator(AnticipateInterpolator(1.1f))
            .withEndAction {
                resetTransform(panel)
                if (stageWidget != null) {
                    stageWidget.visibility = View.VISIBLE
                    stageWidget.translationX = slideOut * 0.25f
                    stageWidget.alpha = 0f
                    stageWidget.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(280L)
                        .setInterpolator(PathInterpolator(0.2f, 0.9f, 0.3f, 1.0f))
                        .start()
                }
                onEnd()
            }.start()
    }

    fun resetTransform(v: View) {
        v.animate().cancel()
        v.alpha = 1f
        v.translationX = 0f
        v.translationY = 0f
        v.translationZ = 0f
        v.rotation = 0f
        v.rotationX = 0f
        v.rotationY = 0f
        v.scaleX = 1f
        v.scaleY = 1f
    }

    fun playDemoPreview(target: View, style: String) {
        target.animate().cancel()
        resetTransform(target)
        val lift = LauncherUtils.dpF(target.context, 10f)
        when (style) {
            LauncherPreferences.MOTION_FLIP -> {
                target.pivotX = target.width * 0.5f
                target.pivotY = target.height * 0.5f
                val a1 = ObjectAnimator.ofFloat(target, View.ROTATION_Y, 0f, -14f, 10f, 0f)
                val a2 = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.04f, 1f)
                AnimatorSet().apply { playTogether(a1, a2); duration = 420L; start() }
            }
            LauncherPreferences.MOTION_BOOK -> {
                target.pivotX = 0f
                target.pivotY = target.height * 0.5f
                ObjectAnimator.ofFloat(target, View.ROTATION_Y, 0f, -12f, 0f).apply {
                    duration = 400L; interpolator = OvershootInterpolator(1.6f); start()
                }
            }
            LauncherPreferences.MOTION_JELLY -> {
                target.pivotX = target.width * 0.5f
                target.pivotY = target.height * 0.5f
                val sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f, 0.96f, 1.03f, 1f)
                val sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.94f, 1.06f, 0.98f, 1f)
                ObjectAnimator.ofPropertyValuesHolder(target, sx, sy).apply {
                    duration = 460L; start()
                }
            }
            LauncherPreferences.MOTION_FLOAT -> {
                val ty = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -lift, 0f)
                val sc = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.03f, 1f)
                val scY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.03f, 1f)
                AnimatorSet().apply { playTogether(ty, sc, scY); duration = 420L; interpolator = AccelerateDecelerateInterpolator(); start() }
            }
            LauncherPreferences.MOTION_SWING -> {
                target.pivotX = target.width * 0.5f
                target.pivotY = 0f
                ObjectAnimator.ofFloat(target, View.ROTATION, 0f, -7f, 6f, -3f, 0f).apply {
                    duration = 450L; start()
                }
            }
            LauncherPreferences.MOTION_SILK -> {
                ObjectAnimator.ofFloat(target, View.TRANSLATION_X, 0f, -lift * 0.6f, 0f).apply {
                    duration = 380L; interpolator = AccelerateDecelerateInterpolator(); start()
                }
            }
            else -> {
                val sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f, 1f)
                val sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f, 1f)
                ObjectAnimator.ofPropertyValuesHolder(target, sx, sy).apply {
                    duration = 340L; interpolator = OvershootInterpolator(2.0f); start()
                }
            }
        }
    }
}
