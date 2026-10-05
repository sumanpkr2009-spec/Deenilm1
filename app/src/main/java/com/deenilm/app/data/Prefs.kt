package com.deenilm.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deen_ilm_prefs")

class Prefs(private val context: Context) {
    companion object {
        val LANG = stringPreferencesKey("lang")               // en|ur|bn|hi
        val CALC_METHOD = stringPreferencesKey("calc_method") // KARACHI|MUSLIM_WORLD_LEAGUE|EGYPTIAN
        val MADHAB = stringPreferencesKey("madhab")           // SHAFI|HANAFI
        val LAT = doublePreferencesKey("lat")                 // 0.0 = unset
        val LNG = doublePreferencesKey("lng")
        val RECITER = stringPreferencesKey("quran_reciter") // alquran.cloud audio edition id
        val PRON_SCRIPT = stringPreferencesKey("pronunciation_script") // en|hi|bn|ur, "" = follow app lang
        val TASBIH_DATE = stringPreferencesKey("tasbih_date")
        val TASBIH_TOTAL = intPreferencesKey("tasbih_total")
    }

    val lang: Flow<String> = context.dataStore.data.map { it[LANG] ?: "en" }
    val calcMethod: Flow<String> = context.dataStore.data.map { it[CALC_METHOD] ?: "KARACHI" }
    val madhab: Flow<String> = context.dataStore.data.map { it[MADHAB] ?: "SHAFI" }
    val lat: Flow<Double> = context.dataStore.data.map { it[LAT] ?: 0.0 }
    val lng: Flow<Double> = context.dataStore.data.map { it[LNG] ?: 0.0 }
    val reciter: Flow<String> = context.dataStore.data.map { it[RECITER] ?: QuranEditions.DEFAULT_RECITER }
    /** Pronunciation script override; blank = follow the app language. */
    val pronScript: Flow<String> = context.dataStore.data.map { it[PRON_SCRIPT] ?: "" }
    val tasbihDate: Flow<String> = context.dataStore.data.map { it[TASBIH_DATE] ?: "" }
    val tasbihTotal: Flow<Int> = context.dataStore.data.map { it[TASBIH_TOTAL] ?: 0 }

    suspend fun setLang(v: String) = set(LANG, v)
    suspend fun setCalcMethod(v: String) = set(CALC_METHOD, v)
    suspend fun setMadhab(v: String) = set(MADHAB, v)
    suspend fun setLat(v: Double) = set(LAT, v)
    suspend fun setLng(v: Double) = set(LNG, v)
    suspend fun setReciter(v: String) = set(RECITER, v)
    suspend fun setPronScript(v: String) = set(PRON_SCRIPT, v)
    suspend fun setTasbihDate(v: String) = set(TASBIH_DATE, v)
    suspend fun setTasbihTotal(v: Int) = set(TASBIH_TOTAL, v)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
