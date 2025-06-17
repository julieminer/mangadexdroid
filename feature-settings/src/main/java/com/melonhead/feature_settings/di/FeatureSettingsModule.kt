package com.melonhead.feature_settings.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val FeatureSettingsModule = module {
//    includes(LibAppEventsModule)
//    includes(LibAppDataContextModule)

    viewModelOf(::SettingsViewModel)

    singleOf(::SettingsActivityResolver)
}
