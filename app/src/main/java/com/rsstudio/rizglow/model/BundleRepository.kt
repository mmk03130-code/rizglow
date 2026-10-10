package com.rsstudio.rizglow.model

import com.rsstudio.rizglow.R

/** Bundle catalog. All three launch bundles ship with full artwork. */
object BundleRepository {

    private val mvWallpapers = listOf(
        R.drawable.wp_mv_cat,
        R.drawable.wp_mv_forest,
        R.drawable.wp_mv_velvet,
        R.drawable.wp_mv_eye,
        R.drawable.wp_mv_nebula,
        R.drawable.wp_mv_orchid
    )
    private val mvIcons = listOf(
        IconItem(R.drawable.ic_mv_phone, "Phone"),
        IconItem(R.drawable.ic_mv_messages, "Messages"),
        IconItem(R.drawable.ic_mv_camera, "Camera"),
        IconItem(R.drawable.ic_mv_photos, "Photos"),
        IconItem(R.drawable.ic_mv_clock, "Clock"),
        IconItem(R.drawable.ic_mv_settings, "Settings"),
        IconItem(R.drawable.ic_mv_music, "Music"),
        IconItem(R.drawable.ic_mv_browser, "Browser"),
        IconItem(R.drawable.ic_mv_maps, "Maps"),
        IconItem(R.drawable.ic_mv_mail, "Mail"),
        IconItem(R.drawable.ic_mv_calendar, "Calendar"),
        IconItem(R.drawable.ic_mv_contacts, "Contacts"),
        IconItem(R.drawable.ic_mv_calculator, "Calculator"),
        IconItem(R.drawable.ic_mv_files, "Files"),
        IconItem(R.drawable.ic_mv_video, "Video"),
        IconItem(R.drawable.ic_mv_games, "Games"),
        IconItem(R.drawable.ic_mv_recorder, "Recorder"),
        IconItem(R.drawable.ic_mv_weather, "Weather"),
        IconItem(R.drawable.ic_mv_notes, "Notes"),
        IconItem(R.drawable.ic_mv_compass, "Compass"),
        IconItem(R.drawable.ic_mv_reminders, "Reminders"),
        IconItem(R.drawable.ic_mv_health, "Health"),
        IconItem(R.drawable.ic_mv_wallet, "Wallet"),
        IconItem(R.drawable.ic_mv_favorites, "Favorites"),
        IconItem(R.drawable.ic_mv_whatsapp, "WhatsApp"),
        IconItem(R.drawable.ic_mv_whatsapp_business, "WhatsApp Business"),
        IconItem(R.drawable.ic_mv_instagram, "Instagram"),
        IconItem(R.drawable.ic_mv_tiktok, "TikTok"),
        IconItem(R.drawable.ic_mv_facebook, "Facebook"),
        IconItem(R.drawable.ic_mv_x, "X"),
        IconItem(R.drawable.ic_mv_snapchat, "Snapchat"),
        IconItem(R.drawable.ic_mv_pinterest, "Pinterest"),
        IconItem(R.drawable.ic_mv_telegram, "Telegram"),
        IconItem(R.drawable.ic_mv_google, "Google")
    )

    private val cgWallpapers = listOf(
        R.drawable.wp_cg_silk,
        R.drawable.wp_cg_roses,
        R.drawable.wp_cg_clouds,
        R.drawable.wp_cg_blossom,
        R.drawable.wp_cg_perfume,
        R.drawable.wp_cg_ballet
    )
    private val cgIcons = listOf(
        IconItem(R.drawable.ic_cg_phone, "Phone"),
        IconItem(R.drawable.ic_cg_messages, "Messages"),
        IconItem(R.drawable.ic_cg_camera, "Camera"),
        IconItem(R.drawable.ic_cg_photos, "Photos"),
        IconItem(R.drawable.ic_cg_clock, "Clock"),
        IconItem(R.drawable.ic_cg_settings, "Settings"),
        IconItem(R.drawable.ic_cg_music, "Music"),
        IconItem(R.drawable.ic_cg_browser, "Browser"),
        IconItem(R.drawable.ic_cg_maps, "Maps"),
        IconItem(R.drawable.ic_cg_mail, "Mail"),
        IconItem(R.drawable.ic_cg_calendar, "Calendar"),
        IconItem(R.drawable.ic_cg_contacts, "Contacts"),
        IconItem(R.drawable.ic_cg_calculator, "Calculator"),
        IconItem(R.drawable.ic_cg_files, "Files"),
        IconItem(R.drawable.ic_cg_video, "Video"),
        IconItem(R.drawable.ic_cg_games, "Games"),
        IconItem(R.drawable.ic_cg_recorder, "Recorder"),
        IconItem(R.drawable.ic_cg_weather, "Weather"),
        IconItem(R.drawable.ic_cg_notes, "Notes"),
        IconItem(R.drawable.ic_cg_compass, "Compass"),
        IconItem(R.drawable.ic_cg_reminders, "Reminders"),
        IconItem(R.drawable.ic_cg_health, "Health"),
        IconItem(R.drawable.ic_cg_wallet, "Wallet"),
        IconItem(R.drawable.ic_cg_favorites, "Favorites"),
        IconItem(R.drawable.ic_cg_whatsapp, "WhatsApp"),
        IconItem(R.drawable.ic_cg_whatsapp_business, "WhatsApp Business"),
        IconItem(R.drawable.ic_cg_instagram, "Instagram"),
        IconItem(R.drawable.ic_cg_tiktok, "TikTok"),
        IconItem(R.drawable.ic_cg_facebook, "Facebook"),
        IconItem(R.drawable.ic_cg_x, "X"),
        IconItem(R.drawable.ic_cg_snapchat, "Snapchat"),
        IconItem(R.drawable.ic_cg_pinterest, "Pinterest"),
        IconItem(R.drawable.ic_cg_telegram, "Telegram"),
        IconItem(R.drawable.ic_cg_google, "Google")
    )

    private val ndWallpapers = listOf(
        R.drawable.wp_nd_rain,
        R.drawable.wp_nd_skyline,
        R.drawable.wp_nd_alley,
        R.drawable.wp_nd_trails,
        R.drawable.wp_nd_rooftop,
        R.drawable.wp_nd_sign
    )
    private val ndIcons = listOf(
        IconItem(R.drawable.ic_nd_phone, "Phone"),
        IconItem(R.drawable.ic_nd_messages, "Messages"),
        IconItem(R.drawable.ic_nd_camera, "Camera"),
        IconItem(R.drawable.ic_nd_photos, "Photos"),
        IconItem(R.drawable.ic_nd_clock, "Clock"),
        IconItem(R.drawable.ic_nd_settings, "Settings"),
        IconItem(R.drawable.ic_nd_music, "Music"),
        IconItem(R.drawable.ic_nd_browser, "Browser"),
        IconItem(R.drawable.ic_nd_maps, "Maps"),
        IconItem(R.drawable.ic_nd_mail, "Mail"),
        IconItem(R.drawable.ic_nd_calendar, "Calendar"),
        IconItem(R.drawable.ic_nd_contacts, "Contacts"),
        IconItem(R.drawable.ic_nd_calculator, "Calculator"),
        IconItem(R.drawable.ic_nd_files, "Files"),
        IconItem(R.drawable.ic_nd_video, "Video"),
        IconItem(R.drawable.ic_nd_games, "Games"),
        IconItem(R.drawable.ic_nd_recorder, "Recorder"),
        IconItem(R.drawable.ic_nd_weather, "Weather"),
        IconItem(R.drawable.ic_nd_notes, "Notes"),
        IconItem(R.drawable.ic_nd_compass, "Compass"),
        IconItem(R.drawable.ic_nd_reminders, "Reminders"),
        IconItem(R.drawable.ic_nd_health, "Health"),
        IconItem(R.drawable.ic_nd_wallet, "Wallet"),
        IconItem(R.drawable.ic_nd_favorites, "Favorites"),
        IconItem(R.drawable.ic_nd_whatsapp, "WhatsApp"),
        IconItem(R.drawable.ic_nd_whatsapp_business, "WhatsApp Business"),
        IconItem(R.drawable.ic_nd_instagram, "Instagram"),
        IconItem(R.drawable.ic_nd_tiktok, "TikTok"),
        IconItem(R.drawable.ic_nd_facebook, "Facebook"),
        IconItem(R.drawable.ic_nd_x, "X"),
        IconItem(R.drawable.ic_nd_snapchat, "Snapchat"),
        IconItem(R.drawable.ic_nd_pinterest, "Pinterest"),
        IconItem(R.drawable.ic_nd_telegram, "Telegram"),
        IconItem(R.drawable.ic_nd_google, "Google")
    )

    val bundles: List<RizBundle> = listOf(
        RizBundle(
            id = "midnight_velvet",
            name = "Midnight Velvet",
            tagline = "Dark, plush & mysterious",
            accentColor = 0xFFB388FF.toInt(),
            wallpapers = mvWallpapers,
            icons = mvIcons
        ),
        RizBundle(
            id = "coquette_glow",
            name = "Coquette Glow",
            tagline = "Soft pink bows & romance",
            accentColor = 0xFFF5A8C0.toInt(),
            wallpapers = cgWallpapers,
            icons = cgIcons
        ),
        RizBundle(
            id = "neon_dusk",
            name = "Neon Dusk",
            tagline = "City lights after dark",
            accentColor = 0xFF7C6CFF.toInt(),
            wallpapers = ndWallpapers,
            icons = ndIcons
        )
    )
}
