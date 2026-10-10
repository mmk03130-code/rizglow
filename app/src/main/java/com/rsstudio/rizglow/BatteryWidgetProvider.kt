package com.rsstudio.rizglow

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews

class BatteryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
            for (id in ids) update(context, mgr, id)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.rsstudio.rizglow.action.REFRESH_BATTERY"

        fun update(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_battery)
            WidgetStyle.apply(context, views, appWidgetId)
            val batt = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batt?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batt?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val pct = if (level >= 0) (level * 100 / scale) else 0
            val status = batt?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            views.setTextViewText(R.id.widget_big, "$pct%")
            views.setTextViewText(R.id.widget_sub, if (charging) "Charging" else "Tap to refresh")
            views.setProgressBar(R.id.widget_bar, 100, pct, false)
            val pi = PendingIntent.getBroadcast(
                context, appWidgetId,
                Intent(context, BatteryWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
