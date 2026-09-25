package com.example.salarycalculator.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object ShiftHeatmapWidgetManager {

    /**
     * Broadcasts an immediate update to all active Shift Heatmap Home Screen Widgets.
     */
    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, ShiftHeatmapWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (widgetIds != null && widgetIds.isNotEmpty()) {
                for (widgetId in widgetIds) {
                    ShiftHeatmapWidgetProvider.updateWidget(context, appWidgetManager, widgetId)
                }
            }
        } catch (_: Exception) {}
    }
}
