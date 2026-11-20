package shree.tvhome

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import shree.tvhome.design.LauncherUtils
import shree.tvhome.design.MotionEngine
import shree.tvhome.source.LauncherPreferences
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class AmbientStageWidget(
    context: Context,
    private val lp: LauncherPreferences,
    private val onWeatherUpdated: ((Int) -> Unit)? = null
) : LinearLayout(context) {

    private val timeTv: TextView
    private val amPmTv: TextView
    private val dateTv: TextView
    private val cityBadgeTv: TextView
    private val weatherSummaryTv: TextView
    private val refreshWeatherBtn: TextView
    private val citySearchInput: EditText
    private val suggestionsContainer: LinearLayout

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private val clockTicker = object : Runnable {
        override fun run() {
            updateClockNow()
            mainHandler.postDelayed(this, 15000L)
        }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        clipChildren = false
        clipToPadding = false
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = stageBg()

        val clockRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        timeTv = TextView(context).apply {
            textSize = 34f
            typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
            setTextColor(Color.WHITE)
            letterSpacing = -0.02f
        }
        amPmTv = TextView(context).apply {
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), 0, 0, dp(5))
        }
        clockRow.addView(timeTv)
        clockRow.addView(amPmTv)
        addView(clockRow)

        dateTv = TextView(context).apply {
            textSize = 10.5f
            setTextColor(Color.argb(215, 215, 232, 252))
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(10))
        }
        addView(dateTv)

        val weatherHeader = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(4))
        }
        cityBadgeTv = TextView(context).apply {
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 1
        }
        refreshWeatherBtn = TextView(context).apply {
            text = "↻"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = smallBtnBg(false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.1f, 3f)
                v.background = smallBtnBg(f)
            }
            setOnClickListener { triggerWeatherRefresh(showToast = true) }
        }
        weatherHeader.addView(cityBadgeTv, LayoutParams(0, -2, 1f))
        weatherHeader.addView(refreshWeatherBtn, LayoutParams(dp(26), dp(26)))
        addView(weatherHeader, LayoutParams(-1, -2))

        weatherSummaryTv = TextView(context).apply {
            text = "Tap ↻ or search city for weather"
            textSize = 10f
            setTextColor(Color.argb(210, 220, 236, 255))
            setPadding(0, dp(2), 0, dp(8))
        }
        addView(weatherSummaryTv)

        val searchRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        citySearchInput = EditText(context).apply {
            hint = "City (e.g. Vizianagaram)"
            setText(lp.weatherCity)
            textSize = 10f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(135, 195, 215, 240))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            isSingleLine = true
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = inputBg(false)
            setOnFocusChangeListener { v, f -> v.background = inputBg(f) }
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    val q = text?.toString()?.trim().orEmpty()
                    if (q.isNotEmpty()) {
                        fetchCitySuggestionsWithStatus(q, autoSelectFirst = true)
                    }
                    true
                } else false
            }
        }
        val goBtn = TextView(context).apply {
            text = "Go"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = smallBtnBg(false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.08f, 3f)
                v.background = smallBtnBg(f)
            }
            setOnClickListener {
                val q = citySearchInput.text?.toString()?.trim().orEmpty()
                if (q.isNotEmpty()) {
                    fetchCitySuggestionsWithStatus(q, autoSelectFirst = true)
                } else {
                    triggerWeatherRefresh(showToast = true)
                }
            }
        }
        searchRow.addView(citySearchInput, LayoutParams(0, dp(34), 1f).apply { rightMargin = dp(6) })
        searchRow.addView(goBtn, LayoutParams(dp(38), dp(34)))
        addView(searchRow, LayoutParams(-1, -2))

        suggestionsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            visibility = View.GONE
            setPadding(0, dp(6), 0, 0)
        }
        addView(suggestionsContainer, LayoutParams(-1, -2))

        updateClockNow()
        refreshStyle()
        if (lp.weatherCity.isNotBlank()) {
            triggerWeatherRefresh(showToast = false)
        } else {
            cityBadgeTv.text = "📍 Set City"
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mainHandler.removeCallbacks(clockTicker)
        mainHandler.post(clockTicker)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainHandler.removeCallbacks(clockTicker)
    }

    fun refreshStyle() {
        background = stageBg()
        amPmTv.setTextColor(lp.accentColor)
        refreshWeatherBtn.background = smallBtnBg(refreshWeatherBtn.hasFocus())
        citySearchInput.background = inputBg(citySearchInput.hasFocus())
    }

    private fun updateClockNow() {
        val now = Date()
        val cal = Calendar.getInstance()
        val timeFmt = SimpleDateFormat("hh:mm", Locale.getDefault())
        val amPmFmt = SimpleDateFormat("a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        timeTv.text = timeFmt.format(now)
        amPmTv.text = amPmFmt.format(now)
        dateTv.text = "${dateFmt.format(now)} • ${cal.get(Calendar.YEAR)}"
    }

    private fun fetchCitySuggestionsWithStatus(query: String, autoSelectFirst: Boolean) {
        weatherSummaryTv.text = "Searching '$query'..."
        ioExecutor.execute {
            val matches = ArrayList<Triple<String, Double, Double>>()
            try {
                if (Geocoder.isPresent()) {
                    @Suppress("DEPRECATION")
                    val list = Geocoder(context, Locale.getDefault()).getFromLocationName(query, 4)
                    list?.forEach { addr ->
                        val title = listOfNotNull(addr.locality, addr.subAdminArea, addr.adminArea)
                            .distinct().joinToString(", ").ifEmpty { addr.featureName ?: query }
                        matches.add(Triple(title, addr.latitude, addr.longitude))
                    }
                }
            } catch (_: Exception) {}

            if (matches.isEmpty()) {
                try {
                    val enc = URLEncoder.encode(query, "UTF-8")
                    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$enc&count=4&language=en&format=json")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONObject(body).optJSONArray("results")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val n = obj.optString("name", query)
                            val adm = obj.optString("admin1", "")
                            val lbl = if (adm.isNotBlank()) "$n, $adm" else n
                            val lat = obj.optDouble("latitude", Double.NaN)
                            val lon = obj.optDouble("longitude", Double.NaN)
                            if (!lat.isNaN() && !lon.isNaN()) {
                                matches.add(Triple(lbl, lat, lon))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            mainHandler.post {
                if (matches.isEmpty()) {
                    weatherSummaryTv.text = "City not found: '$query'"
                    Toast.makeText(context, "City not found", Toast.LENGTH_SHORT).show()
                    return@post
                }
                if (autoSelectFirst) {
                    val chosen = matches.first()
                    selectCity(chosen.first, chosen.second, chosen.third)
                } else {
                    suggestionsContainer.removeAllViews()
                    suggestionsContainer.visibility = View.VISIBLE
                    matches.forEach { (lbl, lat, lon) ->
                        val item = TextView(context).apply {
                            text = "📍 $lbl"
                            textSize = 9.5f
                            setTextColor(Color.WHITE)
                            setPadding(dp(8), dp(6), dp(8), dp(6))
                            isFocusable = true
                            isClickable = true
                            background = smallBtnBg(false)
                            setOnClickListener { selectCity(lbl, lat, lon) }
                        }
                        suggestionsContainer.addView(item, LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
                    }
                }
            }
        }
    }

    private fun selectCity(label: String, lat: Double, lon: Double) {
        suggestionsContainer.visibility = View.GONE
        lp.weatherCity = label
        citySearchInput.setText(label)
        cityBadgeTv.text = "📍 $label"
        fetchWeatherForCoords(label, lat, lon, showToast = true)
    }

    fun triggerWeatherRefresh(showToast: Boolean) {
        val city = lp.weatherCity.trim()
        if (city.isEmpty()) {
            cityBadgeTv.text = "📍 Set City"
            weatherSummaryTv.text = "Enter a city name below"
            if (showToast) Toast.makeText(context, "Enter a city name first", Toast.LENGTH_SHORT).show()
            return
        }
        cityBadgeTv.text = "📍 $city"
        fetchCitySuggestionsWithStatus(city, autoSelectFirst = true)
        if (showToast) Toast.makeText(context, "Refreshing weather for $city", Toast.LENGTH_SHORT).show()
    }

    private fun fetchWeatherForCoords(label: String, lat: Double, lon: Double, showToast: Boolean) {
        weatherSummaryTv.text = "Updating weather..."
        ioExecutor.execute {
            try {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val cw = JSONObject(raw).optJSONObject("current_weather")
                val temp = cw?.optDouble("temperature", Double.NaN) ?: Double.NaN
                val code = cw?.optInt("weathercode", 0) ?: 0
                val cond = describeWeatherCode(code)
                lp.cachedWeatherCode = code

                mainHandler.post {
                    if (!temp.isNaN()) {
                        weatherSummaryTv.text = "${temp.toInt()}°C • $cond"
                        onWeatherUpdated?.invoke(code)
                        if (showToast) Toast.makeText(context, "$label: ${temp.toInt()}°C ($cond)", Toast.LENGTH_SHORT).show()
                    } else {
                        weatherSummaryTv.text = "Weather unavailable"
                    }
                }
            } catch (_: Exception) {
                mainHandler.post {
                    weatherSummaryTv.text = "Offline / Weather fetch failed"
                    if (showToast) Toast.makeText(context, "Unable to fetch weather", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Rainy"
        95, 96, 99 -> "Thunderstorm"
        else -> "Mild"
    }

    private fun stageBg() = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            LauncherUtils.withAlpha(lp.accentColor, 62),
            Color.argb(66, 18, 26, 56),
            Color.argb(82, 10, 15, 34)
        )
    ).apply {
        cornerRadius = LauncherUtils.dpF(context, 24f)
        setStroke(dp(1), LauncherUtils.withAlpha(lp.accentColor, 150))
    }

    private fun inputBg(focused: Boolean) = GradientDrawable().apply {
        setColor(Color.argb(if (focused) 85 else 52, 255, 255, 255))
        cornerRadius = LauncherUtils.dpF(context, 12f)
        setStroke(dp(if (focused) 2 else 1), if (focused) Color.WHITE else LauncherUtils.withAlpha(lp.accentColor, 145))
    }

    private fun smallBtnBg(focused: Boolean) = GradientDrawable().apply {
        setColor(if (focused) LauncherUtils.withAlpha(lp.accentColor, 200) else Color.argb(62, 255, 255, 255))
        cornerRadius = LauncherUtils.dpF(context, 12f)
        setStroke(dp(if (focused) 2 else 1), if (focused) Color.WHITE else LauncherUtils.withAlpha(lp.accentColor, 145))
    }

    private fun dp(v: Int) = LauncherUtils.dp(context, v)
}
