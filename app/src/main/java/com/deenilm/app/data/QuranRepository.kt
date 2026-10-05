package com.deenilm.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Ayah(
    val surah: Int,
    val verse: Int,
    val text: String,
    /** Global ayah number 1..6236 (from alquran.cloud) — used for audio URLs. 0 = unknown. */
    val globalNumber: Int = 0
)

data class SurahMeta(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val ayahs: Int
)

data class Reciter(val id: String, val name: String)

object QuranRepository {

    /** Bundled fallback — 114 surahs never change, so the app works fully offline too. */
    val SURAHS: List<SurahMeta> = listOf(
        Triple("الفاتحة", "Al-Fatihah", 7),
        Triple("البقرة", "Al-Baqarah", 286),
        Triple("آل عمران", "Aal-E-Imran", 200),
        Triple("النساء", "An-Nisa", 176),
        Triple("المائدة", "Al-Ma'idah", 120),
        Triple("الأنعام", "Al-An'am", 165),
        Triple("الأعراف", "Al-A'raf", 206),
        Triple("الأنفال", "Al-Anfal", 75),
        Triple("التوبة", "At-Taubah", 129),
        Triple("يونس", "Yunus", 109),
        Triple("هود", "Hud", 123),
        Triple("يوسف", "Yusuf", 111),
        Triple("الرعد", "Ar-Ra'd", 43),
        Triple("إبراهيم", "Ibrahim", 52),
        Triple("الحجر", "Al-Hijr", 99),
        Triple("النحل", "An-Nahl", 128),
        Triple("الإسراء", "Al-Isra", 111),
        Triple("الكهف", "Al-Kahf", 110),
        Triple("مريم", "Maryam", 98),
        Triple("طه", "Ta-Ha", 135),
        Triple("الأنبياء", "Al-Anbiya", 112),
        Triple("الحج", "Al-Hajj", 78),
        Triple("المؤمنون", "Al-Mu'minun", 118),
        Triple("النور", "An-Nur", 64),
        Triple("الفرقان", "Al-Furqan", 77),
        Triple("الشعراء", "Ash-Shu'ara", 227),
        Triple("النمل", "An-Naml", 93),
        Triple("القصص", "Al-Qasas", 88),
        Triple("العنكبوت", "Al-Ankabut", 69),
        Triple("الروم", "Ar-Rum", 60),
        Triple("لقمان", "Luqman", 34),
        Triple("السجدة", "As-Sajdah", 30),
        Triple("الأحزاب", "Al-Ahzab", 73),
        Triple("سبأ", "Saba", 54),
        Triple("فاطر", "Fatir", 45),
        Triple("يس", "Ya-Sin", 83),
        Triple("الصافات", "As-Saffat", 182),
        Triple("ص", "Sad", 88),
        Triple("الزمر", "Az-Zumar", 75),
        Triple("غافر", "Ghafir", 85),
        Triple("فصلت", "Fussilat", 54),
        Triple("الشورى", "Ash-Shura", 53),
        Triple("الزخرف", "Az-Zukhruf", 89),
        Triple("الدخان", "Ad-Dukhan", 59),
        Triple("الجاثية", "Al-Jathiyah", 37),
        Triple("الأحقاف", "Al-Ahqaf", 35),
        Triple("محمد", "Muhammad", 38),
        Triple("الفتح", "Al-Fath", 29),
        Triple("الحجرات", "Al-Hujurat", 18),
        Triple("ق", "Qaf", 45),
        Triple("الذاريات", "Adh-Dhariyat", 60),
        Triple("الطور", "At-Tur", 49),
        Triple("النجم", "An-Najm", 62),
        Triple("القمر", "Al-Qamar", 55),
        Triple("الرحمن", "Ar-Rahman", 78),
        Triple("الواقعة", "Al-Waqi'ah", 96),
        Triple("الحديد", "Al-Hadid", 29),
        Triple("المجادلة", "Al-Mujadila", 22),
        Triple("الحشر", "Al-Hashr", 24),
        Triple("الممتحنة", "Al-Mumtahanah", 13),
        Triple("الصف", "As-Saff", 14),
        Triple("الجمعة", "Al-Jumu'ah", 11),
        Triple("المنافقون", "Al-Munafiqun", 11),
        Triple("التغابن", "At-Taghabun", 18),
        Triple("الطلاق", "At-Talaq", 12),
        Triple("التحريم", "At-Tahrim", 12),
        Triple("الملك", "Al-Mulk", 30),
        Triple("القلم", "Al-Qalam", 52),
        Triple("الحاقة", "Al-Haqqah", 69),
        Triple("المعارج", "Al-Ma'arij", 44),
        Triple("نوح", "Nuh", 28),
        Triple("الجن", "Al-Jinn", 28),
        Triple("المزمل", "Al-Muzzammil", 20),
        Triple("المدثر", "Al-Muddaththir", 56),
        Triple("القيامة", "Al-Qiyamah", 40),
        Triple("الإنسان", "Al-Insan", 31),
        Triple("المرسلات", "Al-Mursalat", 50),
        Triple("النبأ", "An-Naba", 40),
        Triple("النازعات", "An-Nazi'at", 46),
        Triple("عبس", "Abasa", 42),
        Triple("التكوير", "At-Takwir", 29),
        Triple("الانفطار", "Al-Infitar", 19),
        Triple("المطففين", "Al-Mutaffifin", 36),
        Triple("الانشقاق", "Al-Inshiqaq", 25),
        Triple("البروج", "Al-Buruj", 22),
        Triple("الطارق", "At-Tariq", 17),
        Triple("الأعلى", "Al-A'la", 19),
        Triple("الغاشية", "Al-Ghashiyah", 26),
        Triple("الفجر", "Al-Fajr", 30),
        Triple("البلد", "Al-Balad", 20),
        Triple("الشمس", "Ash-Shams", 15),
        Triple("الليل", "Al-Layl", 21),
        Triple("الضحى", "Ad-Duha", 11),
        Triple("الشرح", "Ash-Sharh", 8),
        Triple("التين", "At-Tin", 8),
        Triple("العلق", "Al-Alaq", 19),
        Triple("القدر", "Al-Qadr", 5),
        Triple("البينة", "Al-Bayyinah", 8),
        Triple("الزلزلة", "Az-Zalzalah", 8),
        Triple("العاديات", "Al-Adiyat", 11),
        Triple("القارعة", "Al-Qari'ah", 11),
        Triple("التكاثر", "At-Takathur", 8),
        Triple("العصر", "Al-Asr", 3),
        Triple("الهمزة", "Al-Humazah", 9),
        Triple("الفيل", "Al-Fil", 5),
        Triple("قريش", "Quraysh", 4),
        Triple("الماعون", "Al-Ma'un", 7),
        Triple("الكوثر", "Al-Kawthar", 3),
        Triple("الكافرون", "Al-Kafirun", 6),
        Triple("النصر", "An-Nasr", 3),
        Triple("المسد", "Al-Masad", 5),
        Triple("الإخلاص", "Al-Ikhlas", 4),
        Triple("الفلق", "Al-Falaq", 5),
        Triple("الناس", "An-Nas", 6)
    ).mapIndexed { index, (arabicName, englishName, ayahs) ->
        SurahMeta(number = index + 1, arabicName = arabicName, englishName = englishName, ayahs = ayahs)
    }

    /* ------------------------------ surah list ------------------------------ */

    /** Live surah metadata from api.alquran.cloud, cached; falls back to the bundled list. */
    suspend fun surahMetas(cache: CacheStore): List<SurahMeta> = withContext(Dispatchers.IO) {
        val key = "quran_surah_list"
        // cached copy first, but only if it still parses
        cache.read(key)?.let { parseSurahMetas(it)?.let { parsed -> return@withContext parsed } }
        try {
            val json = ApiClient.get(QuranEditions.surahListUrl())
            parseSurahMetas(json)?.let { parsed ->
                cache.write(key, json)
                return@withContext parsed
            }
        } catch (e: Exception) {
            // network failed -> bundled fallback below
        }
        SURAHS
    }

    private fun parseSurahMetas(json: String): List<SurahMeta>? {
        return try {
            val data = JSONObject(json).getJSONArray("data")
            val list = List(data.length()) { i ->
                val o = data.getJSONObject(i)
                SurahMeta(
                    number = o.optInt("number", i + 1),
                    arabicName = o.optString("name"),
                    englishName = o.optString("englishName").ifBlank { o.optString("transliteration") },
                    ayahs = o.optInt("numberOfAyahs")
                )
            }.filter { it.number in 1..114 && it.ayahs > 0 && it.englishName.isNotBlank() }
            if (list.size == 114) list else null
        } catch (e: Exception) {
            null
        }
    }

    /* ------------------------------- reciters ------------------------------- */

    private fun defaultReciters(): List<Reciter> = listOf(
        Reciter("ar.alafasy", "Mishary Rashid Alafasy"),
        Reciter("ar.husary", "Mahmoud Khalil Al-Husary"),
        Reciter("ar.husarymujawwad", "Mahmoud Khalil Al-Husary (Mujawwad)")
    )

    /** Verse-by-verse reciter catalogue from api.alquran.cloud, cached; excludes the known-broken edition. */
    suspend fun reciters(cache: CacheStore): List<Reciter> = withContext(Dispatchers.IO) {
        val key = "quran_reciters"
        cache.read(key)?.let { parseReciters(it)?.let { parsed -> return@withContext parsed } }
        try {
            val json = ApiClient.get(QuranEditions.recitersUrl())
            parseReciters(json)?.let { parsed ->
                cache.write(key, json)
                return@withContext parsed
            }
        } catch (e: Exception) {
            // network failed -> defaults below
        }
        defaultReciters()
    }

    private fun parseReciters(json: String): List<Reciter>? {
        return try {
            val data = JSONObject(json).getJSONArray("data")
            val list = mutableListOf<Reciter>()
            for (i in 0 until data.length()) {
                val o = data.getJSONObject(i)
                val id = o.optString("identifier")
                if (id.isBlank() || id == QuranEditions.EXCLUDED_RECITER) continue
                if (o.optString("type") != "versebyverse") continue
                val name = o.optString("englishName").ifBlank { o.optString("name") }.ifBlank { id }
                list.add(Reciter(id, name))
            }
            if (list.isEmpty()) null else list
        } catch (e: Exception) {
            null
        }
    }

    /* ------------------------------ editions I/O ---------------------------- */

    /**
     * Fetches one alquran.cloud editions call and returns identifier -> ayahs.
     * Ayahs are paired by index across editions by the caller.
     */
    private suspend fun fetchEditions(
        surah: Int,
        cache: CacheStore,
        vararg editions: String
    ): Map<String, List<Ayah>> {
        val key = "quran_alq_${editions.joinToString("_") { it.replace(".", "-") }}_$surah"
        // cached copy first, but only if it still parses to real ayahs
        cache.read(key)?.let { cached ->
            val parsed = parseEditions(surah, cached)
            if (parsed.values.any { it.isNotEmpty() }) return parsed
        }
        val json = ApiClient.get(QuranEditions.editionsUrl(surah, *editions))
        val parsed = parseEditions(surah, json)
        if (parsed.values.any { it.isNotEmpty() }) cache.write(key, json)
        return parsed
    }

    private fun parseEditions(surah: Int, json: String): Map<String, List<Ayah>> {
        val out = mutableMapOf<String, List<Ayah>>()
        val data = JSONObject(json).optJSONArray("data") ?: return out
        for (i in 0 until data.length()) {
            val ed = data.optJSONObject(i) ?: continue
            // The editions endpoint nests the identifier inside an "edition" object
            // (top-level "identifier" is also accepted as a fallback).
            val id = ed.optJSONObject("edition")?.optString("identifier").orEmpty()
                .ifBlank { ed.optString("identifier") }
            if (id.isBlank()) continue
            val ayahs = ed.optJSONArray("ayahs") ?: continue
            out[id] = List(ayahs.length()) { j ->
                val a = ayahs.optJSONObject(j) ?: JSONObject()
                Ayah(
                    surah = surah,
                    verse = a.optInt("numberInSurah", j + 1),
                    text = a.optString("text"),
                    globalNumber = a.optInt("number", 0)
                )
            }
        }
        return out
    }

    /** Keep only translations that line up 1:1 with the Arabic by index; else drop (honest empty). */
    private fun aligned(arabic: List<Ayah>, translation: List<Ayah>): List<Ayah> =
        if (translation.size == arabic.size) translation else emptyList()

    /* --------------------------------- Arabic -------------------------------- */

    suspend fun arabicAyahs(context: Context, surah: Int, cache: CacheStore): List<Ayah> {
        val map = fetchEditions(surah, cache, QuranEditions.ARABIC)
        return map[QuranEditions.ARABIC]?.filter { it.text.isNotBlank() } ?: emptyList()
    }

    /** Latin transliteration ayahs (en.transliteration), aligned to the Arabic. */
    suspend fun transliterationAyahs(context: Context, surah: Int, cache: CacheStore): List<Ayah> {
        val arabic = arabicAyahs(context, surah, cache)
        val tr = fetchEditions(surah, cache, QuranEditions.ARABIC, QuranEditions.TRANSLITERATION)[QuranEditions.TRANSLITERATION]
            ?: emptyList()
        return aligned(arabic, tr)
    }

    /* ------------------------------- translations ---------------------------- */

    suspend fun translationAyahs(
        context: Context,
        surah: Int,
        lang: String,
        cache: CacheStore
    ): List<Ayah> {
        return when (lang) {
            "ur" -> quranProjectTranslation(context, surah, cache, "urdu", QuranEditions.URDU_FALLBACK)
            "bn" -> quranProjectTranslation(context, surah, cache, "bengali", QuranEditions.BANGLA_FALLBACK)
            "hi" -> {
                val arabic = arabicAyahs(context, surah, cache)
                val tr = fetchEditions(surah, cache, QuranEditions.ARABIC, QuranEditions.HINDI)[QuranEditions.HINDI]
                    ?: emptyList()
                aligned(arabic, tr)
            }
            else -> {
                val arabic = arabicAyahs(context, surah, cache)
                val tr = fetchEditions(surah, cache, QuranEditions.ARABIC, QuranEditions.ENGLISH)[QuranEditions.ENGLISH]
                    ?: emptyList()
                aligned(arabic, tr)
            }
        }
    }

    /**
     * The Quran Project is the PRIMARY Urdu/Bangla source.
     * Uses it only when the array length matches the Arabic ayah count and every
     * entry is non-blank; otherwise falls back to the alquran.cloud edition.
     */
    private suspend fun quranProjectTranslation(
        context: Context,
        surah: Int,
        cache: CacheStore,
        field: String,
        fallbackEdition: String
    ): List<Ayah> {
        val arabic = arabicAyahs(context, surah, cache)
        val key = "quran_qp_${field}_$surah"
        // cached copy first, but only if it validates
        cache.read(key)?.let { cached ->
            extractQuranProject(cached, field, arabic.size, surah)?.let { return it }
        }
        try {
            val json = ApiClient.get(QuranEditions.quranProjectUrl(surah))
            extractQuranProject(json, field, arabic.size, surah)?.let { valid ->
                cache.write(key, json)
                return valid
            }
        } catch (e: Exception) {
            // fall through to the alquran.cloud fallback below
        }
        val tr = fetchEditions(surah, cache, QuranEditions.ARABIC, fallbackEdition)[fallbackEdition]
            ?: emptyList()
        return aligned(arabic, tr)
    }

    /**
     * Extracts a validated translation array from a Quran Project response,
     * or null when it doesn't line up with the Arabic.
     */
    private fun extractQuranProject(
        json: String,
        field: String,
        arabicCount: Int,
        surah: Int
    ): List<Ayah>? {
        return try {
            val arr = JSONObject(json).optJSONArray(field) ?: return null
            if (arabicCount == 0 || arr.length() != arabicCount) return null
            val texts = List(arr.length()) { i -> arr.optString(i, "").trim() }
            if (texts.any { it.isBlank() }) return null
            List(texts.size) { i -> Ayah(surah, i + 1, texts[i]) }
        } catch (e: Exception) {
            null
        }
    }
}
