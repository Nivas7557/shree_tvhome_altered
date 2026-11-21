package shree.tvhome.panels

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.design.LauncherUtils

object AppActionDialog {

    fun show(
        ctx: Context,
        pkg: String,
        appName: String,
        isFav: Boolean,
        canMoveLeft: Boolean,
        canMoveRight: Boolean,
        accent: Int,
        isHiddenMode: Boolean,
        onToggleFav: () -> Unit,
        onMoveFav: (Int) -> Unit,
        onHideOrUnhideApp: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 20), dp(ctx, 18), dp(ctx, 20), dp(ctx, 18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(26, 36, 72), Color.rgb(14, 20, 44))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 22f)
                setStroke(dp(ctx, 2), LauncherUtils.withAlpha(accent, 185))
            }
        }

        root.addView(TextView(ctx).apply {
            text = appName
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        root.addView(TextView(ctx).apply {
            text = pkg
            textSize = 9.5f
            setTextColor(Color.argb(175, 195, 215, 240))
            setPadding(0, dp(ctx, 2), 0, dp(ctx, 12))
        })

        fun addOption(label: String, tint: Int, action: () -> Unit) {
            val btn = TextView(ctx).apply {
                text = label
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                isFocusable = true
                isClickable = true
                setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10))
                background = GradientDrawable().apply {
                    setColor(LauncherUtils.withAlpha(tint, 75))
                    cornerRadius = LauncherUtils.dpF(ctx, 13f)
                    setStroke(dp(ctx, 1), LauncherUtils.withAlpha(tint, 165))
                }
                setOnClickListener {
                    dialog.dismiss()
                    action()
                }
            }
            root.addView(btn, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })
        }

        if (!isHiddenMode) {
            addOption(
                if (isFav) "♡  Remove from Favorites" else "♥  Add to Favorites",
                Color.rgb(255, 110, 145),
                onToggleFav
            )
            if (canMoveLeft) {
                addOption("‹  Move Left in Favorites", accent) { onMoveFav(-1) }
            }
            if (canMoveRight) {
                addOption("›  Move Right in Favorites", accent) { onMoveFav(1) }
            }
            addOption("👁  Hide App from Home", Color.rgb(185, 135, 255), onHideOrUnhideApp)
        } else {
            addOption("👁  Unhide App to Home", Color.rgb(46, 232, 138), onHideOrUnhideApp)
        }

        addOption("ℹ  App Info & Permissions", Color.rgb(92, 184, 255)) {
            try {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
            } catch (_: Exception) {}
        }

        dialog.setView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(dp(ctx, 360), LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
