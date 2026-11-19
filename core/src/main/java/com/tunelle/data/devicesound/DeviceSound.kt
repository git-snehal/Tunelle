package com.tunelle.data.devicesound

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import com.tunelle.data.DebugLog as Log
import com.tunelle.data.settings.AppSettings
import com.tunelle.data.settings.EqualizerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** What kind of output the music is going to. */
@Serializable
enum class OutputKind(val label: String) {
    SPEAKER("Phone speaker"),
    WIRED("Wired headphones"),
    BLUETOOTH("Bluetooth"),
    USB("USB audio"),
}

/** One output Tunelle can remember a sound for. [key] tells devices apart; [name] is shown. */
@Serializable
data class OutputDevice(val key: String, val name: String, val kind: OutputKind)

/** The sound saved for one output: the whole equalizer (bands, tone, balance, headphone correction) and bass boost. */
@Serializable
data class DeviceProfile(
    val device: OutputDevice,
    val equalizer: EqualizerSettings,
    val bassBoost: Int = 0,
    val savedAt: Long = 0,
)

/**
 * Sound per device. Each output (the phone's speaker, wired headphones, each
 * Bluetooth device by name, each USB DAC) keeps its own equalizer. When the
 * music moves to another output, the sound used on the one it left is saved
 * for it, and the new one's saved sound is put in place; a device heard for
 * the first time starts from the sound that was playing.
 *
 * It works by swapping the app's one equalizer setting, so every existing
 * control (the 15 bands, bass and treble, balance, presets, AutoEq headphone
 * correction, bass boost) is per-device without knowing it.
 */
object DeviceSound {
    private const val TAG = "DeviceSound"
    private const val PREFS = "device_sound"
    private const val K_ENABLED = "enabled"
    private const val K_PROFILES = "profiles"
    private const val K_LAST = "last"

    private val json = AppSettings.json

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val speaker = OutputDevice("speaker", OutputKind.SPEAKER.label, OutputKind.SPEAKER)
    private val _current = MutableStateFlow(speaker)
    /** Where the music is going now. */
    val current: StateFlow<OutputDevice> = _current.asStateFlow()

    private val _profiles = MutableStateFlow<Map<String, DeviceProfile>>(emptyMap())
    /** Saved sounds by [OutputDevice.key]. The current device's live sound is in AppSettings, not here. */
    val profiles: StateFlow<Map<String, DeviceProfile>> = _profiles.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null
    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    /** The output whose sound AppSettings holds now. */
    private var last: OutputDevice? = null

    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        appContext = app
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _enabled.value = p.getBoolean(K_ENABLED, true)
        _profiles.value = runCatching {
            json.decodeFromString(ListSerializer(DeviceProfile.serializer()), p.getString(K_PROFILES, "[]")!!)
        }.getOrDefault(emptyList()).associateBy { it.device.key }
        last = p.getString(K_LAST, null)?.let { runCatching { json.decodeFromString(OutputDevice.serializer(), it) }.getOrNull() }
        val am = app.getSystemService(AudioManager::class.java) ?: return
        audioManager = am
        refresh(announce = false)
        am.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh(announce = true)
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh(announce = true)
        }, Handler(Looper.getMainLooper()))
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        prefs?.edit { putBoolean(K_ENABLED, on) }
        // From now on, what's playing belongs to the device in use.
        if (on) remember(_current.value)
    }

    /** Forgets [key]'s saved sound; it starts again from whatever is playing next time it connects. */
    fun forget(key: String) {
        _profiles.value = _profiles.value - key
        saveProfiles()
    }

    /** Every device with a sound: the saved ones, and the one in use with its live settings. */
    fun all(): List<DeviceProfile> {
        val now = _current.value
        val live = DeviceProfile(now, AppSettings.equalizer.value, AppSettings.sound.value.bassBoost, System.currentTimeMillis())
        return (listOf(live) + _profiles.value.values.filter { it.device.key != now.key }.sortedByDescending { it.savedAt })
    }

    private fun refresh(announce: Boolean) {
        val am = audioManager ?: return
        val device = pick(am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty().toList())
        val changed = device.key != _current.value.key
        _current.value = device
        if (!_enabled.value) return
        val previous = last
        if (previous == null) { remember(device); return }
        if (previous.key == device.key) return
        // Keep the sound used on the output just left, then bring in this one's.
        store(previous)
        val saved = _profiles.value[device.key]
        if (saved != null) {
            AppSettings.updateEqualizer { saved.equalizer }
            AppSettings.updateSound { it.copy(bassBoost = saved.bassBoost) }
            Log.i(TAG, "Sound for ${device.name}")
            if (announce && changed) com.tunelle.data.Pills.info(com.tunelle.i18n.Tr.f("Sound for {0}", device.name), appContext)
        } else {
            Log.i(TAG, "First time on ${device.name}; starting from the current sound")
        }
        remember(device)
        store(device)
    }

    /** Saves AppSettings' current sound as [device]'s. */
    private fun store(device: OutputDevice) {
        _profiles.value = _profiles.value + (device.key to DeviceProfile(
            device, AppSettings.equalizer.value, AppSettings.sound.value.bassBoost, System.currentTimeMillis(),
        ))
        saveProfiles()
    }

    private fun remember(device: OutputDevice) {
        last = device
        prefs?.edit { putString(K_LAST, json.encodeToString(OutputDevice.serializer(), device)) }
    }

    private fun saveProfiles() {
        prefs?.edit { putString(K_PROFILES, json.encodeToString(ListSerializer(DeviceProfile.serializer()), _profiles.value.values.toList())) }
    }

    /**
     * The output media plays through, as Android routes it: a USB DAC first,
     * then Bluetooth, then wired headphones, else the speaker.
     */
    internal fun pick(outputs: List<AudioDeviceInfo>): OutputDevice {
        val sinks = outputs.filter { it.isSink }
        sinks.firstOrNull { it.type in USB }?.let { return OutputDevice("usb:${nameOf(it, "USB audio")}", nameOf(it, "USB audio"), OutputKind.USB) }
        sinks.firstOrNull { it.type in BLUETOOTH }?.let { return OutputDevice("bt:${nameOf(it, "Bluetooth")}", nameOf(it, "Bluetooth"), OutputKind.BLUETOOTH) }
        sinks.firstOrNull { it.type in WIRED }?.let { return OutputDevice("wired", OutputKind.WIRED.label, OutputKind.WIRED) }
        return speaker
    }

    private fun nameOf(d: AudioDeviceInfo, fallback: String): String =
        d.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() && !it.equals(android.os.Build.MODEL, ignoreCase = true) } ?: fallback

    private val USB = setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET)
    private val BLUETOOTH = setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER)
    private val WIRED = setOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL, AudioDeviceInfo.TYPE_AUX_LINE)
}
