package com.rsstudio.rizglow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings

/** Resolve the real app behind an icon label on this device.
 *  Tries implicit probes first (explicit intent to whatever resolves),
 *  then falls back to well-known package names per OEM. */
object IconTargets {
    fun resolve(context: Context, label: String): Intent? {
        val pm = context.packageManager
        val probe: Intent? = when (label) {
            "Phone" -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))
            "Messages" -> Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_MESSAGING")
            "Camera" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            "Photos" -> Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_GALLERY")
            "Clock" -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            "Settings" -> Intent(Settings.ACTION_SETTINGS)
            "Music" -> Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_MUSIC")
            "Browser" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            "Maps" -> Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_MAPS")
            "Mail" -> Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_EMAIL")
            "Calendar" -> Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CALENDAR")
            "Contacts" -> Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CONTACTS")
            else -> null
        }
        if (probe != null) {
            val ai = pm.resolveActivity(probe, 0)?.activityInfo
            if (ai != null) {
                return Intent(Intent.ACTION_MAIN)
                    .setClassName(ai.packageName, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val fallbackPkgs: List<String> = when (label) {
            "Phone" -> listOf("com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer")
            "Messages" -> listOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging")
            "Camera" -> listOf("com.android.camera2", "com.sec.android.app.camera", "com.android.camera")
            "Photos" -> listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.miui.gallery", "com.android.gallery3d")
            "Clock" -> listOf("com.google.android.deskclock", "com.android.deskclock", "com.samsung.android.clock")
            "Settings" -> listOf("com.android.settings")
            "Music" -> listOf("com.google.android.apps.youtube.music", "com.spotify.music", "com.android.music")
            "Browser" -> listOf("com.android.chrome", "com.mi.globalbrowser", "com.sec.android.app.sbrowser", "org.mozilla.firefox")
            "Maps" -> listOf("com.google.android.apps.maps")
            "Mail" -> listOf("com.google.android.gm", "com.microsoft.office.outlook")
            "Calendar" -> listOf("com.google.android.calendar")
            "Contacts" -> listOf("com.google.android.contacts", "com.android.contacts")
            "Calculator" -> listOf("com.google.android.calculator", "com.android.calculator2")
            "Files" -> listOf("com.google.android.apps.nbu.files", "com.android.documentsui")
            "Video" -> listOf("com.google.android.youtube")
            "Games" -> listOf("com.google.android.play.games")
            "Recorder" -> listOf("com.google.android.apps.recorder")
            "Weather" -> listOf("com.google.android.apps.weather")
            "Notes" -> listOf("com.google.android.keep")
            "Reminders" -> listOf("com.google.android.apps.tasks")
            "Health" -> listOf("com.google.android.apps.healthdata", "com.fitbit.FitbitMobile")
            "Wallet" -> listOf("com.google.android.apps.walletnfcrel")
            "WhatsApp" -> listOf("com.whatsapp")
            "WhatsApp Business" -> listOf("com.whatsapp.w4b")
            "Instagram" -> listOf("com.instagram.android")
            "TikTok" -> listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
            "Facebook" -> listOf("com.facebook.katana")
            "X" -> listOf("com.twitter.android")
            "Snapchat" -> listOf("com.snapchat.android")
            "Pinterest" -> listOf("com.pinterest")
            "Telegram" -> listOf("org.telegram.messenger")
            "Google" -> listOf("com.google.android.googlequicksearchbox")
            else -> emptyList()
        }
        for (pkg in fallbackPkgs) {
            val li = pm.getLaunchIntentForPackage(pkg)
            if (li != null) return li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return null
    }
}
