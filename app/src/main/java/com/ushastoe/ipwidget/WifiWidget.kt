package com.ushastoe.ipwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementation of App Widget functionality.
 */
class WifiWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = context.getString(R.string.appwidget_text)

    val intent = Intent(context, WifiWidget::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
    }

    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val longClickIntent = Intent(context, WifiWidget::class.java).apply {
        action = "com.ushastoe.ipwidget.LONG_CLICK_ACTION"
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val longClickPendingIntent = PendingIntent.getBroadcast(context, 0, longClickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val views = RemoteViews(context.packageName, R.layout.wifi_widget)

    views.setTextViewText(R.id.external, widgetText)
    views.setOnClickPendingIntent(R.id.external, pendingIntent)
    views.setOnClickPendingIntent(R.id.internal, pendingIntent)

    views.setOnClickPendingIntent(R.id.internal, longClickPendingIntent)

    views.setTextViewText(R.id.internal, "Обновляется...")
    views.setTextViewText(R.id.internal, getWifiIpAddress(context))
    appWidgetManager.updateAppWidget(appWidgetId, views)

    views.setTextViewText(R.id.external, "Обновляется...")

    CoroutineScope(Dispatchers.IO).launch {
        val externalIp = getIpAddress()

        withContext(Dispatchers.Main) {
            views.setTextViewText(R.id.external, externalIp)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun getWifiIpAddress(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo
    val ipAddress = Formatter.formatIpAddress(wifiInfo.ipAddress)
    return ipAddress
}

private suspend fun getIpAddress(): String? {
    return try {
        val url = URL("https://api.ipify.org")
        val urlConnection = withContext(Dispatchers.IO) {
            url.openConnection()
        } as HttpURLConnection
        urlConnection.requestMethod = "GET"
        urlConnection.connectTimeout = 5000
        urlConnection.readTimeout = 5000

        val inputStream = urlConnection.inputStream
        val ipAddress = inputStream.bufferedReader().use { it.readText() }
        withContext(Dispatchers.IO) {
            inputStream.close()
        }

        ipAddress
    } catch (e: Exception) {
        Log.e("IpWidget", "Error fetching external IP address", e)
        "Не удалось получить внешний IP"
    }
}
