package com.melonhead.lib_logging

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import java.io.EOFException
import java.io.IOException
import kotlin.system.measureTimeMillis

object Clog {
    suspend fun <T> measure(tag: String? = null, block: suspend () -> T): T {
        val result: T
        val time = measureTimeMillis {
            result = block()
        }
        if (time > 100) {
            Log.d(tag, "Took $time ms")
        }
        return result
    }

    fun d(message: String) {
        Log.d(null, message)
        Firebase.crashlytics.log(message)
    }

    fun i(message: String) {
        Log.i(null, message)
        Firebase.crashlytics.log(message)
    }
    
    fun w(message: String) {
        Log.w(null, message)
        Firebase.crashlytics.log(message)
    }

    fun e(message: String, exception: Throwable) {
        Log.e(null, message, exception)
        when (exception) {
            is ConnectTimeoutException, is IOException, is EOFException, is NoTransformationFoundException -> return
        }
        Firebase.crashlytics.recordException(exception)
    }
}
