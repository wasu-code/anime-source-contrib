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
import com.lagradost.cloudstream3.TvType
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

    private fun reloadExtensions(pluginsPref: MultiSelectListPreference, newRepos: List<String>? = null) {
        val repos = newRepos ?: preferences.getString("REPOS", "")?.lines()?.filter { it.isNotBlank() } ?: emptyList()

        CoroutineScope(Dispatchers.IO).launch {
            // Load plugins from repos
            val plugins = repos.flatMap { repo ->
                RepositoryManager(arrayOf(repo)).getRepoPlugins(repo)
            }.distinctBy { it.url }

            // Filter plugins
//            val selectedStatuses = preferences.getStringSet("FILTER_STATUS2", emptySet())
//                ?.map { it.toInt() }
//                ?: emptyList()

            val selectedTvTypes = preferences.getStringSet("FILTER_TVTYPE", emptySet()) ?: emptySet()

            val filteredPlugins = plugins.filter { plugin ->
//                val matchesStatus = selectedStatuses.isEmpty() || selectedStatuses.contains(plugin.status)
                val matchesTvType = selectedTvTypes.isEmpty() || plugin.tvTypes?.any { it in selectedTvTypes } == true
                matchesTvType
            }

            // Set
            withContext(Dispatchers.Main) {
                if (filteredPlugins.isEmpty()) {
                    pluginsPref.summary = "No plugins available"
                    pluginsPref.setEnabled(false)
                } else {
                    pluginsPref.entries = filteredPlugins.map { it.name }.toTypedArray()
                    pluginsPref.entryValues = filteredPlugins.map { it.url }.toTypedArray()
                    pluginsPref.summary = "Showing ${filteredPlugins.size} plugins"
                    pluginsPref.setEnabled(true)
                }
            }
        }
    }


    @SuppressLint("ApplySharedPref")
    override fun setupPreferenceScreen(screen: PreferenceScreen) {

        val pluginsPref = MultiSelectListPreference(screen.context).apply {
            key = "EXTENSIONS"
            title = "Choose plugins to install/uninstall"
            summary = "Loading..."
            dialogTitle = "Check/uncheck plugins to install/uninstall"
            setEnabled(false)
            setOnPreferenceChangeListener { pref, newValue ->
                preferences.edit()
                    .putStringSet(pref.key, newValue as Set<String>)
                    .commit() // save now because app restarts later

                val selected = newValue as Set<String>
                val oldSelected = (pref as MultiSelectListPreference).values
                val removed = oldSelected - selected
                val added = selected - oldSelected

                // Disable temporarily
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

        EditTextPreference(screen.context).apply {
            key = "REPOS"
            title = "Plugin repositories"
            dialogMessage = "CloudStream repositories (one per line):"
            summary = "${preferences.getString("REPOS", "")?.lines()?.filter { it.isNotBlank() }?.size} repo(s) added"
            setDefaultValue("")
            setOnPreferenceChangeListener { _, newValue ->
                val newRepos = (newValue as String).lines().filter { it.isNotBlank() }
                reloadExtensions( pluginsPref, newRepos) // refresh EXTENSIONS
                summary = "${newRepos.size} repo(s) added"
                true
            }
        }.also(screen::addPreference)

        screen.addPreference(pluginsPref)  // add it first, we'll populate later

        // Initial load of plugins list
        reloadExtensions( pluginsPref)

        EditTextPreference(screen.context).apply {
            summary = "Filters"
            setEnabled(false)
        }.also(screen::addPreference)

        MultiSelectListPreference(screen.context).apply {
            key = "FILTER_TVTYPE"
            title = "Filter by type"
            entries = TvType.values().map {it.name}.toTypedArray()
            entryValues = TvType.values().map {it.name}.toTypedArray()
            setOnPreferenceChangeListener { _, newValue ->
                reloadExtensions(pluginsPref)
                true
            }
        }.also(screen::addPreference)

//        MultiSelectListPreference(screen.context).apply {
//            key = "FILTER_STATUS2"
//            entries = arrayOf("All", "Down", "Ok", "Slow", "Beta")
//            entryValues = arrayOf("-1", "0", "1", "2", "3")
//        }.also(screen::addPreference)


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
