package shree.tvhome.panels

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.LauncherPreferences
import shree.tvhome.LauncherUtils
import shree.tvhome.MotionEngine
import shree.tvhome.ui.TileThemePalette
import java.util.Locale

object WifiPanelBuilder {

    @Suppress("DEPRECATION")
    fun build(ctx: Context, panel: LinearLayout, lp: LauncherPreferences) {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val statusCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            isFocusable = true
            isClickable = true
        }

        val infoCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val titleTv = TextView(ctx).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val subTv = TextView(ctx).apply {
            textSize = 9.5f
        }
        infoCol.addView(titleTv)
        infoCol.addView(subTv)
        statusCard.addView(infoCol, LinearLayout.LayoutParams(0, -2, 1f))

        val togglePill = TextView(ctx).apply {
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(ctx, 12), dp(ctx, 5), dp(ctx, 12), dp(ctx, 5))
        }
        statusCard.addView(togglePill)
        panel.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        val dynamicListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }

        lateinit var renderDynamicWifiContent: () -> Unit
        renderDynamicWifiContent = {
            dynamicListContainer.removeAllViews()
            val curOn = try { wm?.isWifiEnabled == true } catch (_: Exception) { false }
            val curSsid = TileThemePalette.getConnectedSsid(ctx)

            titleTv.text = if (curOn) curSsid else "Wi-Fi Turned Off"
            subTv.text = if (curOn) "● Active • Tap to toggle Wi-Fi" else "○ Tap to turn on Wi-Fi"
            subTv.setTextColor(if (curOn) Color.rgb(120, 245, 175) else Color.LTGRAY)
            togglePill.text = if (curOn) "ON" else "OFF"
            togglePill.background = GradientDrawable().apply {
                setColor(if (curOn) Color.rgb(34, 185, 110) else Color.argb(85, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
            }
            statusCard.background = cardBg(ctx, lp.accentColor, curOn, Color.rgb(46, 232, 138))

            statusCard.setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                val nowEnabled = try { wm?.isWifiEnabled == true } catch (_: Exception) { curOn }
                v.background = cardBg(ctx, lp.accentColor, f || nowEnabled, Color.rgb(46, 232, 138))
            }

            if (curOn && wm != null) {
                val info = try { wm.connectionInfo } catch (_: Exception) { null }
                val ipInt = info?.ipAddress ?: 0
                val ipStr = if (ipInt != 0) String.format(
                    Locale.US, "%d.%d.%d.%d",
                    ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff, ipInt shr 24 and 0xff
                ) else "Active Link"
                val speed = info?.linkSpeed ?: 0

                val metricCard = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
                    background = cardBg(ctx, lp.accentColor, false, lp.accentColor)
                }
                val col1 = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
                col1.addView(TextView(ctx).apply { text = "LINK SPEED"; textSize = 8f; setTextColor(Color.LTGRAY) })
                col1.addView(TextView(ctx).apply {
                    text = if (speed > 0) "$speed Mbps" else "Connected"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                })
                metricCard.addView(col1, LinearLayout.LayoutParams(0, -2, 1f))

                val col2 = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
                col2.addView(TextView(ctx).apply { text = "IP ADDRESS"; textSize = 8f; setTextColor(Color.LTGRAY) })
                col2.addView(TextView(ctx).apply {
                    text = ipStr
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                })
                metricCard.addView(col2, LinearLayout.LayoutParams(0, -2, 1f))
                dynamicListContainer.addView(metricCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 10) })
            }

            dynamicListContainer.addView(TextView(ctx).apply {
                text = "DISCOVERED WI-FI NETWORKS"
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(lp.accentColor)
                letterSpacing = 0.08f
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

            @SuppressLint("MissingPermission")
            val results = try { wm?.scanResults ?: emptyList() } catch (_: Exception) { emptyList() }
            val unique = results.filter { !it.SSID.isNullOrBlank() }.distinctBy { it.SSID }.sortedByDescending { it.level }

            if (unique.isEmpty()) {
                dynamicListContainer.addView(TextView(ctx).apply {
                    text = "No networks returned by scan yet. Tap Refresh Scan above."
                    textSize = 9.5f
                    setTextColor(Color.argb(190, 210, 230, 250))
                    setPadding(dp(ctx, 4), dp(ctx, 8), dp(ctx, 4), dp(ctx, 8))
                })
            } else {
                for (scan in unique) {
                    val isCurrent = scan.SSID == curSsid
                    val netRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
                        isFocusable = true
                        isClickable = true
                        background = cardBg(ctx, lp.accentColor, isCurrent, Color.rgb(46, 232, 138))
                        setOnFocusChangeListener { v, f ->
                            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                            v.background = cardBg(ctx, lp.accentColor, f || isCurrent, Color.rgb(46, 232, 138))
                        }
                        setOnClickListener {
                            showGlossyWifiConnectDialog(ctx, lp, wm, scan.SSID)
                        }
                    }
                    netRow.addView(TextView(ctx).apply {
                        text = if (isCurrent) "✓  ${scan.SSID}" else "📶  ${scan.SSID}"
                        textSize = 10.5f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(if (isCurrent) Color.rgb(120, 245, 175) else Color.WHITE)
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                    netRow.addView(TextView(ctx).apply {
                        text = "›"
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.LTGRAY)
                    })
                    dynamicListContainer.addView(netRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
                }
            }
        }

        statusCard.setOnClickListener {
            val currentlyOn = try { wm?.isWifiEnabled == true } catch (_: Exception) { false }
            try {
                wm?.isWifiEnabled = !currentlyOn
                renderDynamicWifiContent()
            } catch (_: Exception) {
                Toast.makeText(ctx, "Wi-Fi radio controlled by system", Toast.LENGTH_SHORT).show()
            }
        }

        val refreshBtn = makeGlassBtn(ctx, lp, "🔄  Refresh Nearby Networks", Color.rgb(92, 184, 255)) {
            try { wm?.startScan() } catch (_: Exception) {}
            renderDynamicWifiContent()
            Toast.makeText(ctx, "Wi-Fi scan refreshed", Toast.LENGTH_SHORT).show()
        }
        panel.addView(refreshBtn, LinearLayout.LayoutParams(-1, dp(ctx, 38)).apply { bottomMargin = dp(ctx, 10) })
        panel.addView(dynamicListContainer, LinearLayout.LayoutParams(-1, -2))
        renderDynamicWifiContent()
    }

    @Suppress("DEPRECATION")
    private fun showGlossyWifiConnectDialog(ctx: Context, lp: LauncherPreferences, wm: WifiManager?, ssid: String) {
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 18), dp(ctx, 22), dp(ctx, 18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(242, 24, 36, 78), Color.argb(248, 12, 18, 44))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 22f)
                setStroke(dp(ctx, 1), Color.argb(140, 255, 255, 255))
            }
        }

        box.addView(TextView(ctx).apply {
            text = "📶  Connect to $ssid"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(ctx, 12))
        })

        val passInput = EditText(ctx).apply {
            isSingleLine = true
            hint = "Enter Wi-Fi Password"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(150, 200, 220, 245))
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            background = GradientDrawable().apply {
                setColor(Color.argb(55, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 12f)
                setStroke(dp(ctx, 1), lp.accentColor)
            }
        }
        box.addView(passInput, LinearLayout.LayoutParams(dp(ctx, 300), -2))

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(
            makeGlassBtn(ctx, lp, "Cancel", Color.rgb(175, 195, 235)) { dialog.dismiss() },
            LinearLayout.LayoutParams(0, dp(ctx, 38), 1f).apply { rightMargin = dp(ctx, 8) }
        )

        btnRow.addView(
            makeGlassBtn(ctx, lp, "✓  Connect", Color.rgb(46, 232, 138)) {
                val pwd = passInput.text.toString()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val spec = WifiNetworkSpecifier.Builder().setSsid(ssid).apply {
                        if (pwd.isNotEmpty()) setWpa2Passphrase(pwd)
                    }.build()
                    val req = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(spec)
                        .build()
                    val cm = ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    cm?.requestNetwork(req, ConnectivityManager.NetworkCallback())
                } else if (wm != null) {
                    val conf = WifiConfiguration().apply {
                        SSID = "\"$ssid\""
                        if (pwd.isNotEmpty()) preSharedKey = "\"$pwd\"" else allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    }
                    val netId = wm.addNetwork(conf)
                    if (netId != -1) {
                        wm.disconnect()
                        wm.enableNetwork(netId, true)
                        wm.reconnect()
                    }
                }
                Toast.makeText(ctx, "Connecting to $ssid...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            },
            LinearLayout.LayoutParams(0, dp(ctx, 38), 1.2f)
        )

        box.addView(btnRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 14) })
        dialog.setContentView(box)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun makeGlassBtn(ctx: Context, lp: LauncherPreferences, label: String, tint: Int, onClick: () -> Unit) =
        TextView(ctx).apply {
            text = label
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = cardBg(ctx, lp.accentColor, false, tint)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 4f)
                v.background = cardBg(ctx, lp.accentColor, f, tint)
            }
            setOnClickListener { onClick() }
        }

    private fun cardBg(ctx: Context, accent: Int, active: Boolean, glowTint: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(glowTint, 150), LauncherUtils.withAlpha(accent, 75))
        else intArrayOf(LauncherUtils.withAlpha(glowTint, 68), Color.argb(34, 20, 30, 65))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 15f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) Color.WHITE else LauncherUtils.withAlpha(glowTint, 125))
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
