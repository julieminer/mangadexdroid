package com.melonhead.lib_navigation.di

import com.melonhead.lib_navigation.Navigator
import com.melonhead.lib_navigation.NavigatorImpl
import com.melonhead.lib_navigation.resolvers.ResolverMap
import com.melonhead.lib_navigation.resolvers.ResolverMapImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val LibNavigationModule = module {
    singleOf(::ResolverMapImpl).bind<ResolverMap>()
    singleOf(::NavigatorImpl).bind<Navigator>()
}
