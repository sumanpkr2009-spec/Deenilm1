package com.deenilm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Translates Indonesian-language content into the user's language
 * (English / Hindi / Urdu / Bangla) at runtime, so no Indonesian text ever
 * reaches the UI. Uses the free Google Translate endpoint (no key), with:
 * - disk cache (per text+target, forever — translations don't change),
 * - in-memory cache for the session,
 * - chunking for long texts (tafsir etc.),
 * - bounded parallelism for lists.
 *
 * On any failure the original text is returned (never crash the screen).
 */
object Translator {

    /** App language codes we support for translated content. */
    val SUPPORTED = listOf("en", "hi", "ur", "bn")

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val memCache = ConcurrentHashMap<String, String>()

    private fun md5(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun cacheKey(text: String, target: String): String =
        "tr:id:$target:${md5(text)}"

    /** Split a long text into <= [max] char chunks at sentence/line boundaries. */
    private fun chunk(text: String, max: Int = 3500): List<String> {
        if (text.length <= max) return listOf(text)
        val out = mutableListOf<String>()
        var rest = text
        while (rest.length > max) {
            var cut = -1
            // prefer a sentence/line boundary in the last 25% of the window
            val windowStart = (max * 0.75).toInt()
            for (i in max downTo windowStart) {
                val c = rest[i]
                if (c == '.' || c == '!' || c == '?' || c == '\n') { cut = i + 1; break }
            }
            if (cut <= 0) {
                // fall back to a space
                cut = rest.lastIndexOf(' ', max).takeIf { it > windowStart } ?: max
            }
            out.add(rest.substring(0, cut).trim())
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) out.add(rest)
        return out
    }

    private fun parseGtx(body: String): String {
        val root = JSONArray(body)
        val sentences = root.getJSONArray(0)
        val sb = StringBuilder()
        for (i in 0 until sentences.length()) {
            val seg = sentences.optJSONArray(i) ?: continue
            sb.append(seg.optString(0, ""))
        }
        return sb.toString()
    }

    private suspend fun fetchOne(chunkText: String, target: String): String = withContext(Dispatchers.IO) {
        val url = "https://translate.googleapis.com/translate_a/single" +
            "?client=gtx&sl=id&tl=${enc(target)}&dt=t&q=${enc(chunkText)}"
        val req = Request.Builder().url(url)
            .header("User-Agent", "DeenIlm/1.0")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("translate http ${resp.code}")
            val body = resp.body?.string() ?: throw IllegalStateException("empty translate body")
            parseGtx(body)
        }
    }

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    /**
     * Translate one text from Indonesian to [target]. Returns the original text
     * unchanged when it is blank or the translation fails.
     */
    suspend fun translate(text: String, target: String, cache: CacheStore): String {
        val t = text.trim()
        if (t.isEmpty() || target !in SUPPORTED) return text
        val key = cacheKey(t, target)
        memCache[key]?.let { return it }
        cache.read(key)?.let { saved ->
            if (saved.isNotEmpty()) { memCache[key] = saved; return saved }
        }
        return try {
            val chunks = chunk(t)
            val translated = if (chunks.size == 1) {
                fetchOne(chunks[0], target)
            } else {
                coroutineScope {
                    chunks.map { c -> async { fetchOne(c, target) } }.awaitAll()
                }.joinToString(" ")
            }
            val final = translated.ifBlank { text }
            memCache[key] = final
            cache.write(key, final)
            final
        } catch (e: Exception) {
            text // honest fallback: never break the screen
        }
    }

    /**
     * Translate many texts, preserving order. Duplicates are translated once.
     * Concurrency is bounded so list screens stay fast without hammering the endpoint.
     */
    suspend fun translateBatch(
        texts: List<String>,
        target: String,
        cache: CacheStore,
        concurrency: Int = 6
    ): List<String> = coroutineScope {
        if (target !in SUPPORTED) return@coroutineScope texts
        val sem = Semaphore(concurrency)
        // de-duplicate identical inputs: translate once, fan out
        val unique = texts.distinct()
        val results = mutableMapOf<String, String>()
        unique.map { txt ->
            async {
                sem.withPermit { txt to translate(txt, target, cache) }
            }
        }.awaitAll().forEach { (txt, out) -> results[txt] = out }
        texts.map { results[it] ?: it }
    }
}
