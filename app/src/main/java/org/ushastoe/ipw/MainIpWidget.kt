package org.ushastoe.ipw

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

class MainIpWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val ACTION_UPDATE = "com.ushastoe.ipwidget.ACTION_UPDATE"

    companion object {
        private var networkCallbackHelper: NetworkCallbackHelper? = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Регистрируем BroadcastReceiver при создании первого виджета
        networkCallbackHelper = NetworkCallbackHelper(context).apply {
            register()
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        handler.removeCallbacksAndMessages(null)
        networkCallbackHelper?.unregister()
        networkCallbackHelper = null
    }


    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.main_ip_widget)

        val updateIntent = Intent(context, MainIpWidget::class.java).apply {
            action = ACTION_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("customscheme://widget/$appWidgetId")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

        // Проверяем состояние сети перед обновлением
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities == null) {
            views.setTextViewText(R.id.localIpWidget, "No network")
            views.setTextViewText(R.id.externalIpWidget, "No network")
            views.setViewVisibility(R.id.localIpContainer, View.GONE)
            views.setViewVisibility(R.id.ipDivider, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        // Инициализация всех полей
        views.setTextViewText(R.id.localIpWidget, "...")
        views.setTextViewText(R.id.externalIpWidget, "...")

        // Сначала скрываем дополнительные элементы
        views.setViewVisibility(R.id.ipDivider, View.GONE)
        views.setViewVisibility(R.id.localIpContainer, View.GONE)

        appWidgetManager.updateAppWidget(appWidgetId, views)

        scope.launch {
            val hotspotIp = getHotspotIpAddress()
            val localIp = if (hotspotIp != null) {
                hotspotIp
            } else {
                getLocalIpAddress(context)
            } ?: "N/A"

            val externalIp = if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                try {
                    URL("https://api.ipify.org").readText()
                } catch (e: Exception) {
                    "N/A"
                }
            } else {
                "No internet"
            }

            handler.post {
                // Обновляем тексты
                views.setTextViewText(R.id.localIpWidget, localIp)
                views.setTextViewText(R.id.externalIpWidget, externalIp)

                // Управляем видимостью элементов
                if (localIp != "N/A") {
                    views.setViewVisibility(R.id.localIpContainer, View.VISIBLE)
                    views.setViewVisibility(R.id.ipDivider, View.VISIBLE)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun getLocalIpAddress(context: Context): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    if (networkInterface.name != "wlan0") { // Пропускаем wlan0, так как он проверяется отдельно
                        val addresses = networkInterface.inetAddresses
                        while (addresses.hasMoreElements()) {
                            val address = addresses.nextElement()
                            if (!address.isLoopbackAddress && address is Inet4Address) {
                                return address.hostAddress
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getHotspotIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (networkInterface.name == "wlan0") { // Ищем именно интерфейс точки доступа
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}