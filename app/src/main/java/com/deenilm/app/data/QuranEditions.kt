package com.deenilm.app.data

/**
 * Web-app Quran stack (masud's choice):
 * - api.alquran.cloud for surah list, Uthmani Arabic, en.asad / hi.hindi translations,
 *   ur.jalandhry + bn.bengali fallbacks, and the reciter catalogue.
 * - The Quran Project (quranapi.pages.dev) as PRIMARY Urdu + Bangla translation.
 * - cdn.islamic.network for verse-by-verse Arabic tilawat audio.
 * - everyayah.com for Urdu translation audio (Shamshad Ali Khan).
 * All free, no API keys.
 */
object QuranEditions {
    const val ARABIC = "quran-uthmani"
    const val ENGLISH = "en.asad"
    const val HINDI = "hi.hindi"
    const val URDU_FALLBACK = "ur.jalandhry"
    const val BANGLA_FALLBACK = "bn.bengali"
    /** Latin transliteration — the basis for pronunciation in every script. */
    const val TRANSLITERATION = "en.transliteration"

    const val DEFAULT_RECITER = "ar.alafasy"

    /** Known-broken in the audio CDN (HTTP 403) — excluded from the catalogue. */
    const val EXCLUDED_RECITER = "ar.abdulbasitmurattal"

    fun surahListUrl(): String =
        "https://api.alquran.cloud/v1/surah"

    fun editionsUrl(surah: Int, vararg editions: String): String =
        "https://api.alquran.cloud/v1/surah/$surah/editions/${editions.joinToString(",")}"

    fun recitersUrl(): String =
        "https://api.alquran.cloud/v1/edition/format/audio"

    fun quranProjectUrl(surah: Int): String =
        "https://quranapi.pages.dev/api/$surah.json"

    /** Verse-by-verse Arabic tilawat. globalAyah is 1..6236 (ayah.number from alquran.cloud). */
    fun arabicAudioUrl(reciter: String, globalAyah: Int): String =
        "https://cdn.islamic.network/quran/audio/128/$reciter/$globalAyah.mp3"

    /** Urdu translation audio (Shamshad Ali Khan). everyayah uses SSSAAA zero-padded keys. */
    fun urduAudioUrl(surah: Int, ayahInSurah: Int): String {
        val s = surah.toString().padStart(3, '0')
        val a = ayahInSurah.toString().padStart(3, '0')
        return "https://everyayah.com/data/translations/urdu_shamshad_ali_khan_46kbps/$s$a.mp3"
    }

    /** Bangla translation audio (imranpollob/bangla-quran, MIT). NOT zero-padded: {surah}-{ayah}.mp3 */
    fun banglaAudioUrl(surah: Int, ayahInSurah: Int): String =
        "https://raw.githubusercontent.com/imranpollob/bangla-quran/master/public/audio/bt/$surah-$ayahInSurah.mp3"
}
