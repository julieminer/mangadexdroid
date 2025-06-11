package com.melonhead.feature_authentication.di

import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.data_authentication.di.DataAuthenticationModule
import com.melonhead.data_user.di.DataUserModule
import com.melonhead.feature_authentication.AuthRepository
import com.melonhead.feature_authentication.AuthRepositoryImpl
import com.melonhead.feature_authentication.navigation.OauthLoginScreenResolver
import com.melonhead.lib_app_context.di.LibAppContextModule
import com.melonhead.lib_app_events.di.LibAppEventsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

val FeatureAuthenticationModule = module {
    factory { CoroutineScope(Dispatchers.IO) }

    includes(LibAppEventsModule)
    includes(LibAppContextModule)
    includes(LibAppDataModule)

    includes(DataAuthenticationModule)
    includes(DataUserModule)

    singleOf(::AuthRepositoryImpl).bind<AuthRepository>().withOptions { createdAtStart() }
    singleOf(::OauthLoginScreenResolver).bind<OauthLoginScreenResolver>().withOptions { createdAtStart() }
}
