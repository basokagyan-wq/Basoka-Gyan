package com.example.executor

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.accessibility.BasitAccessibilityService
import com.example.model.ParsedCommand
import java.io.File

data class ExecutionResult(
    val success: Boolean,
    val kurdishFeedback: String,
    val recoveryIntent: Intent? = null,
    val extraData: String? = null
)

class PhoneActionExecutor(private val context: Context) {

    fun execute(command: ParsedCommand): ExecutionResult {
        return try {
            when (command.intent) {
                "open_app" -> handleOpenApp(command)
                "open_settings" -> handleOpenSettings(command)
                "change_volume" -> handleChangeVolume(command)
                "change_brightness" -> handleChangeBrightness(command)
                "open_camera", "take_photo" -> handleCamera(command)
                "toggle_flashlight", "turn_on_flashlight", "turn_off_flashlight", "flashlight" -> handleFlashlight(command)
                "media_play", "media_pause", "media_next", "media_prev" -> handleMediaControl(command)
                "make_call" -> handleMakeCall(command)
                "send_message" -> handleSendMessage(command)
                "set_alarm", "create_reminder" -> handleSetAlarm(command)
                "search_web" -> handleWebSearch(command)
                "show_notifications" -> handleShowNotifications()
                "read_screen" -> handleReadScreen()
                "go_back" -> handleAccessibilityAction { it.executeBack() }
                "go_home" -> handleAccessibilityAction { it.executeHome() }
                "show_recents" -> handleAccessibilityAction { it.executeRecents() }
                "scroll" -> handleAccessibilityAction { it.scrollScreen(command.target == "down") }
                "click_element" -> handleClickElement(command.target)
                "device_info" -> handleDeviceInfo()
                "clipboard_read" -> handleClipboardRead()
                "clipboard_copy" -> handleClipboardCopy(command.parameter)
                "open_file" -> handleOpenFile()
                else -> ExecutionResult(
                    success = true,
                    kurdishFeedback = command.kurdishSpeech.ifBlank { "فەرمانەکە پێداچوونەوەی بۆ کرا." }
                )
            }
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                kurdishFeedback = "نەتوانرا ئەم کردارە جێبەجێبکرێت: ${e.localizedMessage}"
            )
        }
    }

    private fun handleOpenApp(command: ParsedCommand): ExecutionResult {
        val target = command.target.lowercase()

        if (target == "google_maps" || target.contains("map")) {
            val query = command.parameter
            val mapUri = if (query.isNotBlank()) {
                Uri.parse("geo:0,0?q=" + Uri.encode(query))
            } else {
                Uri.parse("geo:0,0?q=")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return if (canLaunch(mapIntent)) {
                context.startActivity(mapIntent)
                ExecutionResult(true, "Google Maps کرایەوە.")
            } else {
                launchPackageOrPlayStore("com.google.android.apps.maps", "Google Maps")
            }
        }

        if (target == "youtube") {
            val query = command.parameter
            val ytIntent = if (query.isNotBlank()) {
                Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            }
            if (ytIntent != null && canLaunch(ytIntent)) {
                context.startActivity(ytIntent)
                return ExecutionResult(true, "YouTube کرایەوە.")
            }
            val webYt = Intent(Intent.ACTION_VIEW, Uri.parse(if (query.isNotBlank()) "https://www.youtube.com/results?search_query=${Uri.encode(query)}" else "https://www.youtube.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webYt)
            return ExecutionResult(true, "YouTube لە وێبگەڕ کرایەوە.")
        }

        // General package or known app
        val pkg = when {
            target.contains("whatsapp") -> "com.whatsapp"
            target.contains("telegram") -> "org.telegram.messenger"
            target.contains("chrome") -> "com.android.chrome"
            else -> command.target
        }

        return launchPackageOrPlayStore(pkg, command.target)
    }

    private fun launchPackageOrPlayStore(pkgName: String, displayName: String): ExecutionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkgName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ExecutionResult(true, "بەرنامەی $displayName کرایەوە.")
        } else {
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkgName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (canLaunch(storeIntent)) {
                ExecutionResult(
                    success = false,
                    kurdishFeedback = "بەرنامەی $displayName دانەمەزراوە لەسەر مۆبایلەکەت. دەتەوێت دایبەزێنیت؟",
                    recoveryIntent = storeIntent
                )
            } else {
                ExecutionResult(
                    success = false,
                    kurdishFeedback = "نەتوانرا بەرنامەی $displayName بدۆزرێتەوە لەسەر ئەم ئامێرە."
                )
            }
        }
    }

    private fun handleOpenSettings(command: ParsedCommand): ExecutionResult {
        val target = command.target.lowercase()
        val intent = when {
            target.contains("wifi") -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_WIFI).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                }
            }
            target.contains("bluetooth") -> {
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
            target.contains("display") || target.contains("brightness") -> {
                Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
            target.contains("sound") -> {
                Intent(Settings.ACTION_SOUND_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
            target.contains("accessibility") -> {
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
            else -> Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }

        context.startActivity(intent)
        return ExecutionResult(
            success = true,
            kurdishFeedback = "پەڕەی ڕێکخستن کرایەوە."
        )
    }

    private fun handleChangeVolume(command: ParsedCommand): ExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (command.target) {
            "mute" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                return ExecutionResult(true, "مۆبایلەکە بێدەنگ کرا.")
            }
            "up" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                return ExecutionResult(true, "دەنگی مۆبایل زیادکرا.")
            }
            "down" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                return ExecutionResult(true, "دەنگی مۆبایل کەمکرایەوە.")
            }
            else -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                return ExecutionResult(true, "کۆنترۆڵی دەنگ پیشاندرا.")
            }
        }
    }

    private fun handleChangeBrightness(command: ParsedCommand): ExecutionResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
            val percent = command.parameter.toIntOrNull() ?: 50
            val brightnessValue = ((percent / 100f) * 255).toInt().coerceIn(10, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
            return ExecutionResult(true, "ڕووناکی شاشە گۆڕدرا بۆ $percent%.")
        } else {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:" + context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
            return ExecutionResult(
                success = false,
                kurdishFeedback = "نەتوانیم ئەم کارە بکەم، چونکە مۆڵەتی پێویست نییە بۆ گۆڕینی ڕووناکی شاشە. تکایە لە ڕێکخستن مۆڵەت بدە.",
                recoveryIntent = intent
            )
        }
    }

    private fun handleCamera(command: ParsedCommand): ExecutionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (canLaunch(intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "کامێرا کرایەوە.")
        } else {
            ExecutionResult(false, "نەتوانرا ئەپی کامێرا بدۆزرێتەوە.")
        }
    }

    private var isTorchActive = false

    private fun handleFlashlight(command: ParsedCommand): ExecutionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    try {
                        val chars = cameraManager.getCameraCharacteristics(id)
                        chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } catch (_: Exception) {
                        false
                    }
                } ?: cameraManager.cameraIdList.firstOrNull() ?: "0"

                val turnOn = when (command.parameter.lowercase()) {
                    "on", "enable", "true", "داگیرساندن", "هەڵکردن" -> true
                    "off", "disable", "false", "کوژاندنەوە" -> false
                    else -> !isTorchActive
                }

                cameraManager.setTorchMode(cameraId, turnOn)
                isTorchActive = turnOn

                val feedback = if (turnOn) "فلایشی مۆبایل داگیرسێندرا." else "فلایشی مۆبایل کوژێندرایەوە."
                ExecutionResult(true, feedback)
            } else {
                ExecutionResult(false, "سیستەمی ئەم مۆبایلە پشتگیری ڕاستەوخۆی فلایش ناکات.")
            }
        } catch (e: Exception) {
            ExecutionResult(false, "نەتوانرا فلایش کۆنترۆڵ بکرێت: ${e.localizedMessage}")
        }
    }

    private fun handleMediaControl(command: ParsedCommand): ExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyCode = when (command.intent) {
            "media_pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "media_next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "media_prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

        return ExecutionResult(
            success = true,
            kurdishFeedback = command.kurdishSpeech.ifBlank { "فەرمانی مۆسیقا پەخشکرا." }
        )
    }

    private fun handleMakeCall(command: ParsedCommand): ExecutionResult {
        val rawTarget = command.target.filter { it.isDigit() || it == '+' }
        val number = if (rawTarget.isNotBlank()) rawTarget else "0750"
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ExecutionResult(true, "پەیوەندی تەلەفۆنی بۆ $number ئامادەکرا.")
    }

    private fun handleSendMessage(command: ParsedCommand): ExecutionResult {
        val contact = command.target
        val text = command.parameter
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contact")).apply {
            putExtra("sms_body", text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (canLaunch(intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "پەڕەی ناردنی نامە کرایەوە بۆ $contact.")
        } else {
            ExecutionResult(false, "نەتوانرا ئەپی نامە دەستنیشان بکرێت.")
        }
    }

    private fun handleSetAlarm(command: ParsedCommand): ExecutionResult {
        val timeParts = command.parameter.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val minutes = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, "یاریدەدەری باسط")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return if (canLaunch(intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "زەنگ بۆ کاتژمێر $hour:${String.format("%02d", minutes)} دانرا.")
        } else {
            val clockIntent = Intent(Settings.ACTION_DATE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            ExecutionResult(
                success = false,
                kurdishFeedback = "نەتوانیم ئەم کارە بکەم، چونکە مۆڵەتی پێویست نییە.",
                recoveryIntent = clockIntent
            )
        }
    }

    private fun handleWebSearch(command: ParsedCommand): ExecutionResult {
        val query = command.parameter.ifBlank { command.target }
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (canLaunch(intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "گەڕان لە گووگڵ کرایەوە بۆ: $query")
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            ExecutionResult(true, "گەڕان بە سەرکەوتوویی کرایەوە.")
        }
    }

    private fun handleShowNotifications(): ExecutionResult {
        val service = BasitAccessibilityService.get()
        return if (service != null) {
            val done = service.executeNotifications()
            if (done) {
                ExecutionResult(true, "نۆتیفیکەیشنەکان پیشان دران.")
            } else {
                ExecutionResult(false, "نەتوانرا نۆتیفیکەیشنەکان بکرێنەوە.")
            }
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ExecutionResult(
                success = false,
                kurdishFeedback = "نەتوانیم ئەم کارە بکەم، چونکە مۆڵەتی پێویست نییە. تکایە خزمەتگوزاری ئاسانکاری (Accessibility) چالاک بکە.",
                recoveryIntent = intent
            )
        }
    }

    private fun handleReadScreen(): ExecutionResult {
        val service = BasitAccessibilityService.get()
        return if (service != null) {
            val data = service.readCurrentScreen()
            val textSummary = buildString {
                append("ناونیشانی شاشە: ${data.title}\n")
                if (data.visibleTexts.isNotEmpty()) {
                    append("دەقە بینراوەکان:\n")
                    data.visibleTexts.take(8).forEach { append("• $it\n") }
                }
                if (data.clickableButtons.isNotEmpty()) {
                    append("دوگمە کرتەکراوەکان: ${data.clickableButtons.take(5).joinToString("، ")}")
                }
            }
            ExecutionResult(
                success = true,
                kurdishFeedback = "دەقی شاشە خوێندرایەوە بە سەرکەوتوویی.",
                extraData = textSummary
            )
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ExecutionResult(
                success = false,
                kurdishFeedback = "نەتوانیم ئەم کارە بکەم، چونکە مۆڵەتی پێویست نییە. بۆ خوێندنەوەی دەقی شاشە پێویستە دەسەڵاتی ئاسانکاری (Accessibility) بدەیت بە باسط.",
                recoveryIntent = intent
            )
        }
    }

    private fun handleClickElement(target: String): ExecutionResult {
        val service = BasitAccessibilityService.get()
        return if (service != null) {
            val clicked = service.clickElementByText(target)
            if (clicked) {
                ExecutionResult(true, "کرتە کرا لەسەر $target.")
            } else {
                ExecutionResult(false, "دوگمەی $target لەسەر شاشەی ئێستا نەدۆزرایەوە.")
            }
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ExecutionResult(
                success = false,
                kurdishFeedback = "نەتوانیم ئەم کارە بکەم، چونکە مۆڵەتی پێویست نییە.",
                recoveryIntent = intent
            )
        }
    }

    private fun handleAccessibilityAction(action: (BasitAccessibilityService) -> Boolean): ExecutionResult {
        val service = BasitAccessibilityService.get()
        return if (service != null) {
            val ok = action(service)
            if (ok) {
                ExecutionResult(true, "کردارەکە بە سەرکەوتوویی ئەنجامدرا.")
            } else {
                ExecutionResult(false, "ئەنجامدانی کردارەکە سەرکەوتوو نەبوو.")
            }
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ExecutionResult(
                success = false,
                kurdishFeedback = "نەتوانیم ئەم کارە بکەم، چونکە مۆڵەتی پێویست نییە. تکایە دەسەڵاتی Accessibility بدە.",
                recoveryIntent = intent
            )
        }
    }

    private fun handleDeviceInfo(): ExecutionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = bm.isCharging

        val statFs = StatFs(Environment.getDataDirectory().path)
        val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
        val freeGb = freeBytes / (1024 * 1024 * 1024)
        val totalGb = totalBytes / (1024 * 1024 * 1024)

        val info = buildString {
            append("مۆدێلی ئامێر: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("وەشانی ئەندرۆید: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("دۆخی باتری: $batteryPct% ${if (isCharging) "(لە بارگاوییکردندایە)" else ""}\n")
            append("کۆگای ئامێر: $freeGb GB بەردەستە لە کۆی $totalGb GB")
        }

        return ExecutionResult(
            success = true,
            kurdishFeedback = "باتری مۆبایلەکەت $batteryPct% ـە و ${freeGb} گێگابایت کۆگا بەردەستە.",
            extraData = info
        )
    }

    private fun handleClipboardRead(): ExecutionResult {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cb.primaryClip
        val text = clip?.getItemAt(0)?.text?.toString()
        return if (!text.isNullOrBlank()) {
            ExecutionResult(
                success = true,
                kurdishFeedback = "دەقی کۆپیکراو لە کلیپبۆرد: $text",
                extraData = text
            )
        } else {
            ExecutionResult(true, "کلیپبۆرد لەم کاتەدا بەتاڵە.")
        }
    }

    private fun handleClipboardCopy(text: String): ExecutionResult {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("Basit AI", text))
        return ExecutionResult(true, "دەقەکە کۆپیکرا بۆ کلیپبۆرد.")
    }

    private fun handleOpenFile(): ExecutionResult {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (canLaunch(intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "بەڕێوەبەری فایلەکان کرایەوە.")
        } else {
            ExecutionResult(false, "نەتوانرا پەڕەی هەڵبژاردنی فایل بکرێتەوە.")
        }
    }

    private fun canLaunch(intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }
}
