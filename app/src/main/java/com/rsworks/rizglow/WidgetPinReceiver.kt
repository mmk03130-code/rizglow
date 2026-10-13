package com.rsworks.rizglow

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by the system after a widget pin succeeds. Carries our own extras
 *  (bundleId + label) plus EXTRA_APPWIDGET_ID, so the widget configures itself
 *  without relying on the launcher to open any config screen. */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val bundleId = intent.getStringExtra("bundleId")
        val label = intent.getStringExtra("label")
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID && bundleId != null && label != null) {
            IconWidgetProvider.save(context, id, bundleId, label)
        }
    }
}
