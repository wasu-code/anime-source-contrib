package eu.kanade.tachiyomi.animeextension.all.cloudstream

import android.app.Application
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get



class CloudStreamFactory : AnimeSourceFactory {
    private val context = Injekt.get<Application>()

    override fun createSources(): List<AnimeSource> {
        val apis = PluginLoader.loadAllPlugins(context)
        Log.d("CloudStream", "Loaded ${apis.size}")
        return apis.map { api -> MainApiAdapter(api) } + Settings()
    }
}

class Settings() : AnimeHttpSource(), ConfigurableAnimeSource {
    override val lang: String = "all"
    override val name: String = "CloudStream Settings"

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        EditTextPreference(screen.context).apply {
            key = "REPOS"
            title = "List of repo URLs"
            setDefaultValue("")
            //            setOnPreferenceChangeListener { _, newValue ->
            //                preferences.edit().putBoolean(key, newValue as Boolean).commit()
            //            }
            summary = "Repository URLs (one per line)"
        }.also(screen::addPreference)

        //TODO input(s) for filtering list below by language/type(movie/tv/anime)

        MultiSelectListPreference(screen.context).apply {
            key = "EXTENSIONS"
            title = "Choose extensions to install"
            entries = arrayOf("A", "B", "C")
            entryValues = arrayOf("a", "b", "c")
            setDefaultValue(emptySet<String>())
        }.also(screen::addPreference)
    }

    override val baseUrl: String = ""
    override val supportsLatest: Boolean = false
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
}

