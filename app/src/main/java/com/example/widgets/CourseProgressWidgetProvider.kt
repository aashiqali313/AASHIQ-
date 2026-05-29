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

class CourseProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val database = AppDatabase.getDatabase(context)

            // Setup cache learning metrics
            val lastWatched = try {
                database.lessonDao().getContinueWatchingLessons().firstOrNull()?.firstOrNull()
            } catch (e: Exception) {
                null
            }

            val course = lastWatched?.let {
                try {
                    database.courseDao().getCourseById(it.courseId)
                } catch (e: Exception) {
                    null
                }
            }

            // Get all lessons for this course to see completion count
            var completedCount = 0
            var totalCount = 0
            if (lastWatched != null) {
                try {
                    val allLessons = database.lessonDao().getLessonsForCourseDirect(lastWatched.courseId)
                    totalCount = allLessons.size
                    completedCount = allLessons.count { it.isCompleted }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_course_progress)

                // Navigation pending intent
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigation_destination", "syllabus_tab")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    103,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_continue_btn, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_course_name, pendingIntent)

                if (course != null && totalCount > 0) {
                    views.setTextViewText(R.id.widget_course_name, course.title)
                    views.setTextViewText(R.id.widget_lesson_progress, "Completed $completedCount of $totalCount Lessons")
                    val percent = (completedCount * 100) / totalCount
                    views.setProgressBar(R.id.widget_progress_bar, 100, percent, false)
                } else {
                    views.setTextViewText(R.id.widget_course_name, "Deep Work Practice")
                    views.setTextViewText(R.id.widget_lesson_progress, "Select a course to track progress")
                    views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
