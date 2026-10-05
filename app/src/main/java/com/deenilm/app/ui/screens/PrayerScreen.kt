package com.deenilm.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.deenilm.app.ui.theme.DeenOnGold
import com.deenilm.app.ui.theme.DeenOutline
import com.deenilm.app.ui.theme.DeenSurface
import com.deenilm.app.util.hijriDateString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun countdownText(ms: Long): String {
    if (ms <= 0) return "now"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun prettyEnum(value: String): String =
    value.split("_").joinToString(" ") { part ->
        part.lowercase().replaceFirstChar { it.uppercaseChar() }
    }

@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    val lat by prefs.lat.collectAsState(initial = 0.0)
    val lng by prefs.lng.collectAsState(initial = 0.0)
    val calcMethod by prefs.calcMethod.collectAsState(initial = "KARACHI")
    val madhab by prefs.madhab.collectAsState(initial = "SHAFI")

    var refreshTick by remember { mutableStateOf(0) }
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    var locating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            nowTick = System.currentTimeMillis()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            locating = true
            scope.launch {
                PrayerHelper.resolveLocation(context, prefs)
                locating = false
                refreshTick++
            }
        }
    }

    val hasLocation = lat != 0.0 || lng != 0.0
    val times = remember(lat, lng, calcMethod, madhab, refreshTick) {
        if (hasLocation) PrayerHelper.getTimes(lat, lng, calcMethod, madhab) else null
    }
    val next = times?.let { PrayerHelper.nextPrayer(it, nowTick) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeenBg)
    ) {
        EightPointStarBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            IslamicArchHeader(
                title = "Prayer Times",
                subtitle = hijriDateString().ifEmpty { null }
            )
            Spacer(Modifier.height(16.dp))

            if (!hasLocation) {
                // ---- Location unset state ----
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
                            text = "Location needed",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeenGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Prayer times are calculated for your exact location. " +
                                "Allow location access to detect it automatically, or set it " +
                                "manually in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeenCream.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                launcher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            enabled = !locating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeenGold,
                                contentColor = DeenOnGold
                            )
                        ) {
                            Text(if (locating) "Locating…" else "Use current location")
                        }
                    }
                }
            } else if (times == null) {
                // ---- Error state ----
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
                            text = "Prayer times unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeenGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Could not calculate prayer times for this location. " +
                                "Please check your location settings and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeenCream.copy(alpha = 0.85f)
                        )
                    }
                }
            } else {
                // ---- Next-prayer banner ----
                next?.let { n ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DeenGold.copy(alpha = 0.14f)
                        ),
                        border = BorderStroke(1.5.dp, DeenGold)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Next prayer",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeenCream.copy(alpha = 0.7f)
                            )
                            Text(
                                text = n.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = DeenGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = PrayerHelper.formatTime(n.millis),
                                style = MaterialTheme.typography.headlineSmall,
                                color = DeenCream
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "in ${countdownText(n.millis - nowTick)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeenEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ---- All 6 times ----
                times.forEach { prayer ->
                    val isNext = prayer.name == next?.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNext) DeenGold.copy(alpha = 0.14f) else DeenSurface
                        ),
                        border = if (isNext) BorderStroke(1.5.dp, DeenGold)
                        else BorderStroke(1.dp, DeenOutline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prayer.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isNext) DeenGold else DeenCream,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = PrayerHelper.formatTime(prayer.millis),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isNext) DeenGold else DeenCream.copy(alpha = 0.85f),
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color = DeenOutline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Method: ${prettyEnum(calcMethod)} • Madhab: ${prettyEnum(madhab)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeenCream.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
