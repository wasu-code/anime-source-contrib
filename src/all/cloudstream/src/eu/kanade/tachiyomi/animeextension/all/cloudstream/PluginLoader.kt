package eu.kanade.tachiyomi.animeextension.all.cloudstream

import android.app.Application
import android.util.Log
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import dalvik.system.PathClassLoader
import java.io.File
import java.io.InputStreamReader
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

object PluginLoader {
    private const val PLUGIN_FOLDER = "cloudstream"

    private fun copyIfNeeded(src: File, dest: File) {
        if (!dest.exists() ||
            src.length() != dest.length() ||
            src.lastModified() != dest.lastModified()
        ) {
            src.copyTo(dest, overwrite = true)
            dest.setLastModified(src.lastModified())
        }
        // Make sure file is read-only for Android 14+
        dest.setReadOnly()
    }

    private fun loadPlugin(context: Application, file: File) {
        try {
            val loader = PathClassLoader(file.absolutePath, this::class.java.classLoader)
            loader.getResourceAsStream("manifest.json").use { stream ->
                if (stream == null) return
                InputStreamReader(stream).use { reader ->
                    val manifest = parseJson(reader, BasePlugin.Manifest::class.java)
                    val pluginClass = loader.loadClass(manifest.pluginClassName)
                    val pluginInstance = pluginClass.getDeclaredConstructor().newInstance() as BasePlugin

                    pluginInstance.load()
                }
            }
        } catch (e: Throwable) {
            // Skip invalid plugins
            Log.d("CloudStream", "Failed to load $file")
            e.printStackTrace()
        }
    }

    fun loadAllPlugins(context: Application): List<MainAPI> {
        val externalDir = File(context.getExternalFilesDir(null), PLUGIN_FOLDER)
        val internalDir = File(context.filesDir, PLUGIN_FOLDER)
        if (!internalDir.exists()) internalDir.mkdirs()

        val pluginFiles = externalDir.listFiles { f -> f.extension == "cs3" } ?: emptyArray()

        Log.d("CloudStream", "Found ${pluginFiles.size} plugins")

        pluginFiles.forEach { src ->
            val dest = File(internalDir, src.name)
            copyIfNeeded(src, dest)
            loadPlugin(context, dest)
        }

        // Plugins register themselves in APIHolder during load()
        return APIHolder.allProviders.toList()
    }
}
