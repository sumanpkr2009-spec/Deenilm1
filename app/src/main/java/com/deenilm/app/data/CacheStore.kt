package com.deenilm.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CacheStore(context: Context) {
    private val dir = File(context.filesDir, "cache").apply { mkdirs() }

    private fun fileFor(key: String) =
        File(dir, key.replace(Regex("[^A-Za-z0-9]"), "_") + ".json")

    suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        val f = fileFor(key)
        if (f.exists()) f.readText() else null
    }

    suspend fun write(key: String, json: String) = withContext(Dispatchers.IO) {
        fileFor(key).writeText(json)
    }
}
