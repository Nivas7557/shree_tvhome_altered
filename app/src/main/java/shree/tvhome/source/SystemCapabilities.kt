package shree.tvhome.source

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.Settings

object SystemCapabilities {

    data class DiscoveredSetting(
        val icon: String,
        val title: String,
        val subtitle: String,
        val intent: Intent
    )

    data class DiscoveredTvInput(
        val label: String,
        val id: String,
        val setupIntent: Intent?
    )

    data class DiscoveredAudioDevice(
        val typeName: String,
        val productName: String,
        val id: Int
    )

    fun getAvailableSettings(ctx: Context, filterActions: Set<String>? = null): List<DiscoveredSetting> {
        val pm = ctx.packageManager
        val catalog = listOf(
            DiscoveredSetting("⚙", "TV Settings", "System & device preferences", Intent(Settings.ACTION_SETTINGS)),
            DiscoveredSetting("📺", "Display Settings", "Screen, resolution & display options", Intent(Settings.ACTION_DISPLAY_SETTINGS)),
            DiscoveredSetting("🔊", "Sound Settings", "Audio & volume system settings", Intent(Settings.ACTION_SOUND_SETTINGS)),
            DiscoveredSetting("📶", "Network Settings", "Wi-Fi & network settings", Intent(Settings.ACTION_WIFI_SETTINGS)),
            DiscoveredSetting("🔵", "Bluetooth Settings", "Pair or manage Bluetooth devices", Intent(Settings.ACTION_BLUETOOTH_SETTINGS)),
            DiscoveredSetting("📦", "Installed Apps", "System application manager", Intent(Settings.ACTION_APPLICATION_SETTINGS)),
            DiscoveredSetting("🔔", "Notification Access", "System notification listeners", Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        )
        return catalog.filter { item ->
            val actionMatches = filterActions == null || item.intent.action in filterActions
            actionMatches && item.intent.resolveActivity(pm) != null
        }
    }

    fun getTvInputs(ctx: Context): List<DiscoveredTvInput> {
        val tvMgr = ctx.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            tvMgr.tvInputList.map { info ->
                val lbl = try { info.loadLabel(ctx).toString() } catch (_: Exception) { info.id }
                DiscoveredTvInput(
                    label = lbl,
                    id = info.id,
                    setupIntent = try { info.createSetupIntent() } catch (_: Exception) { null }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getAudioOutputs(ctx: Context): List<DiscoveredAudioDevice> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return emptyList()
        return try {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .distinctBy { it.type }
                .map { dev ->
                    val typeLabel = mapAudioDeviceType(dev.type)
                    val prod = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: typeLabel
                    DiscoveredAudioDevice(typeLabel, prod, dev.id)
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun mapAudioDeviceType(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Audio"
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI Audio"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Audio"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Audio"
        else -> "Built-in Speaker"
    }
}
