package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.util.Random

class MotivationWidgetProvider : AppWidgetProvider() {

    private val quotes = listOf(
        Pair("We suffer more often in imagination than in reality.", "Seneca"),
        Pair("No man is free who is not master of himself.", "Epictetus"),
        Pair("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        Pair("He who has a why to live for can bear almost any how.", "Nietzsche"),
        Pair("The first and greatest victory is to conquer yourself.", "Plato"),
        Pair("Associate with people who are likely to improve you.", "Seneca"),
        Pair("Difficulty is what wakes up the genius.", "Nassim Taleb"),
        Pair("Conquer yourself rather than the world.", "Descartes"),
        Pair("Amor Fati — Love your fate, which is in fact your life.", "Nietzsche"),
        Pair("Waste no more time arguing about what a good man should be. Be one.", "Marcus Aurelius")
    )

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val random = Random()
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_motivation)

            // Select a beautiful quote randomly on every refresh
            val randomIndex = random.nextInt(quotes.size)
            val selectedQuote = quotes[randomIndex]

            views.setTextViewText(R.id.widget_quote_text, selectedQuote.first)
            views.setTextViewText(R.id.widget_quote_author, "— ${selectedQuote.second}")

            // Click triggers launching MainActivity to feed focus mode
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                104,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quote_text, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
