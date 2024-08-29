package com.ushastoe.widgetip

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


class WidgetIpFull : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        Log.d("widget", "onEnabled")
    }

    override fun onDisabled(context: Context) {
        Log.d("widget", "onDisabled")
    }
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateFullAppWidget(context, appWidgetManager, appWidgetId)
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        println("width - $width  height - $height")
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("wifi", "onUpdate()")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}


@SuppressLint("RemoteViewLayout")
internal fun updateFullAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = context.getString(R.string.appwidget_text)

    val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
    }

    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val views = RemoteViews(context.packageName, R.layout.widgetip)


    views.setTextViewText(R.id.external, widgetText)
    views.setOnClickPendingIntent(R.id.external, pendingIntent)
    views.setOnClickPendingIntent(R.id.internal, pendingIntent)

    views.setTextViewText(R.id.internal, "Обновляется...")
    val ip = getWifiIpAddress(context)
    if (ip == "0.0.0.0") {
        views.setViewVisibility(R.id.internal, View.GONE)
        views.setViewVisibility(R.id.separator, View.GONE)
    } else {
        views.setViewVisibility(R.id.internal, View.VISIBLE)
        views.setViewVisibility(R.id.separator, View.VISIBLE)
        views.setTextViewText(R.id.internal, ip)
    }
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
