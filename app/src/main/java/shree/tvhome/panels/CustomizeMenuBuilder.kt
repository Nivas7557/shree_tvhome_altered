package shree.tvhome.panels

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.TileThemePalette
import java.util.Locale

object CustomizeMenuBuilder {

    fun build(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        onLiveRefresh: () -> Unit,
        onEditProfile: () -> Unit,
        onOpenWallpaper: () -> Unit,
        onOpenHidden: () -> Unit,
        onCloseDrawer: () -> Unit
    ) {
        addHeader(ctx, panel, "ACCOUNT & VISIBILITY")
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        topRow.addView(
            makeActionTile(ctx, lp, lp.userName.ifBlank { "Shree" }, "Edit Profile & DP", LauncherPreferences.ACCENT_OPTIONS.first().color) {
                onEditProfile()
            },
            LinearLayout.LayoutParams(0, dp(ctx, 52), 1f).apply { rightMargin = dp(ctx, 6) }
        )

        val hiddenCount = lp.getHiddenApps().size
        topRow.addView(
            makeActionTile(ctx, lp, "Hidden Apps", "$hiddenCount hidden", LauncherPreferences.ACCENT_OPTIONS.getOrElse(2) { LauncherPreferences.ACCENT_OPTIONS.first() }.color) {
                onCloseDrawer()
                onOpenHidden()
            },
            LinearLayout.LayoutParams(0, dp(ctx, 52), 1f)
        )
        panel.addView(topRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        addActionCard(
            ctx, panel, lp, "🖼", "Wallpaper & Live Effects",
            "Gallery photos, weather aura & live scenes",
            LauncherPreferences.ACCENT_OPTIONS.getOrElse(1) { LauncherPreferences.ACCENT_OPTIONS.first() }.color
        ) {
            onOpenWallpaper()
        }

        addHeader(ctx, panel, "TILE SIZE")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = LauncherPreferences.TILE_SIZE_OPTIONS.map { it.key to it.label },
            getSelected = { lp.tileSize },
            onSelect = { key ->
                lp.tileSize = key
                onLiveRefresh()
            }
        )

        addHeader(ctx, panel, "STAGE & PANEL WIDTH")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = LauncherPreferences.PANEL_WIDTH_OPTIONS.map { it.key to it.label },
            getSelected = { lp.panelWidthPct.toString() },
            onSelect = { key ->
                lp.panelWidthPct = key.toIntOrNull() ?: lp.panelWidthPct
                onLiveRefresh()
            }
        )

        addHeader(ctx, panel, "ICON SHAPE")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = LauncherPreferences.TILE_SHAPE_OPTIONS.map { it.key to it.label },
            getSelected = { lp.tileShape },
            onSelect = { key ->
                lp.tileShape = key
                onLiveRefresh()
            }
        )

        addHeader(ctx, panel, "GLOBAL MOTION STYLE (TAP TO PREVIEW)")
        val allMotionButtons = ArrayList<Pair<String, TextView>>()
        MotionEngine.OPTIONS.chunked(4).forEach { rowItems ->
            val r = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            rowItems.forEachIndexed { idx, (key, label) ->
                val isSel = lp.motionStyle == key
                val btn = TextView(ctx).apply {
                    text = label
                    textSize = 9.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    isFocusable = true
                    isClickable = true
                    background = chipBg(ctx, lp.accentColor, isSel)
                    setOnFocusChangeListener { v, f ->
                        MotionEngine.animateFocus(v, f, lp.motionStyle, 1.05f, 4f)
                        v.background = chipBg(ctx, lp.accentColor, f || lp.motionStyle == key)
                    }
                    setOnClickListener {
                        lp.motionStyle = key
                        allMotionButtons.forEach { (k, b) ->
                            b.background = chipBg(ctx, lp.accentColor, b.hasFocus() || k == lp.motionStyle)
                        }
                        MotionEngine.playDemoPreview(panel, key)
                    }
                }
                allMotionButtons.add(key to btn)
                r.addView(btn, LinearLayout.LayoutParams(0, dp(ctx, 34), 1f).apply {
                    if (idx < rowItems.size - 1) rightMargin = dp(ctx, 5)
                })
            }
            panel.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 5) })
        }

        addHeader(ctx, panel, "ACCENT AURA")
        val swatchRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        val swatchViews = ArrayList<Pair<Int, View>>()
        LauncherPreferences.ACCENT_OPTIONS.forEachIndexed { idx, opt ->
            val col = opt.color
            val isSel = lp.accentColor == col
            val dot = View(ctx).apply {
                isFocusable = true
                isClickable = true
                background = swatchBg(ctx, col, isSel)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.12f, 5f)
                    v.background = swatchBg(ctx, col, f || lp.accentColor == col)
                }
                setOnClickListener {
                    lp.accentColor = col
                    swatchViews.forEach { (c, v) -> v.background = swatchBg(ctx, c, v.hasFocus() || c == lp.accentColor) }
                    onLiveRefresh()
                }
            }
            swatchViews.add(col to dot)
            swatchRow.addView(dot, LinearLayout.LayoutParams(0, dp(ctx, 32), 1f).apply {
                if (idx < LauncherPreferences.ACCENT_OPTIONS.lastIndex) rightMargin = dp(ctx, 6)
            })
        }
        panel.addView(swatchRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        addHeader(ctx, panel, "TILE GLASS PALETTE")
        val themePairs = TileThemePalette.KEYS.mapIndexed { idx, key ->
            val name = TileThemePalette.NAMES.getOrNull(idx) ?: key
            key to name
        }
        themePairs.chunked(4).forEach { rowChunk ->
            addInPlaceChipRow(
                ctx, panel, lp,
                options = rowChunk,
                getSelected = { lp.tileColor },
                onSelect = { key ->
                    lp.tileColor = key
                    onLiveRefresh()
                }
            )
        }

        addHeader(ctx, panel, "FAVORITE BADGE")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = LauncherPreferences.BADGE_OPTIONS.map { it.key to it.label },
            getSelected = { lp.badgeMode },
            onSelect = { key ->
                lp.badgeMode = key
                onLiveRefresh()
            }
        )
    }

    private fun addInPlaceChipRow(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        options: List<Pair<String, String>>,
        getSelected: () -> String,
        onSelect: (String) -> Unit
    ) {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        val chips = ArrayList<Pair<String, TextView>>()
        options.forEachIndexed { idx, (key, label) ->
            val isSel = getSelected() == key
            val chip = TextView(ctx).apply {
                text = label
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                background = chipBg(ctx, lp.accentColor, isSel)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.05f, 4f)
                    v.background = chipBg(ctx, lp.accentColor, f || getSelected() == key)
                }
                setOnClickListener {
                    onSelect(key)
                    chips.forEach { (k, cView) ->
                        cView.background = chipBg(ctx, lp.accentColor, cView.hasFocus() || getSelected() == k)
                    }
                }
            }
            chips.add(key to chip)
            row.addView(chip, LinearLayout.LayoutParams(0, dp(ctx, 34), 1f).apply {
                if (idx < options.size - 1) rightMargin = dp(ctx, 5)
            })
        }
        panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 4) })
    }

    private fun makeActionTile(ctx: Context, lp: LauncherPreferences, title: String, sub: String, tint: Int, onClick: () -> Unit) =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
            isFocusable = true
            isClickable = true
            background = tintedCardBg(ctx, lp.accentColor, tint, false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 4f)
                v.background = tintedCardBg(ctx, lp.accentColor, tint, f)
            }
            setOnClickListener { onClick() }
            addView(TextView(ctx).apply { text = title; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); isSingleLine = true })
            addView(TextView(ctx).apply { text = sub; textSize = 8.2f; setTextColor(tint); isSingleLine = true })
        }

    private fun addActionCard(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, icon: String, title: String, sub: String, tint: Int, onClick: () -> Unit) {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
            isFocusable = true
            isClickable = true
            background = tintedCardBg(ctx, lp.accentColor, tint, false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                v.background = tintedCardBg(ctx, lp.accentColor, tint, f)
            }
            setOnClickListener { onClick() }
        }
        card.addView(TextView(ctx).apply { text = icon; textSize = 14f; setTextColor(tint); setPadding(0, 0, dp(ctx, 10), 0) })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(TextView(ctx).apply { text = title; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        mid.addView(TextView(ctx).apply { text = sub; textSize = 8.5f; setTextColor(Color.argb(195, 215, 232, 250)) })
        card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(TextView(ctx).apply { text = "›"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 4) })
    }

    private fun tintedCardBg(ctx: Context, accent: Int, tint: Int, focused: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (focused) intArrayOf(LauncherUtils.withAlpha(tint, 155), LauncherUtils.withAlpha(accent, 90))
        else intArrayOf(LauncherUtils.withAlpha(tint, 72), Color.argb(36, 20, 30, 66))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (focused) 2 else 1), if (focused) Color.WHITE else LauncherUtils.withAlpha(tint, 130))
    }

    private fun chipBg(ctx: Context, accent: Int, active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(accent, 215), LauncherUtils.withAlpha(accent, 135))
        else intArrayOf(Color.argb(62, 255, 255, 255), Color.argb(30, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 12f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) Color.WHITE else LauncherUtils.withAlpha(accent, 150))
    }

    private fun swatchBg(ctx: Context, col: Int, active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(col)
        setStroke(dp(ctx, if (active) 3 else 1), if (active) Color.WHITE else Color.argb(110, 255, 255, 255))
    }

    private fun addHeader(ctx: Context, panel: LinearLayout, label: String) {
        panel.addView(TextView(ctx).apply {
            text = label.uppercase(Locale.ROOT)
            textSize = 8.8f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(220, 200, 225, 255))
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 4) })
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
