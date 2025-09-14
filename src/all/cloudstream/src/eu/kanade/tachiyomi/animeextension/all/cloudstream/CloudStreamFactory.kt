package eu.kanade.tachiyomi.animeextension.all.cloudstream

import android.app.Application
import android.util.Log
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CloudStreamFactory : AnimeSourceFactory { // ConfigurableAnimeSource
    private val context = Injekt.get<Application>()
//    private val preferences: SharedPreferences by lazy {
//        Injekt.get<Application>().getSharedPreferences("source_cloudstream", 0x0000)
//    }

    override fun createSources(): List<MainApiAdapter> {
        val apis = PluginLoader.loadAllPlugins(context)
        Log.d("CloudStream", "Loaded ${apis.size}")
        return apis.map { api -> MainApiAdapter(api) }
    }
}


//    override fun setupPreferenceScreen(screen: PreferenceScreen) {
//        SwitchPreferenceCompat(screen.context).apply {
//            key = "TST_KEY"
//            title = "Test title"
//            setDefaultValue(true)
// //            setOnPreferenceChangeListener { _, newValue ->
// //                preferences.edit().putBoolean(key, newValue as Boolean).commit()
// //            }
//            summary = "Test desc"
//        }.also(screen::addPreference)
//    }
//
//    fun getId(): Long {
//        return javaClass.name.hashCode().toLong()
//    }
//    fun getLang() = "all"
//    fun getName() = "CloudStream Settings"
