package org.ushastoe.ipw

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent

class NetworkCallbackHelper(private val context: Context) : ConnectivityManager.NetworkCallback() {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    fun register() {
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(this)
    }

    private fun updateAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MainIpWidget::class.java)
        )

        if (appWidgetIds.isNotEmpty()) {
            val updateIntent = Intent(context, MainIpWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
        }
    }

    override fun onAvailable(network: Network) {
        updateAllWidgets()
    }

    override fun onLost(network: Network) {
        updateAllWidgets()
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        updateAllWidgets()
    }
}