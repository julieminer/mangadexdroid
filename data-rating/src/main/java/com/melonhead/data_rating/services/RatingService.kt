package com.melonhead.data_rating.services

import com.melonhead.data_rating.models.RatingChangeRequest
import com.melonhead.data_rating.models.RatingResult
import com.melonhead.data_rating.models.RatingResults
import com.melonhead.data_rating.routes.HttpRoutes
import com.melonhead.data_rating.routes.HttpRoutes.ID_PLACEHOLDER
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_networking.extensions.catching
import com.melonhead.lib_networking.extensions.catchingSuccess
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface RatingService {
    suspend fun getRatings(mangaIds: List<String>): Map<String, Int>
    suspend fun setRating(mangaId: String, ratingNumber: Int): Boolean
}

internal class RatingServiceImpl(
    private val client: HttpClient,
    private val appData: AppData,
): RatingService {
    override suspend fun getRatings(mangaIds: List<String>): Map<String, Int> {
        val session = appData.getSession() ?: return emptyMap()
        Clog.i("getRatings: manga list: $mangaIds")
        val result = client.catching<RatingResults>("getRatings") {
            client.get(HttpRoutes.RATING_URL) {
                headers {
                    contentType(ContentType.Application.Json)
                    bearerAuth(session)
                }
                url {
                    mangaIds.forEach {
                        encodedParameters.append("manga[]", it)
                    }
                }
            }
        }
        return result?.ratings?.map { it.key to it.value.rating }?.toMap() ?: emptyMap()
    }

    override suspend fun setRating(mangaId: String, ratingNumber: Int): Boolean {
        val session = appData.getSession() ?: return false
        Clog.i("setRating: manga $mangaId rating $ratingNumber")
        return client.catchingSuccess("setRating") {
            client.post(HttpRoutes.RATING_CHANGE_URL.replace(ID_PLACEHOLDER, mangaId)) {
                headers {
                    contentType(ContentType.Application.Json)
                    bearerAuth(session)
                }
                setBody(RatingChangeRequest(ratingNumber))
            }
        }
    }
}
