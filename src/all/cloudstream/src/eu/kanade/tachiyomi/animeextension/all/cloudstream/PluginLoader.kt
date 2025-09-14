package eu.kanade.tachiyomi.animeextension.all.cloudstream

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin
import dalvik.system.PathClassLoader
import java.io.File
import java.io.InputStreamReader
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

object PluginLoader {
    private const val PLUGIN_FOLDER = "cloudstream"
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private fun copyIfNeeded(src: File, dest: File) {
        if (!dest.exists() ||
            src.length() != dest.length() ||
            src.lastModified() != dest.lastModified()
        ) {
            // Make it writable if it exists
            if (dest.exists() && !dest.canWrite()) {
                dest.setWritable(true)
            }

            src.copyTo(dest, overwrite = true)
            dest.setLastModified(src.lastModified())
        }
        // Make sure file is read-only (for Android 14+ to read dex files)
        dest.setReadOnly()
    }

    private fun loadPlugin(context: Application, file: File) {
        try {
            // Pass extension's classloader as parent (not Aniyomi's) so Cloudstream core classes
            // (like BasePlugin, MainAPI) that are bundled in this library are available
            val loader = PathClassLoader(file.absolutePath, this::class.java.classLoader)
            loader.getResourceAsStream("manifest.json").use { stream ->
                if (stream == null) return
                InputStreamReader(stream).use { reader ->
                    val manifest = parseJson(reader, BasePlugin.Manifest::class.java)
                    val pluginClass = loader.loadClass(manifest.pluginClassName)
                    val pluginInstance = pluginClass.getDeclaredConstructor().newInstance() as BasePlugin

                    if (pluginInstance is Plugin) {
                        Log.d("CloudStream","Plugin skipped (Plugin class not yet supported) $file")
                        handler.post {
                            Toast.makeText(context, "Plugin ${manifest.name} not yet supported", Toast.LENGTH_SHORT).show()
                        }
                        //pluginInstance.load(context) // Not sure what to pass here and how it is used
                    } else {
                        pluginInstance.load()
                    }
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
            loadPlugin(context,dest)
        }

        // Plugins register themselves in APIHolder during load()
        return APIHolder.allProviders.toList()
    }
}
