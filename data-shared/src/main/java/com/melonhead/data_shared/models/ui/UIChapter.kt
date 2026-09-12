package com.melonhead.data_shared.models.ui

import android.os.Parcelable
import io.ktor.http.Url
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class UIChapter(
    val id: String,
    val chapter: String?,
    val title: String?,
    val createdDate: Long,
    val read: Boolean,
    val blocked: Boolean,
    val isDownloadingCache: Boolean,
    val externalUrl: String? = null,
    val cachedPages: Int? = null,
) :
    Parcelable {
    @IgnoredOnParcel
    val webAddress: String = "https://mangadex.org/chapter/$id"

    fun getExternalUrlBase(): String? {
        if (externalUrl == null) return null
        try {
            val url = Url(externalUrl)
            return url.host
        } catch (e: Exception) {
            return null
        }
    }

}
