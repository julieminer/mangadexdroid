package com.melonhead.lib_app_data.di

import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_data.AppDataImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

val LibAppDataModule = module {
    factory { CoroutineScope(Dispatchers.IO) }
    singleOf(::AppDataImpl).bind<AppData>().withOptions { createdAtStart() }
}
