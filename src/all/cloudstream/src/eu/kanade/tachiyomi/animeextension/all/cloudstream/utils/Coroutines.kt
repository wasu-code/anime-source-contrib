package eu.kanade.tachiyomi.animeextension.all.cloudstream.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections.synchronizedList

// expect fun runOnMainThreadNative(work: (() -> Unit))
object Coroutines {
    suspend fun <T, V> V.mainWork(work: suspend (CoroutineScope.(V) -> T)): T {
        val value = this
        return withContext(Dispatchers.Main) {
            work(value)
        }
    }

    /**
     * Safe to add and remove how you want
     * If you want to iterate over the list then you need to do:
     * synchronized(allProviders) { code here }
     **/
    fun <T> threadSafeListOf(vararg items: T): MutableList<T> {
        return synchronizedList(items.toMutableList())
    }
}
