package com.melonhead.mangadexfollower.models.auth

@kotlinx.serialization.Serializable
data class AuthResponse(val result: String, val token: AuthToken)

