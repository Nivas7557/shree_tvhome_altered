package shree.tvhome.panels

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.source.LauncherPreferences
import java.util.Locale

object ProfileCropDialog {

    private val GREETING_PRESETS = listOf(
        "auto" to "Smart Time",
        "Welcome back," to "Welcome",
        "TV Time," to "TV Time",
        "Enjoy the show," to "Show Time",
        "Relax & stream," to "Chill Mode",
        "Namaskaram," to "Namaskaram"
    )

    fun show(
        ctx: Context,
        lp: LauncherPreferences,
        onPickPhoto: () -> Unit,
        onSaved: () -> Unit
    ) {
        var workingZoom = lp.prefs.getFloat("profile_pic_zoom", 1.0f).coerceIn(1.0f, 3.0f)
        var workingPanX = lp.prefs.getFloat("profile_pic_pan_x", 0f).coerceIn(-1f, 1f)
        var workingPanY = lp.prefs.getFloat("profile_pic_pan_y", 0f).coerceIn(-1f, 1f)
        var selectedGreeting = lp.greetingPhrase

        val dialog = AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 20), dp(ctx, 18), dp(ctx, 20), dp(ctx, 18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(24, 34, 68), Color.rgb(13, 19, 42))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 22f)
                setStroke(dp(ctx, 2), LauncherUtils.withAlpha(lp.accentColor, 180))
            }
        }

        card.addView(TextView(ctx).apply {
            text = "PROFILE & AVATAR STUDIO"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })

        val scroll = ScrollView(ctx).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(ctx, 8), 0, dp(ctx, 8))
        }

        addLabel(ctx, body, lp.accentColor, "YOUR NAME")
        val nameInput = EditText(ctx).apply {
            setText(lp.userName)
            hint = "Enter display name"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(130, 200, 220, 245))
            textSize = 12f
            isSingleLine = true
            setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
            background = GradientDrawable().apply {
                setColor(Color.argb(55, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 12f)
                setStroke(dp(ctx, 1), Color.argb(120, 255, 255, 255))
            }
        }
        body.addView(nameInput, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        addLabel(ctx, body, lp.accentColor, "GREETING PHRASE")
        val greetingBtns = ArrayList<Pair<String, TextView>>()
        GREETING_PRESETS.chunked(3).forEach { rowItems ->
            val r = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            rowItems.forEachIndexed { idx, (phraseVal, shortLbl) ->
                val active = selectedGreeting == phraseVal
                val btn = TextView(ctx).apply {
                    text = shortLbl
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    isFocusable = true
                    isClickable = true
                    background = chipBg(ctx, lp.accentColor, active)
                    setOnFocusChangeListener { v, f ->
                        MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 3f)
                        v.background = chipBg(ctx, lp.accentColor, f || selectedGreeting == phraseVal)
                    }
                    setOnClickListener {
                        selectedGreeting = phraseVal
                        greetingBtns.forEach { (k, b) ->
                            b.background = chipBg(ctx, lp.accentColor, b.hasFocus() || k == selectedGreeting)
                        }
                    }
                }
                greetingBtns.add(phraseVal to btn)
                r.addView(btn, LinearLayout.LayoutParams(0, dp(ctx, 34), 1f).apply {
                    if (idx < rowItems.size - 1) rightMargin = dp(ctx, 6)
                })
            }
            body.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }

        addLabel(ctx, body, lp.accentColor, "AVATAR PHOTO FRAMING")
        val pickBtn = TextView(ctx).apply {
            text = "🖼  Pick Photo from Gallery"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = chipBg(ctx, lp.accentColor, false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f)
                v.background = chipBg(ctx, lp.accentColor, f)
            }
            setOnClickListener {
                dialog.dismiss()
                onPickPhoto()
            }
        }
        body.addView(pickBtn, LinearLayout.LayoutParams(-1, dp(ctx, 38)).apply { bottomMargin = dp(ctx, 8) })

        val zoomPctTv = TextView(ctx).apply {
            text = "Zoom: ${(workingZoom * 100).toInt()}%"
            textSize = 10f
            setTextColor(Color.WHITE)
        }
        body.addView(zoomPctTv)
        val zoomSeek = SeekBar(ctx).apply {
            max = 200
            progress = ((workingZoom - 1.0f) * 100f).toInt().coerceIn(0, 200)
            isFocusable = true
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    workingZoom = 1.0f + (p / 100f)
                    zoomPctTv.text = "Zoom: ${(workingZoom * 100).toInt()}%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        body.addView(zoomSeek, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        val panXTv = TextView(ctx).apply {
            text = "Horizontal Pan: ${(workingPanX * 100).toInt()}%"
            textSize = 10f
            setTextColor(Color.WHITE)
        }
        body.addView(panXTv)
        val panXSeek = SeekBar(ctx).apply {
            max = 200
            progress = ((workingPanX + 1.0f) * 100f).toInt().coerceIn(0, 200)
            isFocusable = true
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    workingPanX = ((p - 100) / 100f).coerceIn(-1f, 1f)
                    panXTv.text = "Horizontal Pan: ${(workingPanX * 100).toInt()}%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        body.addView(panXSeek, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        val panYTv = TextView(ctx).apply {
            text = "Vertical Pan: ${(workingPanY * 100).toInt()}%"
            textSize = 10f
            setTextColor(Color.WHITE)
        }
        body.addView(panYTv)
        val panYSeek = SeekBar(ctx).apply {
            max = 200
            progress = ((workingPanY + 1.0f) * 100f).toInt().coerceIn(0, 200)
            isFocusable = true
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    workingPanY = ((p - 100) / 100f).coerceIn(-1f, 1f)
                    panYTv.text = "Vertical Pan: ${(workingPanY * 100).toInt()}%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        body.addView(panYSeek, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        scroll.addView(body)
        card.addView(scroll, LinearLayout.LayoutParams(-1, dp(ctx, 290)))

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(ctx, 10), 0, 0)
        }
        val resetBtn = TextView(ctx).apply {
            text = "Reset"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = chipBg(ctx, Color.rgb(255, 110, 145), false)
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 3f) }
            setOnClickListener {
                workingZoom = 1.0f
                workingPanX = 0f
                workingPanY = 0f
                selectedGreeting = ""
                zoomSeek.progress = 0
                panXSeek.progress = 100
                panYSeek.progress = 100
                zoomPctTv.text = "Zoom: 100%"
                panXTv.text = "Horizontal Pan: 0%"
                panYTv.text = "Vertical Pan: 0%"
                greetingBtns.forEach { (k, b) -> b.background = chipBg(ctx, lp.accentColor, k == selectedGreeting) }
            }
        }
        btnRow.addView(resetBtn, LinearLayout.LayoutParams(0, dp(ctx, 38), 1f).apply { rightMargin = dp(ctx, 8) })

        val saveBtn = TextView(ctx).apply {
            text = "Save"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = chipBg(ctx, lp.accentColor, true)
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 3f) }
            setOnClickListener {
                lp.userName = nameInput.text?.toString()?.trim().orEmpty().ifBlank { "Shree" }
                lp.greetingPhrase = selectedGreeting
                lp.prefs.edit()
                    .putFloat("profile_pic_zoom", workingZoom)
                    .putFloat("profile_pic_pan_x", workingPanX)
                    .putFloat("profile_pic_pan_y", workingPanY)
                    .apply()
                dialog.dismiss()
                onSaved()
            }
        }
        btnRow.addView(saveBtn, LinearLayout.LayoutParams(0, dp(ctx, 38), 1.4f))
        card.addView(btnRow)

        dialog.setView(card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(dp(ctx, 440), LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun addLabel(ctx: Context, parent: LinearLayout, accent: Int, title: String) {
        parent.addView(TextView(ctx).apply {
            text = title.uppercase(Locale.ROOT)
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 6); bottomMargin = dp(ctx, 4) })
    }

    private fun chipBg(ctx: Context, accent: Int, active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(accent, 215), LauncherUtils.withAlpha(accent, 135))
        else intArrayOf(Color.argb(65, 255, 255, 255), Color.argb(30, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 12f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) Color.WHITE else LauncherUtils.withAlpha(accent, 150))
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
