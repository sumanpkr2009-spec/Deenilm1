package com.deenilm.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats a remaining duration as H:MM:SS, or MM:SS when under an hour. */
private fun countdownText(ms: Long): String {
    if (ms <= 0) return "now"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val lat by prefs.lat.collectAsState(initial = 0.0)
    val lng by prefs.lng.collectAsState(initial = 0.0)
    val calcMethod by prefs.calcMethod.collectAsState(initial = "KARACHI")
    val madhab by prefs.madhab.collectAsState(initial = "SHAFI")

    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            nowTick = System.currentTimeMillis()
        }
    }

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Assalamu Alaikum"
    }
    val hijri = hijriDateString()
    val gregorian = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
        )
    }
    val dateLine = if (hijri.isNotEmpty()) "$hijri • $gregorian" else gregorian

    val hasLocation = lat != 0.0 || lng != 0.0
    val times = remember(lat, lng, calcMethod, madhab) {
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
            IslamicArchHeader(title = greeting, subtitle = dateLine)

            Spacer(Modifier.height(16.dp))

            // ---- Prayer-times HERO card ----
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
                        text = "Prayer Times",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeenGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        !hasLocation -> {
                            Text(
                                text = "Location not set",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DeenCream,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Set your location to see accurate prayer times for today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeenCream.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { nav.navigate("settings") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeenGold,
                                    contentColor = DeenOnGold
                                )
                            ) {
                                Text("Set location")
                            }
                        }
                        times == null -> {
                            Text(
                                text = "Prayer times unavailable",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DeenCream,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Could not calculate prayer times for this location. Please check your location settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeenCream.copy(alpha = 0.75f)
                            )
                        }
                        else -> {
                            Text(
                                text = "Next prayer",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeenCream.copy(alpha = 0.7f)
                            )
                            if (next != null) {
                                Text(
                                    text = next.name,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = DeenGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = PrayerHelper.formatTime(next.millis),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = DeenCream
                                )
                                Text(
                                    text = "in ${countdownText(next.millis - nowTick)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeenEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = DeenOutline)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val fajr = times.first { it.name == "Fajr" }
                                val maghrib = times.first { it.name == "Maghrib" }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Fajr",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DeenCream.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = PrayerHelper.formatTime(fajr.millis),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeenCream,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Maghrib",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DeenCream.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = PrayerHelper.formatTime(maghrib.millis),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeenCream,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Quick access grid ----
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                color = DeenGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))

            val quickItems = listOf(
                Triple(Icons.Filled.MenuBook, "Quran", "quran"),
                Triple(Icons.Filled.LibraryBooks, "Hadith", "hadith"),
                Triple(Icons.Filled.Favorite, "Dua", "dua"),
                Triple(Icons.Filled.AccessTime, "Tasbih", "tasbih"),
                Triple(Icons.Filled.Schedule, "Prayer Times", "prayer"),
                Triple(Icons.Filled.Explore, "Qibla", "qibla"),
                Triple(Icons.Filled.AutoStories, "Books", "books"),
                Triple(Icons.Filled.Settings, "Settings", "settings")
            )
            quickItems.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (icon, label, route) ->
                        QuickCard(
                            icon = icon,
                            label = label,
                            modifier = Modifier.weight(1f),
                            onClick = { nav.navigate(route) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DeenSurface),
        border = BorderStroke(1.dp, DeenOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeenGold,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = DeenCream,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
