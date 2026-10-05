package com.deenilm.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One entry from books_catalog.json (assets). */
data class BookEntry(
    val title: String,
    val url: String,
    val sizeBytes: Long
) {
    val sizeMb: Double get() = sizeBytes / 1_048_576.0
}

/**
 * Bangla books library: 220 PDFs hosted on Google Drive (direct download URLs).
 * Catalog lives in assets/books_catalog.json: {"count":220,"books":[{"title","url","size_bytes"}]}
 */
object BookCatalog {

    @Volatile
    private var cached: List<BookEntry>? = null

    suspend fun load(context: Context): List<BookEntry> {
        cached?.let { return it }
        val list = withContext(Dispatchers.IO) {
            val json = context.assets.open("books_catalog.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("books")
            val out = ArrayList<BookEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    BookEntry(
                        title = o.optString("title"),
                        url = o.optString("url"),
                        sizeBytes = o.optLong("size_bytes")
                    )
                )
            }
            out
        }
        cached = list
        return list
    }
}
