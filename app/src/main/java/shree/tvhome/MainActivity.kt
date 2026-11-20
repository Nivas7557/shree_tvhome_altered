package shree.tvhome

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.design.AppGridManager
import shree.tvhome.design.AppTileFactory
import shree.tvhome.design.DrawerFocusTrap
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.panels.SideControlDrawer
import shree.tvhome.source.LauncherBackground
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.NightNudgeReceiver

class MainActivity : Activity() {

    private val pickWallpaperCode = 1001
    private val pickProfilePicCode = 1002

    private lateinit var lp: LauncherPreferences
    private lateinit var rootLayout: FrameLayout
    private lateinit var mainPage: LinearLayout
    private lateinit var mainScroll: ScrollView
    private lateinit var topNavBar: TopNavBar
    private lateinit var favSection: LinearLayout
    private lateinit var favRow: LinearLayout
    private lateinit var favCountTv: TextView
    private lateinit var appGrid: LinearLayout
    private lateinit var allCountTv: TextView
    private lateinit var hiddenSection: LinearLayout
    private lateinit var hiddenRow: LinearLayout
    private lateinit var hiddenCountTv: TextView
    private lateinit var ambientStage: AmbientStageWidget
    private lateinit var drawerLayout: LinearLayout
    private lateinit var scrim: View
    private lateinit var drawer: SideControlDrawer
    private lateinit var nudgeBanner: LinearLayout
    private lateinit var nudgeMsgTv: TextView

    private var lastFocusedPkg: String? = null
    private var lastScrollY = 0
    private val nudgeHandler = Handler(Looper.getMainLooper())
    private val nudgeChecker = object : Runnable {
        override fun run() {
            refreshNightNudgeBanner()
            nudgeHandler.postDelayed(this, 30000L)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppGridManager.clearCache()
            refreshGrid(shouldRequestFocus = false)
        }
    }

    private val tileActions = object : AppTileFactory.TileActions {
        override fun onToggleFavorite(pkg: String, label: String) {
            val favs = lp.getOrderedFavorites()
            if (favs.contains(pkg)) {
                favs.remove(pkg)
                lp.setFavorites(favs)
                Toast.makeText(this@MainActivity, "Removed $label from Favorites", Toast.LENGTH_SHORT).show()
            } else {
                favs.add(pkg)
                lp.setFavorites(favs)
                Toast.makeText(this@MainActivity, "Added $label to Favorites", Toast.LENGTH_SHORT).show()
            }
            refreshGrid(shouldRequestFocus = false, restorePkg = pkg)
        }

        override fun onMoveFavorite(pkg: String, delta: Int) {
            if (lp.moveFavorite(pkg, delta)) {
                refreshGrid(shouldRequestFocus = false, restorePkg = pkg)
            }
        }

        override fun onHideApp(pkg: String, label: String) {
            val hidden = lp.getHiddenApps()
            val wasHidden = hidden.remove(pkg)
            if (!wasHidden) hidden.add(pkg)
            lp.setHiddenApps(hidden)
            if (!wasHidden) {
                val favs = lp.getOrderedFavorites()
                if (favs.remove(pkg)) lp.setFavorites(favs)
                Toast.makeText(this@MainActivity, "Hid $label", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Restored $label", Toast.LENGTH_SHORT).show()
            }
            refreshGrid(shouldRequestFocus = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        lp = LauncherPreferences(this)
        NightNudgeReceiver.scheduleNextNudge(this)

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(10, 14, 28))
            clipChildren = false
            clipToPadding = false
        }

        val screenW = resources.displayMetrics.widthPixels
        val panelW = maxOf(dp(250), (screenW * (lp.panelWidthPct / 100f)).toInt())

        val contentArea = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        mainScroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                lastScrollY = scrollY
                LauncherBackground.onScrollParallax(scrollY)
            }
        }

        mainPage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(26), dp(14), dp(14), dp(28))
        }

        topNavBar = TopNavBar(
            context = this,
            lp = lp,
            onAvatarClick = { drawer.showProfileEditDialog() },
            onToggleWifiPanel = { drawer.toggle(SideControlDrawer.MODE_WIFI) },
            onToggleAlertsPanel = { drawer.toggle(SideControlDrawer.MODE_ALERTS) },
            onToggleCustomize = { drawer.toggle(SideControlDrawer.MODE_CUSTOMIZE) },
            onToggleQuickMenu = { drawer.toggle(SideControlDrawer.MODE_QUICK) },
            onTogglePowerMenu = { drawer.toggle(SideControlDrawer.MODE_POWER) }
        )
        mainPage.addView(topNavBar, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val favHeader = AppGridManager.createHeader(this, "FAVORITES", isFav = true)
        favCountTv = favHeader.findViewWithTag("count_tv")
        favRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            clipChildren = false
            clipToPadding = false
        }
        val favScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            addView(favRow, FrameLayout.LayoutParams(-2, -2))
        }
        favSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            addView(favHeader)
            addView(favScroll, LinearLayout.LayoutParams(-1, -2))
        }
        mainPage.addView(favSection, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        val allHeader = AppGridManager.createHeader(
            this, "ALL APPS", isFav = false, lp = lp,
            onSortToggled = { refreshGrid(shouldRequestFocus = false) }
        )
        allCountTv = allHeader.findViewWithTag("count_tv")
        mainPage.addView(allHeader)

        appGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        mainPage.addView(appGrid, LinearLayout.LayoutParams(-1, -2))

        val hiddenHeader = AppGridManager.createHeader(this, "HIDDEN APPS (TAP TO RESTORE)", isFav = true)
        hiddenCountTv = hiddenHeader.findViewWithTag("count_tv")
        hiddenRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            clipChildren = false
            clipToPadding = false
        }
        val hiddenScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            addView(hiddenRow, FrameLayout.LayoutParams(-2, -2))
        }
        hiddenSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            clipChildren = false
            clipToPadding = false
            setPadding(0, dp(14), 0, 0)
            addView(hiddenHeader)
            addView(hiddenScroll, LinearLayout.LayoutParams(-1, -2))
        }
        mainPage.addView(hiddenSection, LinearLayout.LayoutParams(-1, -2))

        mainScroll.addView(mainPage, FrameLayout.LayoutParams(-1, -2))
        contentArea.addView(mainScroll, FrameLayout.LayoutParams(-1, -1).apply {
            rightMargin = panelW + dp(12)
        })

        ambientStage = AmbientStageWidget(
            context = this,
            lp = lp,
            onWeatherUpdated = { _ ->
                if (lp.liveWallpaperMode == LauncherBackground.MODE_SMART) {
                    LauncherBackground.updateThemeAndAccent(rootLayout, lp)
                }
            }
        )
        contentArea.addView(ambientStage, FrameLayout.LayoutParams(panelW, -2, Gravity.END or Gravity.CENTER_VERTICAL).apply {
            rightMargin = dp(14)
        })

        rootLayout.addView(contentArea, FrameLayout.LayoutParams(-1, -1))

        nudgeBanner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.argb(235, 22, 32, 68))
                cornerRadius = LauncherUtils.dpF(this@MainActivity, 16f)
                setStroke(dp(1), Color.rgb(255, 195, 75))
            }
        }
        nudgeMsgTv = TextView(this).apply {
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val dismissNudgeBtn = TextView(this).apply {
            text = "✕"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(255, 195, 75))
            setPadding(dp(10), 0, 0, 0)
            isFocusable = true
            isClickable = true
            setOnClickListener {
                NightNudgeReceiver.dismissActiveNudge(this@MainActivity)
                nudgeBanner.visibility = View.GONE
            }
        }
        nudgeBanner.addView(nudgeMsgTv, LinearLayout.LayoutParams(0, -2, 1f))
        nudgeBanner.addView(dismissNudgeBtn)
        rootLayout.addView(nudgeBanner, FrameLayout.LayoutParams(dp(420), -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(10)
        })

        scrim = View(this).apply {
            visibility = View.GONE
            isClickable = true
            setOnClickListener { drawer.close() }
        }
        rootLayout.addView(scrim, FrameLayout.LayoutParams(-1, -1))

        drawerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            isFocusable = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            clipChildren = true
            clipToPadding = true
        }
        rootLayout.addView(drawerLayout, FrameLayout.LayoutParams(panelW, -1, Gravity.END or Gravity.TOP).apply {
            topMargin = dp(12); bottomMargin = dp(12); rightMargin = dp(12)
        })

        drawer = SideControlDrawer(
            context = this,
            drawerLayout = drawerLayout,
            scrim = scrim,
            mainPage = mainPage,
            lp = lp,
            onLiveRefresh = { applyLiveVisuals() },
            onWallpaperRefresh = { LauncherBackground.updateThemeAndAccent(rootLayout, lp) },
            onPickWallpaper = { launchImagePicker(pickWallpaperCode) },
            onPickProfilePic = { launchImagePicker(pickProfilePicCode) },
            onOpenHiddenRibbon = { openHiddenRibbon() }
        )
        drawer.attachStageWidget(ambientStage)

        setContentView(rootLayout)
        LauncherBackground.apply(this, rootLayout, lp)
        refreshGrid(shouldRequestFocus = true)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(packageReceiver, filter)
        }
        nudgeHandler.removeCallbacks(nudgeChecker)
        nudgeHandler.post(nudgeChecker)
        topNavBar.updateRamInfo()
        topNavBar.updateWifiVisualState()
        topNavBar.updateAlertsBadge()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(packageReceiver) } catch (_: Exception) {}
        nudgeHandler.removeCallbacks(nudgeChecker)
    }

    override fun onDestroy() {
        super.onDestroy()
        nudgeHandler.removeCallbacks(nudgeChecker)
        LauncherBackground.stopAll()
    }

    private fun refreshNightNudgeBanner() {
        val active = NightNudgeReceiver.getActiveNudge(this)
        if (active != null) {
            nudgeMsgTv.text = active
            nudgeBanner.visibility = View.VISIBLE
        } else {
            nudgeBanner.visibility = View.GONE
        }
    }

    private fun openHiddenRibbon() {
        AppGridManager.populateHiddenRibbon(this, lp, hiddenSection, hiddenRow, hiddenCountTv) { pkg, lbl ->
            tileActions.onHideApp(pkg, lbl)
            if (lp.getHiddenApps().isEmpty()) {
                hiddenSection.visibility = View.GONE
                refreshGrid(shouldRequestFocus = true, restorePkg = pkg)
            } else {
                openHiddenRibbon()
            }
        }
        mainScroll.post { mainScroll.smoothScrollTo(0, mainPage.bottom) }
    }

    private fun applyLiveVisuals() {
        topNavBar.updateGreeting()
        topNavBar.updateThemeAccent()
        ambientStage.refreshStyle()
        LauncherBackground.updateThemeAndAccent(rootLayout, lp)

        val screenW = resources.displayMetrics.widthPixels
        val panelW = maxOf(dp(250), (screenW * (lp.panelWidthPct / 100f)).toInt())

        val stageParams = ambientStage.layoutParams as? FrameLayout.LayoutParams
        if (stageParams != null && stageParams.width != panelW) {
            stageParams.width = panelW
            ambientStage.layoutParams = stageParams
        }
        val scrollParams = mainScroll.layoutParams as? FrameLayout.LayoutParams
        val targetRightMargin = panelW + dp(12)
        if (scrollParams != null && scrollParams.rightMargin != targetRightMargin) {
            scrollParams.rightMargin = targetRightMargin
            mainScroll.layoutParams = scrollParams
        }
        if (drawer.isOpen) {
            val dParams = drawerLayout.layoutParams as? FrameLayout.LayoutParams
            if (dParams != null && dParams.width != panelW) {
                dParams.width = panelW
                drawerLayout.layoutParams = dParams
            }
        }

        val focusedTag = (currentFocus?.tag as? String)?.removePrefix("all_")?.removePrefix("fav_")
        refreshGrid(shouldRequestFocus = false, restorePkg = focusedTag ?: lastFocusedPkg)
    }

    private fun refreshGrid(shouldRequestFocus: Boolean, restorePkg: String? = null) {
        val targetPkg = restorePkg ?: lastFocusedPkg
        AppGridManager.populate(
            ctx = this, lp = lp,
            favSection = favSection, favRow = favRow, favCount = favCountTv,
            appGrid = appGrid, allCount = allCountTv,
            shouldRequestFocus = shouldRequestFocus, restorePkg = targetPkg,
            isDrawerOpen = drawer.isOpen, actions = tileActions
        )
        if (hiddenSection.visibility == View.VISIBLE) {
            AppGridManager.populateHiddenRibbon(this, lp, hiddenSection, hiddenRow, hiddenCountTv) { pkg, lbl ->
                tileActions.onHideApp(pkg, lbl)
                if (lp.getHiddenApps().isEmpty()) {
                    hiddenSection.visibility = View.GONE
                    refreshGrid(shouldRequestFocus = true, restorePkg = pkg)
                } else {
                    openHiddenRibbon()
                }
            }
        }
        if (lastScrollY > 0) {
            mainScroll.post { mainScroll.scrollTo(0, lastScrollY) }
        }
    }

    private fun launchImagePicker(code: Int) {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, code)
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, "Select Image"), code)
            } catch (_: Exception) {
                Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            val takeFlags = (data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            if (takeFlags != 0) {
                contentResolver.takePersistableUriPermission(uri, takeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (_: Exception) {}

        if (requestCode == pickWallpaperCode) {
            lp.prefs.edit().putString(LauncherPreferences.PREF_WALLPAPER, uri.toString()).apply()
            LauncherBackground.apply(this, rootLayout, lp)
            if (drawer.isOpen) drawer.open(SideControlDrawer.MODE_WALLPAPER_EDIT, preserveReturn = true)
            Toast.makeText(this, "Custom wallpaper applied", Toast.LENGTH_SHORT).show()
        } else if (requestCode == pickProfilePicCode) {
            lp.prefs.edit()
                .putString("profile_pic_uri", uri.toString())
                .putFloat("profile_pic_zoom", 1.0f)
                .putFloat("profile_pic_pan_x", 0f)
                .putFloat("profile_pic_pan_y", 0f)
                .apply()
            topNavBar.refreshProfileAvatar()
            drawer.showProfileEditDialog()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU) {
            drawer.toggle(SideControlDrawer.MODE_CUSTOMIZE)
            return true
        }
        if (DrawerFocusTrap.handleKeyEvent(event, drawer.isOpen, drawerLayout, topNavBar, drawer.getParentNavTag())) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawer.isOpen) {
            if (!drawer.handleBackPress()) drawer.close()
            return
        }
        if (hiddenSection.visibility == View.VISIBLE) {
            hiddenSection.visibility = View.GONE
            refreshGrid(shouldRequestFocus = true)
            return
        }
        if (mainScroll.scrollY > 0) {
            mainScroll.smoothScrollTo(0, 0)
            return
        }
        val firstFav = if (favRow.childCount > 0) favRow.getChildAt(0) else null
        val firstAll = AppGridManager.let { appGrid.getChildAt(0) }
        val targetFirst = firstFav ?: firstAll
        if (targetFirst != null && currentFocus !== targetFirst) {
            targetFirst.requestFocus()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    private fun dp(v: Int) = LauncherUtils.dp(this, v)
}
