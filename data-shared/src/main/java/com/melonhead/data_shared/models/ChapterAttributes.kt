package com.melonhead.data_shared.models

import kotlin.time.Instant

@kotlinx.serialization.Serializable
data class ChapterAttributes(val title: String?, val chapter: String?, val createdAt: Instant, val externalUrl: String?)
