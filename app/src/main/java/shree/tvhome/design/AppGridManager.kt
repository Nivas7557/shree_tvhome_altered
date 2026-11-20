package shree.tvhome.design

import android.content.Context
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.source.LauncherPreferences
import java.util.Locale

object AppGridManager {

    private var cachedApps: List<ResolveInfo>? = null

    fun clearCache() {
        cachedApps = null
        AppTileFactory.clearAssetCache()
    }

    private fun getApps(ctx: Context): List<ResolveInfo> {
        cachedApps?.let { return it }
        val pm = ctx.packageManager
        val list = LauncherUtils.queryLaunchableApps(pm)
            .sortedBy { AppTileFactory.getCachedLabel(pm, it).lowercase(Locale.ROOT) }
        cachedApps = list
        return list
    }

    fun createHeader(
        ctx: Context,
        title: String,
        isFav: Boolean,
        lp: LauncherPreferences? = null,
        onSortToggled: (() -> Unit)? = null
    ): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(ctx, 2), 0, dp(ctx, 6))

        addView(TextView(ctx).apply {
            text = title
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            letterSpacing = 0.05f
        })

        addView(TextView(ctx).apply {
            tag = "count_tv"
            text = ""
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(235, 220, 238, 255))
            setPadding(dp(ctx, 9), dp(ctx, 2), dp(ctx, 9), dp(ctx, 2))
            background = GradientDrawable().apply {
                setColor(Color.argb(65, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 12f)
                setStroke(dp(ctx, 1), Color.argb(115, 255, 255, 255))
            }
        }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(ctx, 10) })

        if (!isFav && lp != null && onSortToggled != null) {
            addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
            val sortBtn = TextView(ctx).apply {
                tag = "sort_btn"
                text = if (lp.sortOrder == "za") "⇅ Z-A ORDER" else "⇅ A-Z ORDER"
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                isFocusable = true
                isClickable = true
                setPadding(dp(ctx, 11), dp(ctx, 3), dp(ctx, 11), dp(ctx, 3))
                background = GradientDrawable().apply {
                    setColor(Color.argb(72, 255, 255, 255))
                    cornerRadius = LauncherUtils.dpF(ctx, 14f)
                    setStroke(dp(ctx, 1), Color.argb(130, 255, 255, 255))
                }
                setOnFocusChangeListener { v, f ->
                    LauncherUtils.applyFocusSpring(v, f, lp, 1.06f, 4f)
                }
                setOnClickListener {
                    lp.sortOrder = if (lp.sortOrder == "az") "za" else "az"
                    text = if (lp.sortOrder == "za") "⇅ Z-A ORDER" else "⇅ A-Z ORDER"
                    LauncherUtils.pulseSelection(this)
                    onSortToggled()
                }
            }
            addView(sortBtn, LinearLayout.LayoutParams(-2, -2))
        }
    }

    fun populateHiddenRibbon(
        ctx: Context,
        lp: LauncherPreferences,
        hiddenSection: LinearLayout,
        hiddenRow: LinearLayout,
        hiddenCountTv: TextView,
        onRestoreApp: (pkg: String, label: String) -> Unit
    ) {
        val pm = ctx.packageManager
        val hiddenSet = lp.getHiddenApps()
        val hiddenList = getApps(ctx).filter { hiddenSet.contains(it.activityInfo.packageName) }

        hiddenCountTv.text = "${hiddenList.size} Hidden"
        if (hiddenList.isEmpty()) {
            hiddenSection.visibility = View.GONE
            return
        }

        hiddenSection.visibility = View.VISIBLE
        hiddenRow.removeAllViews()

        val tileW = dp(ctx, 126)
        val tileH = dp(ctx, 104)
        val gap = dp(ctx, 10)

        val dummyActions = object : AppTileFactory.TileActions {
            override fun onToggleFavorite(pkg: String, label: String) {}
            override fun onMoveFavorite(pkg: String, delta: Int) {}
            override fun onHideApp(pkg: String, label: String) {
                onRestoreApp(pkg, label)
            }
        }

        hiddenList.forEachIndexed { idx, ri ->
            val pkg = ri.activityInfo.packageName
            val lbl = AppTileFactory.getCachedLabel(pm, ri)
            val tile = AppTileFactory.create(
                ctx, pm, lp, ri,
                isFavRow = false, isFav = lp.getFavorites().contains(pkg),
                canMoveLeft = false, canMoveRight = false,
                w = tileW, h = tileH, actions = dummyActions
            ).apply {
                alpha = 0.88f
                setOnClickListener { onRestoreApp(pkg, lbl) }
                setOnLongClickListener { onRestoreApp(pkg, lbl); true }
            }
            hiddenRow.addView(tile, LinearLayout.LayoutParams(tileW, tileH).apply {
                if (idx < hiddenList.size - 1) rightMargin = gap
            })
        }
        if (hiddenRow.childCount > 0) {
            hiddenRow.post { hiddenRow.getChildAt(0)?.requestFocus() }
        }
    }

    fun populate(
        ctx: Context, lp: LauncherPreferences,
        favSection: LinearLayout, favRow: LinearLayout, favCount: TextView,
        appGrid: LinearLayout, allCount: TextView,
        shouldRequestFocus: Boolean, restorePkg: String?, isDrawerOpen: Boolean,
        actions: AppTileFactory.TileActions
    ) {
        val pm = ctx.packageManager
        val hiddenSet = lp.getHiddenApps()
        val baseApps = getApps(ctx).filter { !hiddenSet.contains(it.activityInfo.packageName) }
        val allApps = if (lp.sortOrder == "za") baseApps.reversed() else baseApps

        val orderedFavPkgs = lp.getOrderedFavorites().filter { !hiddenSet.contains(it) }
        val favSet = orderedFavPkgs.toSet()
        val appByPkg = baseApps.associateBy { it.activityInfo.packageName }
        val favApps = orderedFavPkgs.mapNotNull { appByPkg[it] }

        val screenW = ctx.resources.displayMetrics.widthPixels
        val panelW = maxOf(dp(ctx, 250), (screenW * (lp.panelWidthPct / 100f)).toInt())
        val availW = (screenW - dp(ctx, 56) - panelW - dp(ctx, 16)).coerceAtLeast(dp(ctx, 320))

        val cols = when (lp.tileSize) {
            "large" -> (availW / dp(ctx, 152)).coerceIn(3, 6)
            "small" -> (availW / dp(ctx, 108)).coerceIn(5, 9)
            else -> (availW / dp(ctx, 128)).coerceIn(4, 7)
        }
        lp.columns = cols

        val gap = dp(ctx, if (cols >= 7) 8 else 12)
        val cellW = ((availW - gap * (cols - 1)) / cols).coerceAtLeast(dp(ctx, 84))
        val cellH = when (lp.tileSize) {
            "large" -> dp(ctx, 134)
            "small" -> dp(ctx, 102)
            else -> dp(ctx, 118)
        }

        favCount.text = "${favApps.size} Pinned"
        if (favApps.isEmpty()) {
            favSection.visibility = View.GONE
        } else {
            favSection.visibility = View.VISIBLE
            val favH = cellH + dp(ctx, 6)
            val desiredFavTags = favApps.map { "fav_${it.activityInfo.packageName}" }
            val currentFavTags = (0 until favRow.childCount).map { favRow.getChildAt(it).tag as? String }

            if (currentFavTags == desiredFavTags && favRow.childCount > 0) {
                favApps.forEachIndexed { idx, ri ->
                    val child = favRow.getChildAt(idx)
                    val lpParams = child.layoutParams as? LinearLayout.LayoutParams
                    if (lpParams != null && (lpParams.width != cellW || lpParams.height != favH)) {
                        lpParams.width = cellW
                        lpParams.height = favH
                        lpParams.rightMargin = if (idx < favApps.size - 1) gap else 0
                        child.layoutParams = lpParams
                    }
                    AppTileFactory.updateTileAppearance(ctx, child, lp, true)
                }
            } else {
                favRow.removeAllViews()
                favApps.forEachIndexed { idx, ri ->
                    val pkg = ri.activityInfo.packageName
                    val tile = AppTileFactory.create(
                        ctx, pm, lp, ri,
                        isFavRow = true, isFav = true,
                        canMoveLeft = idx > 0, canMoveRight = idx < favApps.size - 1,
                        w = cellW, h = favH, actions = actions
                    )
                    favRow.addView(tile, LinearLayout.LayoutParams(cellW, favH).apply {
                        if (idx < favApps.size - 1) rightMargin = gap
                    })
                }
            }
        }

        allCount.text = "${allApps.size} Apps"
        FluidAppGrid.updateGridState(
            ctx = ctx,
            pm = pm,
            lp = lp,
            gridContainer = appGrid,
            apps = allApps,
            favSet = favSet,
            columns = cols,
            cellW = cellW,
            cellH = cellH,
            gapPx = gap,
            actions = actions
        )

        if (shouldRequestFocus && !isDrawerOpen) {
            appGrid.post {
                val target: View? = if (restorePkg != null) {
                    appGrid.findViewWithTag("all_$restorePkg")
                        ?: favRow.findViewWithTag("fav_$restorePkg")
                } else null
                val fallback = target
                    ?: (if (favRow.childCount > 0) favRow.getChildAt(0) else null)
                    ?: FluidAppGrid.getFirstTile(appGrid)
                fallback?.requestFocus()
            }
        }
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
