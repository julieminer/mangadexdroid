package com.melonhead.data_rating.di

import com.melonhead.data_rating.services.RatingService
import com.melonhead.data_rating.services.RatingServiceImpl
import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.lib_networking.di.LibNetworkingModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val DataRatingModule = module {
    includes(LibNetworkingModule)
    includes(LibAppDataModule)
    singleOf(::RatingServiceImpl).bind<RatingService>()
}
