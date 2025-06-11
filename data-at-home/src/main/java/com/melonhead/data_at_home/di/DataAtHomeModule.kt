package com.melonhead.data_at_home.di

import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.data_at_home.AtHomeService
import com.melonhead.data_at_home.AtHomeServiceImpl
import com.melonhead.lib_networking.di.LibNetworkingModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val DataAtHomeModule = module {
    includes(LibNetworkingModule)
    includes(LibAppDataModule)
    singleOf(::AtHomeServiceImpl).bind<AtHomeService>()
}
