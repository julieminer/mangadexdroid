package com.melonhead.data_user.di

import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.data_user.services.UserService
import com.melonhead.data_user.services.UserServiceImpl
import com.melonhead.lib_networking.di.LibNetworkingModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val DataUserModule = module {
    includes(LibNetworkingModule)
    includes(LibAppDataModule)
    singleOf(::UserServiceImpl).bind<UserService>()
}
