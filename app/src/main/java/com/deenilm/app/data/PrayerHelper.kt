package com.deenilm.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class NamedPrayer(val name: String, val millis: Long)

object PrayerHelper {

    // Calculation method angles: (fajrAngle, ishaAngle)
    private fun methodAngles(method: String): Pair<Double, Double> = when (method.uppercase()) {
        "KARACHI" -> 18.0 to 18.0
        "MUSLIM_WORLD_LEAGUE" -> 18.0 to 17.0
        "EGYPTIAN" -> 19.5 to 17.5
        else -> 18.0 to 18.0 // default Karachi
    }

    private fun asrFactor(madhab: String): Double =
        if (madhab.uppercase() == "HANAFI") 2.0 else 1.0

    /**
     * Computes prayer times using standard solar-position math.
     * Returns null if inputs are invalid.
     */
    fun getTimes(
        lat: Double,
        lng: Double,
        method: String,
        madhab: String,
        date: LocalDate = LocalDate.now()
    ): List<NamedPrayer>? = try {
        val (fajrAngle, ishaAngle) = methodAngles(method)
        val asrF = asrFactor(madhab)

        // Julian date at noon UTC for the given date
        val jd = julianDate(date.year, date.monthValue, date.dayOfMonth)

        val decl = sunDeclination(jd)
        val eqt = equationOfTime(jd) // minutes

        // Solar noon in UTC minutes from midnight
        val noonUtcMin = 720.0 - 4.0 * lng - eqt

        fun timeForAngle(angleDeg: Double, isMorning: Boolean): Double {
            // Hour angle for the given solar elevation angle
            val latRad = Math.toRadians(lat)
            val declRad = Math.toRadians(decl)
            val angleRad = Math.toRadians(angleDeg)
            var cosH = (sin(angleRad) - sin(latRad) * sin(declRad)) /
                    (cos(latRad) * cos(declRad))
            cosH = cosH.coerceIn(-1.0, 1.0)
            val hDeg = Math.toDegrees(acos(cosH))
            return if (isMorning) noonUtcMin - hDeg * 4.0 else noonUtcMin + hDeg * 4.0
        }

        fun asrTime(): Double {
            // Asr when shadow = object + factor * object
            val latRad = Math.toRadians(lat)
            val declRad = Math.toRadians(decl)
            val angle = atan(1.0 / (asrF + tan(abs(latRad - declRad))))
            val angleDeg = Math.toDegrees(angle)
            // elevation angle above horizon (negative of depression)
            val latR = latRad
            val dR = declRad
            val eRad = Math.toRadians(angleDeg)
            var cosH = (sin(eRad) - sin(latR) * sin(dR)) / (cos(latR) * cos(dR))
            cosH = cosH.coerceIn(-1.0, 1.0)
            val hDeg = Math.toDegrees(acos(cosH))
            return noonUtcMin + hDeg * 4.0
        }

        // Convert UTC minutes -> epoch millis.
        fun toMillis(utcMinutes: Double): Long {
            // utcMinutes is minutes after 00:00 UTC; convert to millis offset then
            // shift by the zone offset at that date.
            val utcInstant = date.atStartOfDay(java.time.ZoneOffset.UTC)
                .plusMinutes(utcMinutes.toLong())
                .plusNanos(((utcMinutes % 1.0) * 60e9).toLong())
                .toInstant()
            return utcInstant.toEpochMilli()
        }

        val fajr = toMillis(timeForAngle(-fajrAngle, true))
        val sunrise = toMillis(timeForAngle(-0.833, true))
        val dhuhr = toMillis(noonUtcMin)
        val asr = toMillis(asrTime())
        val maghrib = toMillis(timeForAngle(-0.833, false))
        val isha = toMillis(timeForAngle(-ishaAngle, false))

        listOf(
            NamedPrayer("Fajr", fajr),
            NamedPrayer("Sunrise", sunrise),
            NamedPrayer("Dhuhr", dhuhr),
            NamedPrayer("Asr", asr),
            NamedPrayer("Maghrib", maghrib),
            NamedPrayer("Isha", isha)
        )
    } catch (e: Exception) {
        null
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunDeclination(jd: Double): Double {
        val d = jd - 2451543.5
        val w = 282.9404 + 4.70935e-5 * d
        val e = 0.016709 - 1.151e-9 * d
        val m = 356.0470 + 0.9856002585 * d
        val mRad = Math.toRadians(m)
        val eAnom = m + e * (180 / PI) * sin(mRad) * (1 + e * cos(mRad))
        val eAnomRad = Math.toRadians(eAnom)
        val x = cos(eAnomRad) - e
        val y = sin(eAnomRad) * sqrt(1 - e * e)
        val v = Math.toDegrees(atan2(y, x))
        val lon = v + w
        val lonRad = Math.toRadians(lon)
        val xe = cos(lonRad)
        val ye = sin(lonRad)
        val obleq = 23.4393 - 3.563e-7 * d
        val obleqRad = Math.toRadians(obleq)
        val ze = sin(obleqRad) * ye
        return Math.toDegrees(asin(ze))
    }

    private fun equationOfTime(jd: Double): Double {
        val d = jd - 2451543.5
        val w = 282.9404 + 4.70935e-5 * d
        val e = 0.016709 - 1.151e-9 * d
        val m = 356.0470 + 0.9856002585 * d
        val mRad = Math.toRadians(m)
        val eAnom = m + e * (180 / PI) * sin(mRad) * (1 + e * cos(mRad))
        val eAnomRad = Math.toRadians(eAnom)
        val x = cos(eAnomRad) - e
        val y = sin(eAnomRad) * sqrt(1 - e * e)
        val v = Math.toDegrees(atan2(y, x))
        val lon = v + w
        var eqt = lon - m
        // normalize to [-180, 180]
        while (eqt > 180) eqt -= 360
        while (eqt < -180) eqt += 360
        return 4.0 * eqt // minutes
    }

    fun formatTime(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    fun nextPrayer(times: List<NamedPrayer>, now: Long = System.currentTimeMillis()): NamedPrayer? =
        times.filter { it.name != "Sunrise" }.firstOrNull { it.millis > now }
            ?: times.filter { it.name != "Sunrise" }.firstOrNull() // tomorrow's Fajr approx

    /**
     * Great-circle initial bearing from (lat, lng) to the Kaaba in Makkah.
     */
    fun qiblaBearing(lat: Double, lng: Double): Double {
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLng = Math.toRadians(39.8262)
        val latRad = Math.toRadians(lat)
        val dLng = kaabaLng - Math.toRadians(lng)
        val y = sin(dLng)
        val x = cos(latRad) * tan(kaabaLat) - sin(latRad) * cos(dLng)
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        return bearing
    }

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
