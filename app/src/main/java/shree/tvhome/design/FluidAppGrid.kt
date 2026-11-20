package shree.tvhome.design

import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import shree.tvhome.source.LauncherPreferences
import kotlin.math.ceil

object FluidAppGrid {

    private var lastCols = -1
    private var lastCellW = -1
    private var lastCellH = -1
    private var lastGap = -1
    private var lastSignature = ""

    fun updateGridState(
        ctx: Context,
        pm: PackageManager,
        lp: LauncherPreferences,
        gridContainer: LinearLayout,
        apps: List<ResolveInfo>,
        favSet: Set<String>,
        columns: Int,
        cellW: Int,
        cellH: Int,
        gapPx: Int,
        actions: AppTileFactory.TileActions
    ) {
        val sig = apps.joinToString(",") {
            val p = it.activityInfo.packageName
            "$p:${favSet.contains(p)}"
        }

        if (gridContainer.childCount > 0 && sig == lastSignature) {
            if (columns == lastCols && cellW == lastCellW && cellH == lastCellH && gapPx == lastGap) {
                restyleExistingTiles(ctx, gridContainer, lp, favSet)
                return
            }
            reflowExistingGrid(ctx, gridContainer, lp, favSet, columns, cellW, cellH, gapPx)
            lastCols = columns
            lastCellW = cellW
            lastCellH = cellH
            lastGap = gapPx
            return
        }

        rebuildGrid(ctx, pm, lp, gridContainer, apps, favSet, columns, cellW, cellH, gapPx, actions)
        lastSignature = sig
        lastCols = columns
        lastCellW = cellW
        lastCellH = cellH
        lastGap = gapPx
    }

    private fun restyleExistingTiles(ctx: Context, gridContainer: LinearLayout, lp: LauncherPreferences, favSet: Set<String>) {
        for (r in 0 until gridContainer.childCount) {
            val row = gridContainer.getChildAt(r) as? LinearLayout ?: continue
            for (c in 0 until row.childCount) {
                val tile = row.getChildAt(c) ?: continue
                val tag = tile.tag as? String ?: ""
                val pkg = tag.removePrefix("all_").removePrefix("fav_")
                AppTileFactory.updateTileAppearance(ctx, tile, lp, favSet.contains(pkg))
            }
        }
    }

    private fun reflowExistingGrid(
        ctx: Context,
        gridContainer: LinearLayout,
        lp: LauncherPreferences,
        favSet: Set<String>,
        newCols: Int,
        newCellW: Int,
        newCellH: Int,
        newGap: Int
    ) {
        val allTiles = ArrayList<View>()
        for (r in 0 until gridContainer.childCount) {
            val row = gridContainer.getChildAt(r) as? LinearLayout ?: continue
            while (row.childCount > 0) {
                val child = row.getChildAt(0)
                row.removeViewAt(0)
                allTiles.add(child)
            }
        }
        gridContainer.removeAllViews()

        if (allTiles.isEmpty()) return
        val totalRows = ceil(allTiles.size.toDouble() / newCols).toInt()
        for (r in 0 until totalRows) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
                clipChildren = false
                clipToPadding = false
            }
            val start = r * newCols
            val end = minOf(start + newCols, allTiles.size)
            for (i in start until end) {
                val tile = allTiles[i]
                val isLastInRow = (i == end - 1)
                val param = LinearLayout.LayoutParams(newCellW, newCellH).apply {
                    if (!isLastInRow) rightMargin = newGap
                }
                row.addView(tile, param)
                val tag = tile.tag as? String ?: ""
                val pkg = tag.removePrefix("all_").removePrefix("fav_")
                AppTileFactory.updateTileAppearance(ctx, tile, lp, favSet.contains(pkg))

                ObjectAnimator.ofFloat(tile, View.ALPHA, 0.75f, 1f).apply {
                    duration = 220L
                    interpolator = DecelerateInterpolator(1.5f)
                    start()
                }
            }
            gridContainer.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                if (r < totalRows - 1) bottomMargin = newGap
            })
        }
    }

    private fun rebuildGrid(
        ctx: Context,
        pm: PackageManager,
        lp: LauncherPreferences,
        gridContainer: LinearLayout,
        apps: List<ResolveInfo>,
        favSet: Set<String>,
        columns: Int,
        cellW: Int,
        cellH: Int,
        gapPx: Int,
        actions: AppTileFactory.TileActions
    ) {
        gridContainer.removeAllViews()
        if (apps.isEmpty()) return

        val totalRows = ceil(apps.size.toDouble() / columns).toInt()
        for (r in 0 until totalRows) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
                clipChildren = false
                clipToPadding = false
            }
            val start = r * columns
            val end = minOf(start + columns, apps.size)
            for (i in start until end) {
                val ri = apps[i]
                val pkg = ri.activityInfo.packageName
                val isLastInRow = (i == end - 1)
                val tile = AppTileFactory.create(
                    ctx, pm, lp, ri,
                    isFavRow = false, isFav = favSet.contains(pkg),
                    canMoveLeft = false, canMoveRight = false,
                    w = cellW, h = cellH, actions = actions
                )
                row.addView(tile, LinearLayout.LayoutParams(cellW, cellH).apply {
                    if (!isLastInRow) rightMargin = gapPx
                })
            }
            gridContainer.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                if (r < totalRows - 1) bottomMargin = gapPx
            })
        }
    }

    fun getFirstTile(gridContainer: LinearLayout): View? {
        val firstRow = gridContainer.getChildAt(0) as? LinearLayout ?: return null
        return if (firstRow.childCount > 0) firstRow.getChildAt(0) else null
    }
}
