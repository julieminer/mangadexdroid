package com.melonhead.data_user.models

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val data: User
)

@Serializable
data class User(
    val id: String,
)
