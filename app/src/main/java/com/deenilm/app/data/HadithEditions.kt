package com.deenilm.app.data

object HadithEditions {
    const val BASE = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions"
    val BOOKS = linkedMapOf(
        "bukhari" to "Sahih al Bukhari",
        "muslim" to "Sahih Muslim",
        "abudawud" to "Sunan Abu Dawud",
        "tirmidhi" to "Jami At Tirmidhi",
        "nasai" to "Sunan an Nasai",
        "ibnmajah" to "Sunan Ibn Majah",
        "malik" to "Muwatta Malik",
        "dehlawi" to "Mishkat al-Masabih"
    )
    fun prefixFor(lang: String, book: String): String {
        if (book == "dehlawi") return "eng"
        return when (lang) { "ur" -> "urd"; "bn" -> "ben"; "hi" -> "eng"; else -> "eng" }
    }
    fun hadithUrl(prefix: String, book: String, n: Int) = "$BASE/$prefix-$book/$n.min.json"
}
