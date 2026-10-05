package com.deenilm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class HadithSection(val number: String, val name: String, val first: Int, val last: Int)
data class Hadith(val number: Int, val displayNumber: String, val text: String)

object HadithRepository {
    private fun parseHadith(book: String, json: String): Hadith {
        val o = JSONObject(json).getJSONArray("hadiths").getJSONObject(0)
        val num = o.getInt("hadithnumber")
        val display = if (book == "muslim") o.optInt("arabicnumber", num).toString() else num.toString()
        return Hadith(num, display, o.getString("text"))
    }

    suspend fun getHadith(book: String, prefix: String, n: Int, cache: CacheStore): Hadith {
        val key = "hadith_${prefix}_${book}_$n"
        cache.read(key)?.let { return parseHadith(book, it) }
        val json = ApiClient.get(HadithEditions.hadithUrl(prefix, book, n))
        cache.write(key, json)
        return parseHadith(book, json)
    }

    suspend fun getSections(book: String, prefix: String, cache: CacheStore): List<HadithSection> {
        // metadata comes from any single-hadith response; use hadith #1
        val key = "hadithmeta_${prefix}_$book"
        val json = cache.read(key) ?: ApiClient.get(HadithEditions.hadithUrl(prefix, book, 1)).also { cache.write(key, it) }
        val meta = JSONObject(json).getJSONObject("metadata")
        val sections = meta.getJSONObject("section")
        val detail = meta.optJSONObject("section_detail")
        val out = mutableListOf<HadithSection>()
        val keys = sections.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: 0 }
        for (k in keys) {
            var first = -1; var last = -1
            val d = detail?.optJSONObject(k)
            if (d != null) {
                first = d.optInt("hadithnumber_first", -1)
                last = d.optInt("hadithnumber_last", -1)
            }
            out.add(HadithSection(k, sections.getString(k), first, last))
        }
        return out
    }

    // Fetch hadiths numbered from..to (inclusive), cached individually. Stops early on 404.
    suspend fun getHadithRange(book: String, prefix: String, from: Int, to: Int, cache: CacheStore): List<Hadith> =
        withContext(Dispatchers.IO) {
            val out = mutableListOf<Hadith>()
            for (n in from..to) {
                try { out.add(getHadith(book, prefix, n, cache)) }
                catch (e: Exception) {
                    if (e.message?.contains("404") == true) break else throw e
                }
            }
            out
        }

    // Detail: merge Arabic + translation
    suspend fun getHadithDetail(book: String, lang: String, n: Int, cache: CacheStore): Pair<Hadith, Hadith> =
        withContext(Dispatchers.IO) {
            val prefix = HadithEditions.prefixFor(lang, book)
            val ar = async { getHadith(book, "ara", n, cache) }
            val tr = async { if (prefix == "ara") getHadith(book, "ara", n, cache) else getHadith(book, prefix, n, cache) }
            Pair(ar.await(), tr.await())
        }
}
