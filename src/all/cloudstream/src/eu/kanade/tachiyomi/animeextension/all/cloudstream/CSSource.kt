package eu.kanade.tachiyomi.animeextension.all.cloudstream

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.Request
import okhttp3.Response

class CSSource(val initialName: String) : AnimeHttpSource() {
    override val baseUrl: String
        get() = TODO("Not yet implemented")

    override fun animeDetailsParse(response: Response): SAnime {
        TODO("Not yet implemented")
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        TODO("Not yet implemented")
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        TODO("Not yet implemented")
    }

    override fun popularAnimeRequest(page: Int): Request {
        TODO("Not yet implemented")
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        TODO("Not yet implemented")
    }

    override fun searchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeFilterList,
    ): Request {
        TODO("Not yet implemented")
    }

    override val lang: String = "all"
    override val supportsLatest: Boolean = false
    override val name: String = initialName
}
