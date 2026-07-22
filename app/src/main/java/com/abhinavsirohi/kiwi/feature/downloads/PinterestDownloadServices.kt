package com.abhinavsirohi.kiwi.feature.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PinterestMedia(
    val sourceUrl: String,
    val mediaUrl: String,
    val previewUrl: String?,
    val title: String,
    val attribution: String,
    val mimeType: String,
)

sealed interface PinterestExtractionResult {
    data class Success(val media: PinterestMedia) : PinterestExtractionResult
    data class Failure(val message: String) : PinterestExtractionResult
}

fun interface PinterestExtractor {
    suspend fun extract(url: String): PinterestExtractionResult
}

fun interface PinterestDownloadGateway {
    fun enqueue(media: PinterestMedia): Long
}

object PinterestUrlValidator {
    fun normalize(value: String): String? {
        val candidate = value.trim().trimEnd('.', ',', ';', ')', ']', '}')
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.userInfo != null) return null
        val host = uri.host?.lowercase() ?: return null
        if (!isPinterestHost(host)) return null
        return uri.normalize().toASCIIString()
    }

    fun firstPinterestUrl(value: String): String? = URL_PATTERN
        .findAll(value)
        .mapNotNull { normalize(it.value) }
        .firstOrNull()

    fun isPinterestHost(host: String): Boolean =
        host == "pinterest.com" || host.endsWith(".pinterest.com") || host == "pin.it"

    fun isPinterestMediaHost(host: String): Boolean = host == "pinimg.com" || host.endsWith(".pinimg.com")

    private val URL_PATTERN = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
}

object PinterestMetadataParser {
    fun parse(sourceUrl: String, html: String): PinterestExtractionResult {
        val metadata = META_TAG.findAll(html).mapNotNull { match ->
            val attributes = ATTRIBUTE.findAll(match.value).associate { attribute ->
                attribute.groupValues[1].lowercase() to decodeHtml(attribute.groupValues[3])
            }
            val key = attributes["property"] ?: attributes["name"] ?: attributes["itemprop"]
            val content = attributes["content"]
            if (key.isNullOrBlank() || content.isNullOrBlank()) null else key.lowercase() to content
        }.toMap()

        val metadataVideos = listOf("og:video:secure_url", "og:video", "twitter:player:stream")
            .mapNotNull(metadata::get)
        val mediaUrl = (metadataVideos + embeddedMp4Urls(html))
            .filter(::isAllowedMediaUrl)
            .distinct()
            .maxByOrNull(::videoQualityScore)
            ?: return PinterestExtractionResult.Failure(
                "Kiwi couldn’t find a public video on that Pinterest page. The Pin may be private, unsupported, or no longer available.",
            )
        val previewUrl = metadata["og:image"]?.takeIf(::isAllowedMediaUrl)
        val title = metadata["og:title"]?.takeIf(String::isNotBlank)?.take(120) ?: "Pinterest video"
        val attribution = listOf("pinterestapp:creator_name", "article:author", "og:site_name")
            .firstNotNullOfOrNull(metadata::get)
            ?.takeIf(String::isNotBlank)
            ?.take(80)
            ?: "Pinterest"
        val mimeType = metadata["og:video:type"]
            ?.takeIf { it.startsWith("video/") }
            ?: "video/mp4"
        return PinterestExtractionResult.Success(
            PinterestMedia(sourceUrl, mediaUrl, previewUrl, title, attribution, mimeType),
        )
    }

    private fun isAllowedMediaUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() == "https" &&
            uri.userInfo == null &&
            PinterestUrlValidator.isPinterestMediaHost(uri.host?.lowercase().orEmpty())
    }

    private fun embeddedMp4Urls(html: String): List<String> {
        val normalized = html
            .replace("\\u002F", "/", ignoreCase = true)
            .replace("\\/", "/")
            .replace("\\u0026", "&", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
        return EMBEDDED_MP4.findAll(normalized).map { it.value }.toList()
    }

    private fun videoQualityScore(url: String): Int = VIDEO_WIDTH
        .find(url.substringBefore('?'))
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()
        ?: 0

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .trim()

    private val META_TAG = Regex("<meta\\s+[^>]*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ATTRIBUTE = Regex("""([:\w-]+)\s*=\s*(["'])(.*?)\2""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val EMBEDDED_MP4 = Regex(
        """https://[A-Za-z0-9.-]*pinimg\.com/[^"'<>\s\\]+\.mp4(?:\?[^"'<>\s\\]*)?""",
        RegexOption.IGNORE_CASE,
    )
    private val VIDEO_WIDTH = Regex("""(?:_|/)(\d{3,4})(?:w|p)?\.mp4$""", RegexOption.IGNORE_CASE)
}

class PublicPinterestExtractor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PinterestExtractor {
    override suspend fun extract(url: String): PinterestExtractionResult = withContext(dispatcher) {
        val normalized = PinterestUrlValidator.normalize(url)
            ?: return@withContext PinterestExtractionResult.Failure("Paste a public Pinterest Pin link to continue.")
        runCatching { fetchPublicPage(normalized) }
            .fold(
                onSuccess = { (finalUrl, html) -> PinterestMetadataParser.parse(finalUrl, html) },
                onFailure = {
                    PinterestExtractionResult.Failure(
                        it.message ?: "Kiwi couldn’t open that public Pinterest page. Check your connection and try again.",
                    )
                },
            )
    }

    private fun fetchPublicPage(initialUrl: String): Pair<String, String> {
        var current = initialUrl
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
                setRequestProperty("User-Agent", PUBLIC_BROWSER_USER_AGENT)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    if (redirectCount == MAX_REDIRECTS) error("That Pinterest link redirected too many times.")
                    val location = connection.getHeaderField("Location")
                        ?: error("Pinterest returned an incomplete redirect.")
                    val redirected = URI(current).resolve(location).toString()
                    current = PinterestUrlValidator.normalize(redirected)
                        ?: error("Pinterest redirected outside its public website.")
                    return@repeat
                }
                if (code !in 200..299) error("Pinterest returned ${code}; the Pin may not be publicly available.")
                val contentType = connection.contentType.orEmpty().lowercase()
                if (!contentType.contains("text/html") && !contentType.contains("xhtml")) {
                    error("Pinterest returned an unsupported page format.")
                }
                val html = connection.inputStream.use(::readBoundedUtf8)
                return current to html
            } finally {
                connection.disconnect()
            }
        }
        error("That Pinterest link redirected too many times.")
    }

    private fun readBoundedUtf8(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_PAGE_BYTES) error("The Pinterest page was too large to preview safely.")
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_PAGE_BYTES = 2 * 1024 * 1024
        const val CONNECT_TIMEOUT_MS = 12_000
        const val READ_TIMEOUT_MS = 15_000
        const val PUBLIC_BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 Kiwi/1.0"
    }
}

class AndroidPinterestDownloadGateway(context: Context) : PinterestDownloadGateway {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    override fun enqueue(media: PinterestMedia): Long {
        val safeTitle = media.title
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
            .take(48)
            .ifBlank { "pinterest-video" }
        val filename = "$safeTitle-${System.currentTimeMillis()}.mp4"
        val request = DownloadManager.Request(Uri.parse(media.mediaUrl))
            .setTitle(media.title)
            .setDescription("Saved from Pinterest by Kiwi")
            .setMimeType(media.mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Kiwi/$filename")
        return downloadManager.enqueue(request)
    }
}
