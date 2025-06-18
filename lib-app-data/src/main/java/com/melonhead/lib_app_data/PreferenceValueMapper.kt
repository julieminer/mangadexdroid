package com.melonhead.lib_app_data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PreferenceValueMapper<U, T>(
    private val preferenceValue: PreferenceValue<T>,
    private val mapping: PreferenceValueMapping<U, T>,
) {
    val flow: Flow<U> = preferenceValue.flow.map { value ->
        if (value == null) return@map mapping.fromValue(preferenceValue.defaultValue)
        mapping.fromValue(value)
    }.distinctUntilChanged()

    suspend fun setValue(value: U) {
        preferenceValue.setValue(mapping.toValue(value))
    }

    suspend fun getValue(): U {
        return flow.firstOrNull() ?: mapping.fromValue(preferenceValue.defaultValue)
    }

    @Composable
    fun collectAsState(): State<U> {
        val initValue = mapping.fromValue(preferenceValue.defaultValue)
        return flow.collectAsState(initial = initValue)
    }
}