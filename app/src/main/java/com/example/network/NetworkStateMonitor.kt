package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network availability in real time to power intelligent hybrid switching
 * between Cloud (Gemini Live) and Local On-Device Offline mode.
 */
class NetworkStateMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentNetwork())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow(getNetworkTypeDescription())
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network became available")
            _isOnline.value = true
            _networkType.value = getNetworkTypeDescription()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network connection lost")
            _isOnline.value = checkCurrentNetwork()
            _networkType.value = getNetworkTypeDescription()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
            _networkType.value = getNetworkTypeDescription()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun checkCurrentNetwork(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getNetworkTypeDescription(): String {
        val cm = connectivityManager ?: return "Offline"
        val activeNetwork = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 5G/4G"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) "Connected" else "Offline"
        }
    }

    companion object {
        private const val TAG = "NetworkStateMonitor"
    }
}
