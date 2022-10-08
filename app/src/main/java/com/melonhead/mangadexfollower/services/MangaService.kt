package com.melonhead.mangadexfollower.services

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.melonhead.mangadexfollower.models.auth.AuthToken
import com.melonhead.mangadexfollower.models.content.Manga
import com.melonhead.mangadexfollower.models.content.MangaReadMarkersResponse
import com.melonhead.mangadexfollower.models.content.MangaResponse
import com.melonhead.mangadexfollower.routes.HttpRoutes.MANGA_READ_MARKERS_URL
import com.melonhead.mangadexfollower.routes.HttpRoutes.MANGA_URL
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay


interface MangaService {
    suspend fun getManga(token: AuthToken, mangaIds: List<String>): List<Manga>
    suspend fun getReadChapters(mangaIds: List<String>, token: AuthToken): List<String>
}

class MangaServiceImpl(
    private val client: HttpClient,
): MangaService {
    override suspend fun getManga(token: AuthToken, mangaIds: List<String>): List<Manga> {
        Log.i("", "getManga: ${mangaIds.count()} $mangaIds")
        val result = client.get(MANGA_URL) {
            headers {
                contentType(ContentType.Application.Json)
                bearerAuth(token.session)
            }
            url {
                mangaIds.forEach { encodedParameters.append("ids[]", it) }
            }
        }

        return try {
            result.body<MangaResponse>().data
        } catch (e: Exception) {
            Log.i("", "getManga: ${result.bodyAsText()}")
            Firebase.crashlytics.recordException(e)
            emptyList()
        }
    }

    override suspend fun getReadChapters(mangaIds: List<String>, token: AuthToken): List<String> {
        Log.i("", "getReadChapters: total $mangaIds")
        val allChapters = mutableListOf<String>()
        mangaIds.chunked(100).map { list ->
            Log.i("", "getReadChapters: chunked ${list.count()}, ${list}")
            val result = client.get(MANGA_READ_MARKERS_URL) {
                headers {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token.session)
                }
                url {
                    list.forEach {
                        encodedParameters.append("ids[]", it)
                    }
                }
            }

            val chapters = try {
                result.body<MangaReadMarkersResponse>().data
            } catch (e: Exception) {
                Log.i("", "getReadChapters: ${result.bodyAsText()}")
                Firebase.crashlytics.recordException(e)
                emptyList()
            }
            allChapters.addAll(chapters)
            // prevents triggering the anti-spam
            delay(1000)
        }
        return allChapters
    }
}