package com.deenilm.app.ui.screens

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deenilm.app.data.PrayerHelper
import com.deenilm.app.data.Prefs
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.components.IslamicArchHeader
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenEmerald
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOutline
import com.deenilm.app.ui.theme.DeenSurface
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val lat by prefs.lat.collectAsState(initial = 0.0)
    val lng by prefs.lng.collectAsState(initial = 0.0)

    val hasLocation = lat != 0.0 || lng != 0.0
    val bearing = remember(lat, lng) {
        if (hasLocation) PrayerHelper.qiblaBearing(lat, lng) else null
    }

    var azimuth by remember { mutableStateOf(0f) } // device heading, true north, 0..360
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(lat, lng) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotVec = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val smoothed = floatArrayOf(0f)
        var gravity: FloatArray? = null
        var geomag: FloatArray? = null

        fun updateFromAzimuth(rawAzimuth: Float) {
            // Low-pass filter to smooth jitter
            smoothed[0] = smoothed[0] + 0.15f * (rawAzimuth - smoothed[0])
            // Declination correction: magnetic north -> true north
            val decl = try {
                GeomagneticField(
                    lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()
                ).declination
            } catch (e: Exception) {
                0f
            }
            azimuth = (smoothed[0] + decl + 360f) % 360f
        }

        fun updateFromGravityAndMag() {
            val g = gravity
            val m = geomag
            if (g == null || m == null) return
            val rm = FloatArray(9)
            val orient = FloatArray(3)
            if (SensorManager.getRotationMatrix(rm, null, g, m)) {
                SensorManager.getOrientation(rm, orient)
                updateFromAzimuth(Math.toDegrees(orient[0].toDouble()).toFloat())
            }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rm = FloatArray(9)
                        val orient = FloatArray(3)
                        SensorManager.getRotationMatrixFromVector(rm, e.values)
                        SensorManager.getOrientation(rm, orient)
                        updateFromAzimuth(Math.toDegrees(orient[0].toDouble()).toFloat())
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = e.values.clone()
                        updateFromGravityAndMag()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomag = e.values.clone()
                        updateFromGravityAndMag()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        when {
            rotVec != null ->
                sm.registerListener(listener, rotVec, SensorManager.SENSOR_DELAY_UI)
            accel != null && mag != null -> {
                sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
                sm.registerListener(listener, mag, SensorManager.SENSOR_DELAY_UI)
            }
            else -> sensorAvailable = false
        }
        onDispose { sm.unregisterListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeenBg)
    ) {
        EightPointStarBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IslamicArchHeader(
                title = "Qibla",
                subtitle = "Find the direction of the Kaaba"
            )
            Spacer(Modifier.height(16.dp))

            when {
                !hasLocation -> {
                    // ---- Location unset guidance ----
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeenSurface),
                        border = BorderStroke(1.dp, DeenOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Location not set",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeenGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "The qibla direction depends on where you are. " +
                                    "Set your location in Settings so the qibla bearing " +
                                    "and compass declination are accurate for you.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeenCream.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                !sensorAvailable -> {
                    // ---- No compass hardware ----
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeenSurface),
                        border = BorderStroke(1.dp, DeenOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Compass sensor not available",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeenGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "This device has no rotation or magnetic sensors, so the " +
                                    "live compass cannot run. Your qibla bearing is " +
                                    "${"%.1f".format(bearing ?: 0.0)}° clockwise from true north.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeenCream.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                else -> {
                    // ---- Compass dial ----
                    val qibla = bearing ?: 0.0
                    val rel = ((qibla - azimuth + 360f) % 360f).toDouble() // degrees to turn

                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val c = center
                            val r = size.minDimension / 2f

                            // Outer rings
                            drawCircle(color = DeenGold, radius = r, style = Stroke(width = 3f))
                            drawCircle(
                                color = DeenOutline,
                                radius = r * 0.97f,
                                style = Stroke(width = 1.5f)
                            )

                            // Degree ticks every 15°
                            for (deg in 0 until 360 step 15) {
                                val rad = Math.toRadians(deg.toDouble()).toFloat()
                                val major = deg % 90 == 0
                                val inner = r * if (major) 0.88f else 0.93f
                                val outer = r * 0.985f
                                drawLine(
                                    color = DeenGold.copy(alpha = if (major) 1f else 0.55f),
                                    start = Offset(
                                        c.x + inner * sin(rad),
                                        c.y - inner * cos(rad)
                                    ),
                                    end = Offset(
                                        c.x + outer * sin(rad),
                                        c.y - outer * cos(rad)
                                    ),
                                    strokeWidth = if (major) 4f else 2f
                                )
                            }

                            // Gold Kaaba marker at the qibla bearing (fixed on dial)
                            val qbRad = Math.toRadians(qibla).toFloat()
                            val qOff = Offset(
                                c.x + r * 0.55f * sin(qbRad),
                                c.y - r * 0.55f * cos(qbRad)
                            )
                            val half = 11f
                            drawRect(
                                color = DeenGold,
                                topLeft = Offset(qOff.x - half, qOff.y - half),
                                size = Size(half * 2, half * 2)
                            )

                            // Needle: rotated by (qibla - azimuth)
                            val relRad = Math.toRadians(rel).toFloat()
                            val tip = Offset(
                                c.x + r * 0.45f * sin(relRad),
                                c.y - r * 0.45f * cos(relRad)
                            )
                            val tail = Offset(
                                c.x - r * 0.12f * sin(relRad),
                                c.y + r * 0.12f * cos(relRad)
                            )
                            drawLine(
                                color = DeenEmerald,
                                start = tail,
                                end = tip,
                                strokeWidth = 7f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = DeenGold, radius = 9f, center = c)
                        }

                        // Cardinal labels
                        Text(
                            text = "N",
                            color = DeenGold,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 30.dp)
                        )
                        Text(
                            text = "S",
                            color = DeenCream.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 30.dp)
                        )
                        Text(
                            text = "E",
                            color = DeenCream.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 30.dp)
                        )
                        Text(
                            text = "W",
                            color = DeenCream.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 30.dp)
                        )

                        // Center: degrees to turn
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${rel.roundToInt()}°",
                                style = MaterialTheme.typography.headlineMedium,
                                color = DeenGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "to Qibla",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeenCream.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Qibla: ${"%.1f".format(qibla)}° • Facing: ${"%.1f".format(azimuth)}°",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeenCream,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Turn until the needle points straight up (0°).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeenCream.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
