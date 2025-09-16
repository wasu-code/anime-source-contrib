package eu.kanade.tachiyomi.animeextension.all.cloudstream

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CloudStreamSettings() : AnimeHttpSource(), ConfigurableAnimeSource {
    override val lang: String = "all"
    override val name: String = "CloudStream Settings"

    private val context = Injekt.get<Application>()
    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    @SuppressLint("ApplySharedPref")
    override fun setupPreferenceScreen(screen: PreferenceScreen) {

        // --- Repo URLs input ---
        EditTextPreference(screen.context).apply {
            key = "REPOS"
            title = "Plugin repositories"
            dialogMessage = "CloudStream repositories (one per line):"
            summary = "${preferences.getString("REPOS", "")?.lines()?.filter { it.isNotBlank() }?.size} repo(s) added"
            setDefaultValue("")
        }.also(screen::addPreference)

//        EditTextPreference(screen.context).apply {
//            summary = "Filters"
//            setEnabled(false)
//        }.also(screen::addPreference)

        val pluginsPref = MultiSelectListPreference(screen.context).apply {
            key = "EXTENSIONS"
            title = "Choose plugins to install/uninstall"
            summary = "Loading..."
            dialogTitle = "Check/uncheck plugins to install/uninstall"
            setEnabled(false)
            setOnPreferenceChangeListener { pref, newValue ->
                val selected = newValue as Set<String>
                val oldSelected = (pref as MultiSelectListPreference).values
                val removed = oldSelected - selected
                val added = selected - oldSelected

                // Disable temporarily (avoid double taps)
                pref.setEnabled(false)

                val scope = CoroutineScope(Dispatchers.IO)

                scope.launch {
                    // Install
                    added.forEach { pluginUrl ->
                        val pluginFile = PluginManager.downloadPluginToFile(pluginUrl)
                        // validate if plugin loads
                        if (pluginFile != null) {
                            PluginLoader.loadPlugin(context, pluginFile)
                        }
                    }

                    // Remove
                    removed.forEach { pluginUrl ->
                        PluginManager.deletePluginFile(pluginUrl)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Installed ${added.size}, removed ${removed.size} plugins", Toast.LENGTH_SHORT).show()
                        setEnabled(true)
                        restartApp(context)
                    }
                }
                true
            }
        }

        screen.addPreference(pluginsPref)  // add it first, we'll populate later

        // Launch coroutine to fetch plugins from repos
        CoroutineScope(Dispatchers.IO).launch {
            val repos = preferences.getString("REPOS", "")?.lines()?.filter { it.isNotBlank() } ?: emptyList()
            val plugins = repos.flatMap { repo ->
                RepositoryManager(arrayOf(repo)).getRepoPlugins(repo)
            }.distinctBy { it.url }

            withContext(Dispatchers.Main) {
                if (plugins.isEmpty()) {
                    pluginsPref.summary = "No plugins available"
                } else {
                    pluginsPref.entries = plugins.map { it.name }.toTypedArray()
                    pluginsPref.entryValues = plugins.map { it.url }.toTypedArray()

                    pluginsPref.summary = "Showing ${plugins.size} plugins"
                    pluginsPref.setEnabled(true)
                }
            }
        }

        SwitchPreferenceCompat(screen.context).apply {
            key = "PLUGINS_PURGE"
            title = "Purge all plugin files"
            setDefaultValue(false)
            setOnPreferenceClickListener { pref ->
                val switchPref = pref as SwitchPreferenceCompat
                switchPref.isChecked = false

                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    setEnabled(false)
                    val success = PluginManager.deleteAllPluginFiles()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "All plugin files deleted? $success", Toast.LENGTH_SHORT).show()
                        setEnabled(true)
                        preferences.edit()
                            .putBoolean(pref.key, false)
                            .putStringSet("EXTENSIONS", emptySet<String>())
                            .apply()
                    }
                }

                false
            }
        }.also(screen::addPreference)

    }

    fun restartApp(context: Application) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        if (componentName != null) {
            val restartIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(restartIntent)
            Runtime.getRuntime().exit(0) // kill old process after scheduling restart
        }
    }



    override val baseUrl: String = ""
    override val supportsLatest: Boolean = false
    override fun animeDetailsParse(response: Response): SAnime = throw UnsupportedOperationException()
    override fun episodeListParse(response: Response): List<SEpisode> = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun popularAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun popularAnimeRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun searchAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request  = throw UnsupportedOperationException()
}
