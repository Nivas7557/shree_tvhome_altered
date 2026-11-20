package shree.tvhome.source

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class NightNudgeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NIGHT_NUDGE = "shree.tvhome.action.NIGHT_NUDGE"
        const val PREFS_NUDGE = "night_nudge_prefs"
        const val KEY_ACTIVE_NUDGE = "active_nudge_msg"
        const val KEY_ACTIVE_EXPIRY = "active_nudge_expiry"

        private data class NudgeSlot(
            val hour: Int,
            val minute: Int,
            val reqCode: Int,
            val message: String,
            val durationMs: Long
        )

        private val SLOTS = listOf(
            NudgeSlot(21, 30, 7001, "9:30 PM • Time to wrap up heavy tasks & start winding down.", 25 * 60 * 1000L),
            NudgeSlot(22, 0, 7002, "10:00 PM • Night wind-down: dim screens & prep for rest.", 30 * 60 * 1000L),
            NudgeSlot(22, 30, 7003, "10:30 PM • Late night check-in: wrap up & rest soon.", 30 * 60 * 1000L),
            NudgeSlot(23, 0, 7004, "11:00 PM • Sleep time reminder — rest well for tomorrow.", 45 * 60 * 1000L),
            NudgeSlot(0, 0, 7005, "12:00 AM • Midnight alert — time to sleep now.", 60 * 60 * 1000L)
        )

        fun scheduleNextNudge(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = System.currentTimeMillis()
            var nextTrigger = Long.MAX_VALUE
            var nextReq = 7001
            var nextMsg = SLOTS.first().message
            var nextDur = SLOTS.first().durationMs

            for (slot in SLOTS) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, slot.hour)
                    set(Calendar.MINUTE, slot.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                if (cal.timeInMillis < nextTrigger) {
                    nextTrigger = cal.timeInMillis
                    nextReq = slot.reqCode
                    nextMsg = slot.message
                    nextDur = slot.durationMs
                }
            }

            if (nextTrigger == Long.MAX_VALUE) return

            val intent = Intent(context, NightNudgeReceiver::class.java).apply {
                action = ACTION_NIGHT_NUDGE
                putExtra("nudge_msg", nextMsg)
                putExtra("nudge_dur", nextDur)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pi = PendingIntent.getBroadcast(context, nextReq, intent, flags)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pi)
                } else {
                    @Suppress("DEPRECATION")
                    am.setExact(AlarmManager.RTC_WAKEUP, nextTrigger, pi)
                }
            } catch (_: Exception) {
                try {
                    am.set(AlarmManager.RTC_WAKEUP, nextTrigger, pi)
                } catch (_: Exception) {}
            }
        }

        fun getActiveNudge(context: Context): String? {
            val sp = context.getSharedPreferences(PREFS_NUDGE, Context.MODE_PRIVATE)
            val msg = sp.getString(KEY_ACTIVE_NUDGE, null) ?: return null
            val exp = sp.getLong(KEY_ACTIVE_EXPIRY, 0L)
            if (System.currentTimeMillis() > exp) {
                sp.edit().remove(KEY_ACTIVE_NUDGE).remove(KEY_ACTIVE_EXPIRY).apply()
                return null
            }
            return msg
        }

        fun dismissActiveNudge(context: Context) {
            context.getSharedPreferences(PREFS_NUDGE, Context.MODE_PRIVATE)
                .edit().remove(KEY_ACTIVE_NUDGE).remove(KEY_ACTIVE_EXPIRY).apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == ACTION_NIGHT_NUDGE) {
            val msg = intent.getStringExtra("nudge_msg") ?: "Night wind-down reminder."
            val dur = intent.getLongExtra("nudge_dur", 25 * 60 * 1000L)
            context.getSharedPreferences(PREFS_NUDGE, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_NUDGE, msg)
                .putLong(KEY_ACTIVE_EXPIRY, System.currentTimeMillis() + dur)
                .apply()
        }
        scheduleNextNudge(context)
    }
}
