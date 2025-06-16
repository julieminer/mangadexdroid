package com.melonhead.data_rating.models

import kotlinx.serialization.Serializable

@Serializable
internal data class RatingResults(
    val ratings: Map<String, RatingResult>
)

@Serializable
internal data class RatingResult(
    val rating: Int
)
