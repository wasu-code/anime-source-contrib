package eu.kanade.tachiyomi.animeextension.all.cloudstream.mvvm

import android.util.Log

sealed class Resource<out T> {
    data class Success<out T>(val value: T) : Resource<T>()
    data class Failure(
        val isNetworkError: Boolean,
        val errorString: String,
    ) : Resource<Nothing>()

    data class Loading(val url: String? = null) : Resource<Nothing>()

    companion object {
        fun <T> fromResult(result: Result<T>): Resource<T> {
            val value = result.getOrNull()
            return (
                if (value != null) {
                    Success(value)
                } else {
                    Exception("this should not be possible")
                }
                ) as Resource<T>
        }
    }
}

fun logError(throwable: Throwable) {
    Log.d("ApiError", "-------------------------------------------------------------------")
    Log.d("ApiError", "safeApiCall: " + throwable.localizedMessage)
    Log.d("ApiError", "safeApiCall: " + throwable.message)
    throwable.printStackTrace()
    Log.d("ApiError", "-------------------------------------------------------------------")
}

/** Catches any exception (or error) and only logs it.
 * Will return null on exceptions. */
fun <T> safe(apiCall: () -> T): T? {
    return try {
        apiCall.invoke()
    } catch (throwable: Throwable) {
        logError(throwable)
        return null
    }
}
