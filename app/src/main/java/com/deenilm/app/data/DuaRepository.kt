package com.deenilm.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hisnul Muslim dua API (https://dua-api.hisnul.workers.dev) — Arabic + Bangla
 * transliteration/translation, 18 categories, ~421 duas, no API key required.
 *
 * Live-verified endpoints (2026-10-05):
 *   GET /api/categories                 -> {"success":true,"total":18,"data":[{"id","name","dua_count"}]}
 *   GET /api/categories/{id}/duas?page=N&limit=100
 *        -> {"success":true,"pagination":{...},"data":[{"dua_global_id","chapname","duaname","tags"}]}
 *   GET /api/duas/{duaGlobalId}
 *        -> {"success":true,"data":{"dua_global_id","duaname","segments":[{...}]}}
 *        segment: {"top","arabic","arabic_diacless","transliteration","translations","reference","bottom"}
 *
 * NOTE: the API exposes no explicit "repeat count" field; `top`/`bottom` context
 * text carries any such guidance when present.
 */
data class DuaCategory(val id: Int, val name: String, val duaCount: Int)

data class DuaListItem(
    val globalId: Int,
    val name: String,
    val chapter: String,
    val tags: String
)

data class DuaSegment(
    val top: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String,
    val bottom: String
)

data class DuaDetail(
    val globalId: Int,
    val name: String,
    val segments: List<DuaSegment>
)

object DuaRepository {
    private const val BASE = "https://dua-api.hisnul.workers.dev"

    private suspend fun fetchCached(context: Context, key: String, url: String): String =
        withContext(Dispatchers.IO) {
            val store = CacheStore(context)
            try {
                val fresh = ApiClient.get(url)
                // sanity: must be a success JSON envelope
                if (JSONObject(fresh).optBoolean("success", false)) {
                    store.write(key, fresh)
                    return@withContext fresh
                }
            } catch (e: Exception) {
                // fall through to cache
            }
            val cached = store.read(key)
            if (!cached.isNullOrBlank()) return@withContext cached
            throw IllegalStateException("Could not load data. Check your connection and retry.")
        }

    suspend fun getCategories(context: Context): List<DuaCategory> {
        val json = fetchCached(context, "hm_categories", "$BASE/api/categories")
        val arr = JSONObject(json).optJSONArray("data") ?: JSONArray()
        val out = ArrayList<DuaCategory>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                DuaCategory(
                    id = o.optInt("id"),
                    name = o.optString("name"),
                    duaCount = o.optInt("dua_count")
                )
            )
        }
        return out
    }

    suspend fun getDuasInCategory(context: Context, categoryId: Int): List<DuaListItem> {
        // Paginate: page 1 first to learn the page count, then fetch the rest.
        val first = fetchCached(
            context,
            "hm_cat_${categoryId}_p1",
            "$BASE/api/categories/$categoryId/duas?page=1&limit=100"
        )
        val firstObj = JSONObject(first)
        val pages = firstObj.optJSONObject("pagination")?.optInt("pages", 1) ?: 1
        val all = ArrayList<DuaListItem>()
        all += parseDuaList(firstObj.optJSONArray("data") ?: JSONArray())
        for (p in 2..pages) {
            val json = fetchCached(
                context,
                "hm_cat_${categoryId}_p$p",
                "$BASE/api/categories/$categoryId/duas?page=$p&limit=100"
            )
            all += parseDuaList(JSONObject(json).optJSONArray("data") ?: JSONArray())
        }
        return all
    }

    private fun parseDuaList(arr: JSONArray): List<DuaListItem> {
        val out = ArrayList<DuaListItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                DuaListItem(
                    globalId = o.optInt("dua_global_id"),
                    name = o.optString("duaname"),
                    chapter = o.optString("chapname"),
                    tags = o.optString("tags")
                )
            )
        }
        return out
    }

    suspend fun getDuaDetail(context: Context, globalId: Int): DuaDetail {
        val json = fetchCached(context, "hm_dua_$globalId", "$BASE/api/duas/$globalId")
        val data = JSONObject(json).getJSONObject("data")
        val segs = data.optJSONArray("segments") ?: JSONArray()
        val out = ArrayList<DuaSegment>(segs.length())
        for (i in 0 until segs.length()) {
            val s = segs.getJSONObject(i)
            val arabic = s.optString("arabic").ifBlank { s.optString("arabic_diacless") }
            out.add(
                DuaSegment(
                    top = s.optString("top"),
                    arabic = arabic,
                    transliteration = s.optString("transliteration"),
                    translation = s.optString("translations"),
                    reference = s.optString("reference"),
                    bottom = s.optString("bottom")
                )
            )
        }
        return DuaDetail(
            globalId = data.optInt("dua_global_id", globalId),
            name = data.optString("duaname"),
            segments = out
        )
    }
}
