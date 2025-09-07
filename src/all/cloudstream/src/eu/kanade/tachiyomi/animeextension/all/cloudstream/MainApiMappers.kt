package eu.kanade.tachiyomi.animeextension.all.cloudstream

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode

/**
 * Mapping extensions between Cloudstream MainAPI models and Tachiyomi anime models.
 */

fun SearchResponse.toSAnime(): SAnime {
    return SAnime.create().apply {
        title = name
        url = this@toSAnime.url
        thumbnail_url = posterUrl
    }
}

fun LoadResponse.toSAnime(): SAnime {
    return SAnime.create().apply {
        title = name
        url = this@toSAnime.url
        thumbnail_url = posterUrl
        initialized = true
    }
}

fun LoadResponse.toSEpisodeList(): List<SEpisode> {
    return when (this) {
        is AnimeLoadResponse -> episodes.values.flatten().map { ep: Episode ->
            SEpisode.create().apply {
                name = ep.name ?: "Untitled"
                url = ep.data
            }
        }

        is TvSeriesLoadResponse -> episodes.map { ep: Episode ->
            SEpisode.create().apply {
                name = ep.name ?: "Untitled"
                url = ep.data
                ep.episode?.let { episode_number = it.toFloat() }
                ep.date?.let { date_upload = it }
            }
        }

        else -> emptyList()
    }
}

// fun ExtractorLink.toVideo(): Video {
//    return Video(
//        url = url,
//        quality = quality.toString(),
//        videoUrl = url,
//        headers = headers ?: emptyMap()
//    )
// }
