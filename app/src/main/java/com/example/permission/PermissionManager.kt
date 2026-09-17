package com.example.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.accessibility.BasitAccessibilityService
import com.example.model.PermissionItem

class PermissionManager(private val context: Context) {

    fun getPermissionsList(): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        list.add(
            PermissionItem(
                id = "mic",
                nameKurdish = "مایکڕۆفۆن",
                descriptionKurdish = "پێویستە بۆ وەرگرتنی فەرمانە دەنگییەکان بە زمانی کوردی سۆرانی و گفتوگۆ.",
                iconName = "mic",
                isGranted = isPermissionGranted(Manifest.permission.RECORD_AUDIO),
                androidPermission = Manifest.permission.RECORD_AUDIO
            )
        )

        // 2. Accessibility
        list.add(
            PermissionItem(
                id = "accessibility",
                nameKurdish = "خزمەتگوزاری ئاسانکاری (Accessibility)",
                descriptionKurdish = "ڕێگە دەدات بە BASOKA دەقی شاشە بخوێنێتەوە، دوگمەکان دابگرێت، بگەڕێتەوە، و شاشە سکرۆڵ بکات.",
                iconName = "accessibility",
                isGranted = BasitAccessibilityService.isEnabled(),
                isSpecialSettings = true
            )
        )

        // 3. Camera
        list.add(
            PermissionItem(
                id = "camera",
                nameKurdish = "کامێرا",
                descriptionKurdish = "بۆ وێنەگرتن بە فەرمانی دەنگی و شیکارکردنی دەق و وێنە بە زیرەکی دەستکرد.",
                iconName = "camera",
                isGranted = isPermissionGranted(Manifest.permission.CAMERA),
                androidPermission = Manifest.permission.CAMERA
            )
        )

        // 4. Contacts
        list.add(
            PermissionItem(
                id = "contacts",
                nameKurdish = "ناوەکان و پەیوەندییەکان",
                descriptionKurdish = "بۆ دۆزینەوەی ناوی کەسەکان کاتێک داوای پەیوەندی یان ناردنی نامە دەکەیت.",
                iconName = "contacts",
                isGranted = isPermissionGranted(Manifest.permission.READ_CONTACTS),
                androidPermission = Manifest.permission.READ_CONTACTS
            )
        )

        // 5. Phone Call
        list.add(
            PermissionItem(
                id = "call",
                nameKurdish = "پەیوەندی تەلەفۆنی",
                descriptionKurdish = "بۆ پەیوەندیکردنی ڕاستەوخۆ بە ژمارە و کەسەکان دوای دڵنیابوونەوە لە بەکارهێنەر.",
                iconName = "call",
                isGranted = isPermissionGranted(Manifest.permission.CALL_PHONE),
                androidPermission = Manifest.permission.CALL_PHONE
            )
        )

        // 6. Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    id = "notifications",
                    nameKurdish = "ئاگادارکردنەوەکان (Notifications)",
                    descriptionKurdish = "بۆ پیشاندانی دۆخی باسط، زەنگی ئاگادارکەرەوە، و بیرخەرەوەکان.",
                    iconName = "notifications",
                    isGranted = isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS),
                    androidPermission = Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }

        // 7. Write Settings (Brightness)
        val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true
        list.add(
            PermissionItem(
                id = "system_settings",
                nameKurdish = "گۆڕینی ڕێکخستنی سیستەم",
                descriptionKurdish = "بۆ زیادکردن یان کەمکردنەوەی ڕاستەوخۆی ڕووناکی شاشە بە فەرمانی دەنگی.",
                iconName = "settings",
                isGranted = canWrite,
                isSpecialSettings = true
            )
        )

        return list
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getSettingsIntentFor(item: PermissionItem): Intent {
        return when (item.id) {
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            "system_settings" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:" + context.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } else {
                    Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }
            else -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
    }
}
