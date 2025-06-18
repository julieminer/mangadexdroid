package com.melonhead.lib_app_data

interface PreferenceValueMapping<U, T> {
    fun toValue(arg: U): T
    fun fromValue(arg: T): U
}