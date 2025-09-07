package eu.kanade.tachiyomi.animeextension.all.cloudstream

import android.app.Application
import com.lagradost.cloudstream3.MainAPI
import dalvik.system.DexClassLoader
import dalvik.system.DexFile
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.zip.ZipFile

class CloudStreamFactory : AnimeSourceFactory { // ConfigurableAnimeSource
    private val context = Injekt.get<Application>()
//    private val preferences: SharedPreferences by lazy {
//        Injekt.get<Application>().getSharedPreferences("source_cloudstream", 0x0000)
//    }

    override fun createSources(): List<AnimeSource> {
        val csDir = File(context.getExternalFilesDir(null), "cloudstream")
        if (!csDir.exists()) csDir.mkdirs()

//        val file = File(csDir, "config.json")
//        if (!file.exists()) {
//            file.writeText("{\"plugins\":[\"ala\", \"bela\"]}")
//        }
//
//        val pluginsJson = file.readText()
//        val jsonObject = JSONObject(pluginsJson)
//        val pluginsArray = jsonObject.getJSONArray("plugins")
//
//        val sources = mutableListOf<AnimeSource>()
//        for (i in 0 until pluginsArray.length()) {
//            val pluginName = pluginsArray.getString(i)
//            sources.add(CSSource(pluginName))
//        }

//        val cs3File = File(csDir, "DailyMotion.cs3")
//        val dexFile = extractDexFromCs3(cs3File, context.codeCacheDir)
//
//        val apis = loadMainApisFromDex(dexFile, context)
//
//        val sources = apis.map { api -> MainApiAnimeHttpSourceAdapter(api) }
//
//        return sources

        val myMainApi: MainAPI = DailymotionProvider()
        val httpSource: AnimeHttpSource = MainApiAnimeHttpSourceAdapter(myMainApi)
        return listOf(httpSource)
    }

    fun extractDexFromCs3(cs3File: File, outputDir: File): File {
        val zip = ZipFile(cs3File)
        val dexEntry = zip.getEntry("classes.dex")
            ?: throw IllegalArgumentException("No classes.dex found in ${cs3File.name}")

        val dexFile = File(outputDir, "${cs3File.nameWithoutExtension}.dex")
        zip.getInputStream(dexEntry).use { input ->
            dexFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return dexFile
    }

    fun loadMainApisFromDex(dexFile: File, context: Application): List<MainAPI> {
        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            context.codeCacheDir.absolutePath,
            null,
            context.classLoader,
        )

        val apis = mutableListOf<MainAPI>()

        // Enumerate all classes in dex
        val dexFileObj = DexFile(dexFile)
        val entries = dexFileObj.entries()
        while (entries.hasMoreElements()) {
            val className = entries.nextElement()
            val clazz = try {
                classLoader.loadClass(className)
            } catch (e: Throwable) {
                null
            }
            if (clazz != null && MainAPI::class.java.isAssignableFrom(clazz)) {
                val instance = clazz.newInstance() as MainAPI
                apis.add(instance)
            }
        }
        return apis
    }
}
//    val myMainApi: MainAPI = SomeProvider()  // your existing extension
//    val httpSource: HttpSource = MainApiHttpSourceAdapter(myMainApi)

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

// define list of installed plugins
// create source for every installed plugin
// to every source add common settings for managing plugins

// when repo is addded from url save json in csDir
// installed.json with name and internalName ?
