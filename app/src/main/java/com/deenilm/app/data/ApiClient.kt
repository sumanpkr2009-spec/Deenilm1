package com.deenilm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", "DeenIlm/2.0").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            resp.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    suspend fun getAuthed(url: String, bearer: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", "DeenIlm/2.0")
            .header("Authorization", "Bearer $bearer").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            resp.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    suspend fun post(url: String, jsonBody: String, bearer: String): String = withContext(Dispatchers.IO) {
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).header("User-Agent", "DeenIlm/2.0")
            .header("Authorization", "Bearer $bearer").post(body).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            resp.body?.string() ?: throw IOException("Empty body for $url")
        }
    }
}
