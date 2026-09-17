package com.example.ai

import com.example.model.ActionCategory
import com.example.model.ParsedCommand
import java.util.regex.Pattern

object KurdishNaturalCommandEngine {

    fun parseKurdishCommand(rawInput: String): ParsedCommand? {
        val input = rawInput.trim()
        val normalized = normalizeKurdish(input)

        // 1. Wi-Fi
        if (containsAny(normalized, "وایفای", "وای فای", "wifi", "wi-fi", "ئینتەرنێت")) {
            val isOff = containsAny(normalized, "دابخە", "بکوژێنەوە", "بوەستێنە", "ناچالاک", "off", "کوژانەوە")
            return ParsedCommand(
                intent = "open_settings",
                target = "wifi",
                parameter = if (isOff) "disable" else "enable",
                kurdishSpeech = if (isOff) "ڕێکخستنی وایفای دەکەمەوە بۆ کوژاندنەوە." else "ڕێکخستنی وایفای دەکەمەوە بۆ هەڵکردن.",
                kurdishSummary = "کردنەوەی ڕێکخستنی Wi-Fi",
                category = ActionCategory.SETTINGS
            )
        }

        // 2. Bluetooth
        if (containsAny(normalized, "بلووتووس", "بلوتوس", "bluetooth", "بلوتوز")) {
            val isOff = containsAny(normalized, "دابخە", "بکوژێنەوە", "بوەستێنە", "ناچالاک", "off")
            return ParsedCommand(
                intent = "open_settings",
                target = "bluetooth",
                parameter = if (isOff) "disable" else "enable",
                kurdishSpeech = if (isOff) "ڕێکخستنی بلووتووس دەکەمەوە بۆ کوژاندنەوە." else "ڕێکخستنی بلووتووس دەکەمەوە بۆ هەڵکردن.",
                kurdishSummary = "کردنەوەی ڕێکخستنی Bluetooth",
                category = ActionCategory.SETTINGS
            )
        }

        // 3. Google Maps
        if (containsAny(normalized, "گووگڵ مەپ", "گوگل ماپ", "گووگڵ ماپس", "google maps", "maps", "نەخشە", "نەخشەی گووگڵ")) {
            val destination = extractTargetAfterKeywords(input, listOf("بۆ", "لە سەر", "شوێنی", "دەربارەی"))
            return ParsedCommand(
                intent = "open_app",
                target = "google_maps",
                parameter = destination,
                kurdishSpeech = if (destination.isNotBlank()) "Google Maps دەکەمەوە بۆ شوێنی $destination." else "باشە، Google Maps دەکەمەوە.",
                kurdishSummary = "کردنەوەی Google Maps",
                category = ActionCategory.SYSTEM
            )
        }

        // 4. YouTube
        if (containsAny(normalized, "یوتیوب", "یووتیوب", "youtube", "یوتوب")) {
            val searchParam = extractTargetAfterKeywords(input, listOf("بگەڕێ", "سەیری", "گۆرانی", "ڤیدیۆی"))
            return ParsedCommand(
                intent = "open_app",
                target = "youtube",
                parameter = searchParam,
                kurdishSpeech = if (searchParam.isNotBlank()) "یوتیوب دەکەمەوە بۆ گەڕان بەدوای $searchParam." else "باشە، YouTube دەکەمەوە.",
                kurdishSummary = "کردنەوەی YouTube",
                category = ActionCategory.SYSTEM
            )
        }

        // 5. Screen Brightness
        if (containsAny(normalized, "ڕووناکی", "روناکی", "شاشە تاریک", "شاشە ڕووناک", "brightness")) {
            val isIncrease = containsAny(normalized, "زیاد", "بەرز", "زیاتر", "رووناکتر", "ڕووناکتر")
            val isDecrease = containsAny(normalized, "کەم", "نزم", "تاریک", "کەمتر")
            val value = extractNumber(normalized) ?: if (isIncrease) 85 else if (isDecrease) 25 else 50
            return ParsedCommand(
                intent = "change_brightness",
                target = "screen",
                parameter = value.toString(),
                kurdishSpeech = if (isIncrease) "کەمێک ڕووناکی شاشە زیاد دەکەم." else if (isDecrease) "ڕووناکی شاشە کەم دەکەمەوە." else "ڕێکخستنی ڕووناکی شاشە دەکەمەوە.",
                kurdishSummary = "گۆڕینی ڕووناکی شاشە ($value%)",
                category = ActionCategory.SETTINGS
            )
        }

        // 6. Volume Control
        if (containsAny(normalized, "دەنگ زیاد", "دەنگی مۆبایل زیاد", "دەنگ بەرز", "دەنگ کەم", "دەنگ نزم", "بێدەنگ", "volume")) {
            val isMute = containsAny(normalized, "بێدەنگ", "سایلێنت", "mute")
            val isIncrease = containsAny(normalized, "زیاد", "بەرز", "up")
            val isDecrease = containsAny(normalized, "کەم", "نزم", "down")
            return ParsedCommand(
                intent = "change_volume",
                target = if (isMute) "mute" else if (isIncrease) "up" else "down",
                parameter = if (isMute) "0" else if (isIncrease) "+1" else "-1",
                kurdishSpeech = if (isMute) "مۆبایلەکە بێدەنگ دەکەم." else if (isIncrease) "دەنگی مۆبایل زیاد دەکەم." else "دەنگی مۆبایل کەم دەکەمەوە.",
                kurdishSummary = if (isMute) "بێدەنگکردن" else if (isIncrease) "زیادکردنی دەنگ" else "کەمکردنی دەنگ",
                category = ActionCategory.MEDIA
            )
        }

        // 7. Take Photo / Camera
        if (containsAny(normalized, "وێنەیەک بگرە", "وێنە بگرە", "کامێرا بکەرەوە", "کامێرا", "camera", "ڕەسمێک بگرە")) {
            return ParsedCommand(
                intent = "open_camera",
                target = "camera",
                parameter = if (containsAny(normalized, "بگرە")) "capture" else "launch",
                kurdishSpeech = "کامێرا دەکەمەوە بۆ گرتنی وێنە.",
                kurdishSummary = "کردنەوەی کامێرا",
                category = ActionCategory.SYSTEM
            )
        }

        // 7b. Flashlight / Torch
        if (containsAny(normalized, "فلایش", "فلاش", "لایت", "گڵۆپ", "تۆڕچ", "flashlight", "torch")) {
            val isOff = containsAny(normalized, "بکوژێنەوە", "دابخە", "کوژانەوە", "کوژاندنەوە", "off", "بکوژێنە")
            val param = if (isOff) "off" else "on"
            return ParsedCommand(
                intent = "toggle_flashlight",
                target = "flashlight",
                parameter = param,
                kurdishSpeech = if (isOff) "فلایشی مۆبایل دەکوژێنمەوە." else "فلایشی مۆبایل دادەگیرسێنم.",
                kurdishSummary = if (isOff) "کوژاندنەوەی فلایش" else "داگیرساندنی فلایش",
                category = ActionCategory.SETTINGS
            )
        }

        // 8. Music / Media Playback
        if (containsAny(normalized, "مۆسیقا", "گۆرانی", "میوزیک", "music", "audio")) {
            val isPause = containsAny(normalized, "بوەستێنە", "ڕاگرە", "دابخە", "pause", "stop")
            val isNext = containsAny(normalized, "دواتر", "داهاتوو", "دواترین", "next")
            val isPrev = containsAny(normalized, "پێشوو", "پێشتر", "previous")
            val intent = when {
                isNext -> "media_next"
                isPrev -> "media_prev"
                isPause -> "media_pause"
                else -> "media_play"
            }
            val kurdishSpeech = when {
                isNext -> "گۆرانی داهاتوو پەخش دەکەم."
                isPrev -> "گۆرانی پێشوو لێدەدەمەوە."
                isPause -> "مۆسیقاکە دەوەستێنم."
                else -> "مۆسیقا پەخش دەکەم."
            }
            return ParsedCommand(
                intent = intent,
                target = "media",
                kurdishSpeech = kurdishSpeech,
                kurdishSummary = kurdishSpeech,
                category = ActionCategory.MEDIA
            )
        }

        // 9. Send Message (SMS) - Sensitive (Confirmation Required)
        if (containsAny(normalized, "نامەیەک", "نامە بنێرە", "مەسج بنێرە", "sms", "پیام")) {
            val contact = extractContactName(normalized, listOf("بۆ", "بە"))
            return ParsedCommand(
                intent = "send_message",
                target = contact.ifBlank { "کەسی دیاریکراو" },
                parameter = input,
                requiresConfirmation = true,
                kurdishSpeech = "ئایا دڵنیایت دەتەوێت نامە بۆ $contact بنێریت؟",
                kurdishSummary = "ناردنی نامە بۆ $contact",
                category = ActionCategory.COMMUNICATION
            )
        }

        // 10. Make Phone Call - Sensitive (Confirmation Required)
        if (containsAny(normalized, "تەلەفۆن بۆ", "تەلەفۆن بکە", "پەیوەندی بە", "پەیوەندی بکە", "پەیوەندی لەگەڵ", "call", "تەلەفون")) {
            val contact = extractContactName(normalized, listOf("بۆ", "بە", "لەگەڵ"))
            return ParsedCommand(
                intent = "make_call",
                target = contact.ifBlank { "کەسی داواکراو" },
                parameter = contact,
                requiresConfirmation = true,
                kurdishSpeech = "ئایا پەیوەندی بە $contact بکەم؟",
                kurdishSummary = "پەیوەندی تەلەفۆنی بە $contact",
                category = ActionCategory.COMMUNICATION
            )
        }

        // 11. Alarm / Reminder
        if (containsAny(normalized, "بیرم بخەرەوە", "زەنگ", "ئاگادارم بکەرەوە", "ئاگادارکەرەوە", "alarm", "reminder")) {
            val hour = extractHour(normalized) ?: 8
            val isMorning = containsAny(normalized, "بەیانی", "am")
            val isEvening = containsAny(normalized, "ئێوارە", "شەو", "pm")
            val finalHour = if (isEvening && hour < 12) hour + 12 else hour
            return ParsedCommand(
                intent = "set_alarm",
                target = "alarm",
                parameter = "$finalHour:00",
                kurdishSpeech = "باشە، کاتژمێر $finalHour بیرت دەخەمەوە و زەنگ دادەنێم.",
                kurdishSummary = "دانانی زەنگ بۆ کاتژمێر $finalHour:00",
                category = ActionCategory.ALARM_REMINDER
            )
        }

        // 12. Close App / Go Back
        if (containsAny(normalized, "ئەم ئەپە دابخە", "ئەپەکە دابخە", "دابخە", "دەرچۆ", "close app", "بڕۆ دەرەوە")) {
            return ParsedCommand(
                intent = "go_home",
                target = "home",
                kurdishSpeech = "باشە، ئەپەکە دادەخرێت و دەگەڕێمەوە شاشەی سەرەکی.",
                kurdishSummary = "دەرچوون بۆ شاشەی سەرەکی",
                category = ActionCategory.ACCESSIBILITY
            )
        }

        // 13. Web Search
        if (containsAny(normalized, "بگەڕێ بەدوای", "لە google", "لە گووگڵ", "search", "گەڕان لە ئینتەرنێت")) {
            val query = extractTargetAfterKeywords(input, listOf("بەدوای", "دەربارەی", "لە google", "لە گووگڵ", "بگەڕێ"))
            val finalQuery = if (query.isNotBlank()) query else input
            return ParsedCommand(
                intent = "search_web",
                target = "google",
                parameter = finalQuery,
                kurdishSpeech = "لە Google دەگەڕێم بەدوای: $finalQuery.",
                kurdishSummary = "گەڕان لە Google: $finalQuery",
                category = ActionCategory.SYSTEM
            )
        }

        // 14. Notifications
        if (containsAny(normalized, "notification", "نۆتیفیکەیشن", "ئاگاداریەکان", "ئاگادارکردنەوەکانم")) {
            return ParsedCommand(
                intent = "show_notifications",
                target = "notifications",
                kurdishSpeech = "هەموو نۆتیفیکەیشنەکانت پیشان دەدەم.",
                kurdishSummary = "پیشاندانی پەڕەی نۆتیفیکەیشن",
                category = ActionCategory.ACCESSIBILITY
            )
        }

        // 15. Read Screen / Accessibility
        if (containsAny(normalized, "دەقی شاشە", "شاشە بخوێنەوە", "شاشەم بۆ بخوێنەوە", "read screen", "دەقی سەر شاشە")) {
            return ParsedCommand(
                intent = "read_screen",
                target = "screen_content",
                kurdishSpeech = "دەقی شاشەی ئێستا دەخوێنمەوە...",
                kurdishSummary = "خوێندنەوەی دەقی سەر شاشە",
                category = ActionCategory.ACCESSIBILITY
            )
        }

        // 16. Accessibility Global Actions: Back, Home, Recents, Scroll, Click
        if (containsAny(normalized, "بگەڕێوە دواوە", "بڕۆ دواوە", "بگەڕێوە", "go back")) {
            return ParsedCommand(
                intent = "go_back",
                target = "back",
                kurdishSpeech = "دەگەڕێمەوە دواوە.",
                kurdishSummary = "گەڕانەوە بۆ دواوە",
                category = ActionCategory.ACCESSIBILITY
            )
        }
        if (containsAny(normalized, "بڕۆ سەرەتا", "شاشەی سەرەکی", "هۆم", "go home")) {
            return ParsedCommand(
                intent = "go_home",
                target = "home",
                kurdishSpeech = "دەچمەوە شاشەی سەرەکی مۆبایل.",
                kurdishSummary = "چوونە شاشەی سەرەکی",
                category = ActionCategory.ACCESSIBILITY
            )
        }
        if (containsAny(normalized, "ئەپە کراوەکان", "بەرنامە کراوەکان", "ڕیسێنت", "recents")) {
            return ParsedCommand(
                intent = "show_recents",
                target = "recents",
                kurdishSpeech = "لیستی بەرنامە کراوەکان پیشان دەدەم.",
                kurdishSummary = "پیشاندانی بەرنامە کراوەکان",
                category = ActionCategory.ACCESSIBILITY
            )
        }
        if (containsAny(normalized, "شاشە دابگرە خوارەوە", "بڕۆ خوارەوە", "سکرۆڵ خوارەوە", "scroll down")) {
            return ParsedCommand(
                intent = "scroll",
                target = "down",
                kurdishSpeech = "شاشە دابەزێندرا بۆ خوارەوە.",
                kurdishSummary = "سکرۆڵ بۆ خوارەوە",
                category = ActionCategory.ACCESSIBILITY
            )
        }
        if (containsAny(normalized, "شاشە ببە سەرەوە", "بڕۆ سەرەوە", "سکرۆڵ سەرەوە", "scroll up")) {
            return ParsedCommand(
                intent = "scroll",
                target = "up",
                kurdishSpeech = "شاشە بەرزکرایەوە بۆ سەرەوە.",
                kurdishSummary = "سکرۆڵ بۆ سەرەوە",
                category = ActionCategory.ACCESSIBILITY
            )
        }
        if (containsAny(normalized, "کرتە بکە لەسەر", "پەنجە بنێ بە", "دابگرە لەسەر", "click")) {
            val element = extractTargetAfterKeywords(input, listOf("لەسەر", "بە", "دوگمەی"))
            return ParsedCommand(
                intent = "click_element",
                target = element,
                kurdishSpeech = "کرتە دەکەم لەسەر $element.",
                kurdishSummary = "کرتەکردن لەسەر $element",
                category = ActionCategory.ACCESSIBILITY
            )
        }

        // 17. Device Information & Battery
        if (containsAny(normalized, "باتری", "شەحن", "زانیاری مۆبایل", "دۆخی مۆبایل", "کۆگا", "مۆبایلەکەم")) {
            return ParsedCommand(
                intent = "device_info",
                target = "battery_storage",
                kurdishSpeech = "زانیاریەکانی مۆبایل و دۆخی باتری دەپشکنم.",
                kurdishSummary = "پشکنینی دۆخی باتری و مۆبایل",
                category = ActionCategory.SYSTEM
            )
        }

        // 18. Open Other Popular Apps
        if (containsAny(normalized, "واتسئەپ", "واتساپ", "whatsapp")) {
            return ParsedCommand(
                intent = "open_app",
                target = "com.whatsapp",
                kurdishSpeech = "WhatsApp دەکەمەوە.",
                kurdishSummary = "کردنەوەی WhatsApp",
                category = ActionCategory.SYSTEM
            )
        }
        if (containsAny(normalized, "تێلیگرام", "تێلەگرام", "telegram")) {
            return ParsedCommand(
                intent = "open_app",
                target = "org.telegram.messenger",
                kurdishSpeech = "Telegram دەکەمەوە.",
                kurdishSummary = "کردنەوەی Telegram",
                category = ActionCategory.SYSTEM
            )
        }
        if (containsAny(normalized, "کرۆم", "chrome", "براوسەر", "وێبگەڕ")) {
            return ParsedCommand(
                intent = "open_app",
                target = "com.android.chrome",
                kurdishSpeech = "گووگڵ کرۆم دەکەمەوە.",
                kurdishSummary = "کردنەوەی Chrome",
                category = ActionCategory.SYSTEM
            )
        }
        if (containsAny(normalized, "فایل", "دۆکیۆمێنت", "فۆڵدەر", "files")) {
            return ParsedCommand(
                intent = "open_file",
                target = "files",
                kurdishSpeech = "فایلەکانی مۆبایل دەکەمەوە.",
                kurdishSummary = "کردنەوەی بەڕێوەبەری فایل",
                category = ActionCategory.SYSTEM
            )
        }

        // 19. Clipboard Operations
        if (containsAny(normalized, "کلیپبۆرد", "کۆپی بکە", "دەقی کۆپیکراو")) {
            return ParsedCommand(
                intent = "clipboard_read",
                target = "clipboard",
                kurdishSpeech = "دەقی کۆپیکراوی ناو کلیپبۆرد دەپشکنم.",
                kurdishSummary = "خوێندنەوەی کلیپبۆرد",
                category = ActionCategory.SYSTEM
            )
        }

        // 20. Analyze Image / Translation
        if (containsAny(normalized, "وێنە بخوێنەوە", "وێنەکە بخوێنەوە", "شیکارکردنی وێنە", "ئەم وێنەیە بۆم بخوێنەوە")) {
            return ParsedCommand(
                intent = "analyze_image",
                target = "current_image",
                kurdishSpeech = "تکایە وێنەیەک دیاریبکە تاوەکو دەق و ناوەڕۆکەکەی بە کوردی بۆ شیکاربکەم.",
                kurdishSummary = "شیکارکردنی وێنە بە AI",
                category = ActionCategory.AI_VISION
            )
        }
        if (containsAny(normalized, "وەرگێڕە", "وەرگێڕان", "تەرجومە", "translate")) {
            val textToTranslate = extractTargetAfterKeywords(input, listOf("وەرگێڕە", "وەرگێڕانی", "تەرجومەی"))
            return ParsedCommand(
                intent = "translate_text",
                target = "kurdish",
                parameter = textToTranslate,
                kurdishSpeech = "دەقەکە وەردەگێڕم بۆ کوردی سۆرانی.",
                kurdishSummary = "وەرگێڕانی دەق بۆ کوردی",
                category = ActionCategory.AI_VISION
            )
        }

        // 21. Identity & Introduction
        if (containsAny(normalized, "تۆ کێیت", "ناوت چییە", "کێیت تۆ", "ناساندن", "بەرنامەکە چییە", "دەربارەی تۆ", "باسۆکا", "basoka")) {
            return ParsedCommand(
                intent = "assistant_identity",
                target = "basoka",
                kurdishSpeech = "من BASOKA ـم، یاریدەدەری زیرەکی مۆبایلەکەت بە زمانی کوردی سۆرانی بۆ جێبەجێکردنی فەرمانەکان و کۆنترۆڵی مۆبایل.",
                kurdishSummary = "ناساندنی یاریدەدەری BASOKA",
                category = ActionCategory.SYSTEM
            )
        }

        return null
    }

    private fun normalizeKurdish(str: String): String {
        return str.lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ە', 'ە')
            .replace('ۆ', 'ۆ')
            .replace('ێ', 'ێ')
            .replace('ڕ', 'ڕ')
            .replace('ڵ', 'ڵ')
            .replace("ـ", "")
            .trim()
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun extractTargetAfterKeywords(original: String, keywords: List<String>): String {
        for (keyword in keywords) {
            val idx = original.indexOf(keyword, ignoreCase = true)
            if (idx != -1) {
                val candidate = original.substring(idx + keyword.length).trim()
                if (candidate.isNotBlank()) {
                    return candidate
                }
            }
        }
        return ""
    }

    private fun extractContactName(text: String, prefixes: List<String>): String {
        for (prefix in prefixes) {
            val p = Pattern.compile("(?:$prefix)\\s+([\\w\\p{L}]+)", Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
            val m = p.matcher(text)
            if (m.find()) {
                val name = m.group(1)?.trim() ?: ""
                if (name.isNotBlank() && !listOf("ئەم", "کەسە", "یەکێک").contains(name)) {
                    return name
                }
            }
        }
        return "کەسی پەیوەندیدار"
    }

    private fun extractHour(text: String): Int? {
        val kurdishNumbers = mapOf(
            "١" to 1, "٢" to 2, "٣" to 3, "٤" to 4, "٥" to 5,
            "٦" to 6, "٧" to 7, "٨" to 8, "٩" to 9, "١٠" to 10,
            "١١" to 11, "١٢" to 12, "یەک" to 1, "دوو" to 2, "سێ" to 3,
            "چوار" to 4, "پێنج" to 5, "شەش" to 6, "حەوت" to 7, "هەشت" to 8,
            "نۆ" to 9, "دە" to 10, "یازدە" to 11, "دوازدە" to 12
        )
        for ((word, num) in kurdishNumbers) {
            if (text.contains(word)) return num
        }
        val digitMatch = Pattern.compile("(\\d{1,2})").matcher(text)
        if (digitMatch.find()) {
            return digitMatch.group(1)?.toIntOrNull()?.coerceIn(0, 23)
        }
        return null
    }

    private fun extractNumber(text: String): Int? {
        val digitMatch = Pattern.compile("(\\d{1,3})").matcher(text)
        if (digitMatch.find()) {
            return digitMatch.group(1)?.toIntOrNull()
        }
        return null
    }
}
