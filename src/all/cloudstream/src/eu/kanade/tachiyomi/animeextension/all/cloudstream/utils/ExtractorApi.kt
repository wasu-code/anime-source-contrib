package eu.kanade.tachiyomi.animeextension.all.cloudstream.utils

import com.fasterxml.jackson.annotation.JsonIgnore
import eu.kanade.tachiyomi.animeextension.all.cloudstream.IDownloadableMinimum
import java.net.URI

/** Metadata about the file type used for downloads and exoplayer hint,
 * if you respond with the wrong one the file will fail to download or be played */
enum class ExtractorLinkType {
    /** Single stream of bytes no matter the actual file type */
    VIDEO,

    /** Split into several .ts files, has support for encrypted m3u8s */
    M3U8,

    /** Like m3u8 but uses xml, currently no download support */
    DASH,

    /** No support at the moment */
    TORRENT,

    /** No support at the moment */
    MAGNET,

    ;

    // See https://www.iana.org/assignments/media-types/media-types.xhtml
    fun getMimeType(): String {
        return when (this) {
            VIDEO -> "video/mp4"
            M3U8 -> "application/x-mpegURL"
            DASH -> "application/dash+xml"
            TORRENT -> "application/x-bittorrent"
            MAGNET -> "application/x-bittorrent"
        }
    }
}

private fun inferTypeFromUrl(url: String): ExtractorLinkType {
    val path = try {
        URI(url).path
    } catch (_: Throwable) {
        // don't log magnet links as errors
        null
    }
    return when {
        path?.endsWith(".m3u8") == true -> ExtractorLinkType.M3U8
        path?.endsWith(".mpd") == true -> ExtractorLinkType.DASH
        path?.endsWith(".torrent") == true -> ExtractorLinkType.TORRENT
        url.startsWith("magnet:") -> ExtractorLinkType.MAGNET
        else -> ExtractorLinkType.VIDEO
    }
}

val INFER_TYPE: ExtractorLinkType? = null

/** Class holds extracted media info to be passed to the player.
 * @property source Name of the media source, appears on player layout.
 * @property name Title of the media, appears on player layout.
 * @property url Url string of media file
 * @property referer Referer that will be used by network request.
 * @property quality Quality of the media file
 * @property headers Headers <String, String> map that will be used by network request.
 * @property extractorData Used for getExtractorVerifierJob()
 * @property type Extracted link type (Video, M3u8, Dash, Torrent or Magnet)
 * */
open class ExtractorLink constructor(
    open val source: String,
    open val name: String,
    override val url: String,
    override var referer: String,
    open var quality: Int,
    override var headers: Map<String, String> = mapOf(),
    /** Used for getExtractorVerifierJob() */
    open var extractorData: String? = null,
    open var type: ExtractorLinkType,
) : IDownloadableMinimum {
    val isM3u8: Boolean get() = type == ExtractorLinkType.M3U8
    val isDash: Boolean get() = type == ExtractorLinkType.DASH

    // Cached video size
    private var videoSize: Long? = null

    /**
     * Get video size in bytes with one head request. Only available for ExtractorLinkType.Video
     * @param timeoutSeconds timeout of the head request.
     */
    suspend fun getVideoSize(timeoutSeconds: Long = 3L): Long? {
        // Content-Length is not applicable to other types of formats
        if (this.type != ExtractorLinkType.VIDEO) return null

        videoSize = videoSize ?: runCatching {
            val response =
                app.head(this.url, headers = headers, referer = referer, timeout = timeoutSeconds)
            response.headers["Content-Length"]?.toLong()
        }.getOrNull()

        return videoSize
    }

    @JsonIgnore
    fun getAllHeaders(): Map<String, String> {
        if (referer.isBlank()) {
            return headers
        } else if (headers.keys.none { it.equals("referer", ignoreCase = true) }) {
            return headers + mapOf("referer" to referer)
        }
        return headers
    }

    @Deprecated("Use newExtractorLink", level = DeprecationLevel.ERROR)
    constructor(
        source: String,
        name: String,
        url: String,
        referer: String? = null,
        quality: Int? = null,
        /** the type of the media, use INFER_TYPE if you want to auto infer the type from the url */
        type: ExtractorLinkType? = INFER_TYPE,
        headers: Map<String, String> = mapOf(),
        /** Used for getExtractorVerifierJob() */
        extractorData: String? = null,
    ) : this(
        source = source,
        name = name,
        url = url,
        referer = referer ?: "",
        quality = quality ?: Qualities.Unknown.value,
        headers = headers,
        extractorData = extractorData,
        type = type ?: inferTypeFromUrl(url),
    )

    @Deprecated("Use newExtractorLink", level = DeprecationLevel.ERROR)
    constructor(
        source: String,
        name: String,
        url: String,
        referer: String,
        quality: Int,
        /** the type of the media, use INFER_TYPE if you want to auto infer the type from the url */
        type: ExtractorLinkType?,
        headers: Map<String, String> = mapOf(),
        /** Used for getExtractorVerifierJob() */
        extractorData: String? = null,
    ) : this(
        source = source,
        name = name,
        url = url,
        referer = referer,
        quality = quality,
        headers = headers,
        extractorData = extractorData,
        type = type ?: inferTypeFromUrl(url),
    )

    /**
     * Old constructor without isDash, allows for backwards compatibility with extensions.
     * Should be removed after all extensions have updated their cloudstream.jar
     **/
    @Suppress("DEPRECATION_ERROR")
    @Deprecated("Use newExtractorLink", level = DeprecationLevel.ERROR)
    constructor(
        source: String,
        name: String,
        url: String,
        referer: String,
        quality: Int,
        isM3u8: Boolean = false,
        headers: Map<String, String> = mapOf(),
        /** Used for getExtractorVerifierJob() */
        extractorData: String? = null,
    ) : this(source, name, url, referer, quality, isM3u8, headers, extractorData, false)

    @Deprecated("Use newExtractorLink", level = DeprecationLevel.ERROR)
    constructor(
        source: String,
        name: String,
        url: String,
        referer: String,
        quality: Int,
        isM3u8: Boolean = false,
        headers: Map<String, String> = mapOf(),
        /** Used for getExtractorVerifierJob() */
        extractorData: String? = null,
        isDash: Boolean,
    ) : this(
        source = source,
        name = name,
        url = url,
        referer = referer,
        quality = quality,
        headers = headers,
        extractorData = extractorData,
        type = if (isDash) ExtractorLinkType.DASH else if (isM3u8) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
    )

    override fun toString(): String {
        return "ExtractorLink(name=$name, url=$url, referer=$referer, type=$type)"
    }
}

enum class Qualities(var value: Int, val defaultPriority: Int) {
    Unknown(400, 4),
    P2160(2160, 8), // 4k or 2160p
    ;

    companion object {
        fun getStringByInt(qual: Int?): String {
            return when (qual) {
                0 -> "Auto"
                Unknown.value -> ""
                P2160.value -> "4K"
                null -> ""
                else -> "${qual}p"
            }
        }

        fun getStringByIntFull(quality: Int): String {
            return when (quality) {
                0 -> "Auto"
                Unknown.value -> "Unknown"
                P2160.value -> "4K"
                else -> "${quality}p"
            }
        }
    }
}
