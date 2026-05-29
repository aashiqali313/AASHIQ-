package com.example.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.widgets.ContinueLearningWidgetProvider
import com.example.widgets.DisciplineWidgetProvider
import com.example.widgets.CourseProgressWidgetProvider
import com.example.widgets.MotivationWidgetProvider

object WidgetUpdater {
    
    fun updateAllWidgets(context: Context) {
        try {
            val appContext = context.applicationContext
            
            // 1. Continue Learning Widget
            val continueIntent = Intent(appContext, ContinueLearningWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val continueIds = AppWidgetManager.getInstance(appContext).getAppWidgetIds(
                ComponentName(appContext, ContinueLearningWidgetProvider::class.java)
            )
            if (continueIds.isNotEmpty()) {
                continueIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, continueIds)
                appContext.sendBroadcast(continueIntent)
            }

            // 2. Discipline Widget
            val disciplineIntent = Intent(appContext, DisciplineWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val disciplineIds = AppWidgetManager.getInstance(appContext).getAppWidgetIds(
                ComponentName(appContext, DisciplineWidgetProvider::class.java)
            )
            if (disciplineIds.isNotEmpty()) {
                disciplineIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, disciplineIds)
                appContext.sendBroadcast(disciplineIntent)
            }

            // 3. Course Progress Widget
            val progressIntent = Intent(appContext, CourseProgressWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val progressIds = AppWidgetManager.getInstance(appContext).getAppWidgetIds(
                ComponentName(appContext, CourseProgressWidgetProvider::class.java)
            )
            if (progressIds.isNotEmpty()) {
                progressIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, progressIds)
                appContext.sendBroadcast(progressIntent)
            }

            // 4. Motivation Widget
            val motivationIntent = Intent(appContext, MotivationWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val motivationIds = AppWidgetManager.getInstance(appContext).getAppWidgetIds(
                ComponentName(appContext, MotivationWidgetProvider::class.java)
            )
            if (motivationIds.isNotEmpty()) {
                motivationIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, motivationIds)
                appContext.sendBroadcast(motivationIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
