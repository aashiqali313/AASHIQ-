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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContinueLearningWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val database = AppDatabase.getDatabase(context)
            
            // Fetch cached learning state safely
            val activeLesson = try {
                database.lessonDao().getContinueWatchingLessons().firstOrNull()?.firstOrNull()
            } catch (e: Exception) {
                null
            }

            val activeCourse = activeLesson?.let {
                try {
                    database.courseDao().getCourseById(it.courseId)
                } catch (e: Exception) {
                    null
                }
            }

            val profile = try {
                database.userProfileDao().getProfileDirect()
            } catch (e: Exception) {
                null
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_continue_learning)

                // Setup Pending Intent to open MainActivity
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigation_destination", "syllabus_tab")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_resume_btn, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_course_title, pendingIntent)

                // Populate views dynamically with elite styling
                if (activeLesson != null && activeCourse != null) {
                    views.setTextViewText(R.id.widget_course_title, activeCourse.title)
                    views.setTextViewText(R.id.widget_progress_text, "Lesson: ${activeLesson.title}")
                    
                    val progressPercent = activeLesson.playProgressPercent
                    views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)
                } else {
                    views.setTextViewText(R.id.widget_course_title, "No active course in progress")
                    views.setTextViewText(R.id.widget_progress_text, "Ready to start learning?")
                    views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                }

                val streakDays = profile?.currentStreak ?: 5
                views.setTextViewText(R.id.widget_streak, "🔥 WEEKLY STREAK: $streakDays DAYS")

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
