package com.rsstudio.rizglow

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.rsstudio.rizglow.model.BundleRepository

/** 1x1 home-screen widget that looks like an app icon.
 *  Widgets never get the system shortcut "watermark" badge. */
class IconWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        // Extras from requestPinAppWidget arrive on the first update broadcast.
        // Save the mapping here so the widget configures itself even when the
        // launcher never opens the config activity (happens on some launchers).
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            val bundleId = intent.getStringExtra("bundleId")
            val label = intent.getStringExtra("label")
            if (ids != null && bundleId != null && label != null) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
                    ids.forEach { putString(key(it), "$bundleId|$label|0") }
                }.apply()
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            // Self-heal: on launchers where the pin callback/extras never arrive
            // (e.g. vivo), an unmapped widget claims the oldest pending pin.
            if (!hasMapping(context, id)) {
                val pending = claimPending(context)
                if (pending != null) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(key(id), "${pending.first}|${pending.second}|0").apply()
                }
            }
            updateWidget(context, mgr, id)
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply { ids.forEach { remove(key(it)) } }.apply()
    }

    companion object {
        const val PREFS = "rizglow_icon_widgets"
        private const val PENDING_KEY = "pending_pins"
        private const val PENDING_TTL_MS = 5 * 60 * 1000L
        fun key(id: Int) = "widget_$id"

        fun hasMapping(context: Context, appWidgetId: Int): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(key(appWidgetId), null) != null

        /** Stash a pin request before pinning; a widget that wakes up unmapped
         *  claims the oldest entry. Entries expire after 5 minutes. */
        fun addPending(context: Context, bundleId: String, label: String) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val q = prefs.getString(PENDING_KEY, "") ?: ""
            val entry = "$bundleId|$label|${System.currentTimeMillis()}"
            prefs.edit()
                .putString(PENDING_KEY, if (q.isEmpty()) entry else "$q;$entry")
                .apply()
        }

        fun claimPending(context: Context): Pair<String, String>? {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val entries = (prefs.getString(PENDING_KEY, "") ?: "")
                .split(";").filter { it.isNotEmpty() }.toMutableList()
            while (entries.isNotEmpty()) {
                val parts = entries.removeAt(0).split("|")
                prefs.edit().putString(PENDING_KEY, entries.joinToString(";")).apply()
                if (parts.size == 3) {
                    val age = now - (parts[2].toLongOrNull() ?: 0L)
                    if (age in 0..PENDING_TTL_MS) return parts[0] to parts[1]
                }
            }
            return null
        }

        /** (bundleId, label, iconResId) stored for this widget, or null. */
        fun load(context: Context, appWidgetId: Int): Triple<String, String, Int>? {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(key(appWidgetId), null) ?: return null
            val parts = raw.split("|")
            if (parts.size != 3) return null
            val bundle = BundleRepository.bundles.find { it.id == parts[0] } ?: return null
            val icon = bundle.icons.find { it.label == parts[1] } ?: return null
            return Triple(bundle.id, icon.label, icon.resId)
        }

        fun save(context: Context, appWidgetId: Int, bundleId: String, label: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(key(appWidgetId), "$bundleId|$label|0").apply()
            updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
        }

        fun updateWidget(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_icon)
            val saved = load(context, appWidgetId)
            if (saved == null) {
                // Graceful fallback: show the logo, tap opens Rizglow.
                views.setImageViewResource(R.id.widget_icon_img, R.drawable.logo)
                val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (openApp != null) {
                    val pi = PendingIntent.getActivity(
                        context, appWidgetId, openApp,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_icon_img, pi)
                }
                mgr.updateAppWidget(appWidgetId, views)
                return
            }
            val (_, label, resId) = saved
            views.setImageViewResource(R.id.widget_icon_img, resId)
            val target = IconTargets.resolve(context, label)
            if (target != null) {
                // unique data uri so each widget's PendingIntent stays distinct
                target.data = Uri.parse("rizglow://widget/$appWidgetId")
                val pi = PendingIntent.getActivity(
                    context, appWidgetId, target,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_icon_img, pi)
            }
            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
