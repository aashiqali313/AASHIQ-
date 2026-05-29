package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DisciplineWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val database = AppDatabase.getDatabase(context)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val habits = try {
                database.habitDao().getAllHabitsDirect()
            } catch (e: Exception) {
                emptyList()
            }

            val todayLogs = try {
                database.habitLogDao().getLogsForDateDirect(todayStr)
            } catch (e: Exception) {
                emptyList()
            }

            val totalHabits = habits.size
            val completedHabits = todayLogs.count { it.isCompleted }
            val completionPercent = if (totalHabits > 0) (completedHabits * 100) / totalHabits else 0
            val todayXPGained = todayLogs.sumOf { it.earnedXP }

            val profile = try {
                database.userProfileDao().getProfileDirect()
            } catch (e: Exception) {
                null
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_discipline)

                // Pending Intent to open Habits tracker
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigation_destination", "discipline_tab")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_action_btn, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_completion, pendingIntent)

                // Set information
                if (totalHabits > 0) {
                    views.setTextViewText(R.id.widget_completion, "Daily Habits: $completedHabits / $totalHabits Done ($completionPercent%)")
                    views.setProgressBar(R.id.widget_progress_bar, 100, completionPercent, false)
                } else {
                    views.setTextViewText(R.id.widget_completion, "No active habits configured today")
                    views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                }

                val streakDays = profile?.currentStreak ?: 5
                views.setTextViewText(R.id.widget_streak_lbl, "🔥 Streak: $streakDays Days")
                views.setTextViewText(R.id.widget_xp_ring, "+${todayXPGained} XP gained")

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
