package com.melonhead.lib_networking.ratelimit.impl

import com.melonhead.lib_networking.ratelimit.core.RateInfo
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class RateContainer(private val info: RateInfo) {
    val permits by info::permits

    val period = Duration.convert(
        info.period.toDouble(), info.unit, DurationUnit.MILLISECONDS
    ).toLong()

    val requests = LongContinuousBuffer(info.permits)

    companion object {
        fun fromAll(rates: List<RateInfo>) = rates.map(::RateContainer)
    }
}
