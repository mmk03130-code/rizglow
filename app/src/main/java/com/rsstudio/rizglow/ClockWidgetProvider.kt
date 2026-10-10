package com.rsstudio.rizglow

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class ClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(context, mgr, id)
    }

    companion object {
        fun update(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            WidgetStyle.apply(context, views, appWidgetId)
            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
