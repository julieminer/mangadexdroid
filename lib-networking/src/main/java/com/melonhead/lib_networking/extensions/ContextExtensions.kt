package com.melonhead.lib_networking.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

fun Context.networkAvailability(): Flow<Boolean> {
    return flow {
        emit(isNetworkAvailable())
        while (true) {
            delay(500L)
            emit(isNetworkAvailable())
        }
    }.distinctUntilChanged()
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // For 29 api or above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
            }
        }
    }
    // For below 29 api
    else {
        @Suppress("DEPRECATION") // Suppress deprecation for older API levels
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
            return true
        }
    }
    return false
}