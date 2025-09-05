package eu.kanade.tachiyomi.animeextension.all.cloudstream

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.Response

/**
 * Adapter: Wraps a Cloudstream MainAPI provider so it can be used
 * as a Tachiyomi AnimeHttpSource at runtime.
 */
class MainApiAnimeHttpSourceAdapter(
    private val api: MainAPI,
) : AnimeHttpSource() {

    override val name: String = api.name
    override val baseUrl: String = api.mainUrl
    override val lang: String = api.lang
    override val supportsLatest: Boolean = false

    // === Popular Anime ===

    override suspend fun getPopularAnime(page: Int): AnimesPage {
        val items = runBlocking {
            api.getMainPage(page, MainPageRequest("popular", baseUrl, false))
                ?.items?.flatMap { it.list }?.map { it.toSAnime() } ?: emptyList()
        }
        return AnimesPage(items, hasNextPage = false)
    }

    override fun popularAnimeRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun popularAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()

    // === Search ===
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$baseUrl/search?q=$query&page=$page", headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        val query = response.request.url.queryParameter("q") ?: ""
        val items = runBlocking {
            api.search(query, page = 1)?.items?.map { it.toSAnime() }
                ?: api.search(query)?.map { it.toSAnime() }
                ?: emptyList()
        }
        return AnimesPage(items, hasNextPage = false)
    }

    // === Latest Updates ===
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): AnimesPage = throw UnsupportedOperationException()

    // === Anime Details ===
    override fun animeDetailsParse(response: Response): SAnime {
        val url = response.request.url.toString()
        val details = runBlocking { api.load(url) }
        return details?.toSAnime() ?: SAnime.create()
    }

    // === Episode List ===
    override fun episodeListParse(response: Response): List<SEpisode> {
        val url = response.request.url.toString()
        val loadResponse = runBlocking { api.load(url) }
        return loadResponse?.toSEpisodeList() ?: emptyList()
    }

    // === Video Streams ===
//    override fun videoListParse(response: Response): List<Video> {
//        val url = response.request.url.toString()
//        val videos = mutableListOf<Video>()
//        runBlocking {
//            api.loadLinks(
//                url,
//                isCasting = false,
//                subtitleCallback = { /* ignore for now */ },
//                callback = { extractorLink ->
//                    videos.add(extractorLink.toVideo())
//                }
//            )
//        }
//        return videos
//    }
}
