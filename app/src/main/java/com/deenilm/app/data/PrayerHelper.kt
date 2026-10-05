package com.deenilm.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

data class NamedPrayer(val name: String, val millis: Long)

object PrayerHelper {
    fun getTimes(
        lat: Double,
        lng: Double,
        method: String,
        madhab: String,
        date: LocalDate = LocalDate.now()
    ): List<NamedPrayer>? = try {
        val params = CalculationMethod.valueOf(method).parameters
        params.madhab = Madhab.valueOf(madhab)
        val t = PrayerTimes(
            Coordinates(lat, lng),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            params
        )
        listOf(
            NamedPrayer("Fajr", t.fajr.time),
            NamedPrayer("Sunrise", t.sunrise.time),
            NamedPrayer("Dhuhr", t.dhuhr.time),
            NamedPrayer("Asr", t.asr.time),
            NamedPrayer("Maghrib", t.maghrib.time),
            NamedPrayer("Isha", t.isha.time)
        )
    } catch (e: Exception) {
        null
    }

    fun formatTime(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    fun nextPrayer(times: List<NamedPrayer>, now: Long = System.currentTimeMillis()): NamedPrayer? =
        times.filter { it.name != "Sunrise" }.firstOrNull { it.millis > now }
            ?: times.filter { it.name != "Sunrise" }.firstOrNull() // tomorrow's Fajr approx

    fun qiblaBearing(lat: Double, lng: Double): Double = Qibla(Coordinates(lat, lng)).direction

    /**
     * Resolves a usable location. If a location permission is already granted, tries the last
     * known GPS/network fix, persists it to Prefs and returns it. Otherwise falls back to the
     * stored Prefs lat/lng (null when never set).
     */
    suspend fun resolveLocation(context: Context, prefs: Prefs): Pair<Double, Double>? {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = try {
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }
            if (loc != null) {
                prefs.setLat(loc.latitude)
                prefs.setLng(loc.longitude)
                return loc.latitude to loc.longitude
            }
        }
        val lat = prefs.lat.first()
        val lng = prefs.lng.first()
        return if (lat != 0.0 || lng != 0.0) lat to lng else null
    }
}
