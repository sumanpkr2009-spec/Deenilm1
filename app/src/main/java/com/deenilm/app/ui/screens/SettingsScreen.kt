package com.deenilm.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deenilm.app.data.Prefs
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.components.IslamicArchHeader
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenEmerald
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOnGold
import com.deenilm.app.ui.theme.DeenSurface
import com.deenilm.app.ui.theme.DeenSurfaceVariant
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()

    val lang by prefs.lang.collectAsState(initial = "en")
    val calcMethod by prefs.calcMethod.collectAsState(initial = "KARACHI")
    val madhab by prefs.madhab.collectAsState(initial = "SHAFI")
    val latPref by prefs.lat.collectAsState(initial = 0.0)
    val lngPref by prefs.lng.collectAsState(initial = 0.0)

    var latText by remember(latPref) { mutableStateOf(if (latPref != 0.0) latPref.toString() else "") }
    var lngText by remember(lngPref) { mutableStateOf(if (lngPref != 0.0) lngPref.toString() else "") }
    var locMessage by remember { mutableStateOf<String?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            locMessage = "Location permission was denied — you can enter coordinates manually."
            return@rememberLauncherForActivityResult
        }
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                scope.launch {
                    prefs.setLat(loc.latitude)
                    prefs.setLng(loc.longitude)
                    locMessage = "Location saved."
                }
            } else {
                locMessage = "No recent location found on this device — please enter coordinates manually."
            }
        } catch (_: SecurityException) {
            locMessage = "Location permission was denied."
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeenBg)) {
        EightPointStarBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            IslamicArchHeader(
                title = "Settings",
                subtitle = "Language, prayer times and location"
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    SettingsSection("Language") {
                        ChipRow(
                            options = listOf("en" to "English", "ur" to "Urdu", "bn" to "Bangla", "hi" to "Hindi"),
                            selected = lang,
                            onSelect = { scope.launch { prefs.setLang(it) } }
                        )
                    }
                }
                item {
                    SettingsSection("Prayer calculation") {
                        ChipRow(
                            options = listOf(
                                "KARACHI" to "Karachi",
                                "MUSLIM_WORLD_LEAGUE" to "Muslim World League",
                                "EGYPTIAN" to "Egyptian"
                            ),
                            selected = calcMethod,
                            onSelect = { scope.launch { prefs.setCalcMethod(it) } }
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Madhab (Asr time)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeenCream,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        ChipRow(
                            options = listOf("SHAFI" to "Shafi'i", "HANAFI" to "Hanafi"),
                            selected = madhab,
                            onSelect = { scope.launch { prefs.setMadhab(it) } }
                        )
                    }
                }
                item {
                    SettingsSection("Location") {
                        Text(
                            text = if (latPref != 0.0 || lngPref != 0.0)
                                "Saved: $latPref, $lngPref"
                            else "No location saved yet — prayer times need your coordinates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeenCream.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = latText,
                                onValueChange = { latText = it },
                                label = { Text("Latitude") },
                                placeholder = { Text("23.81") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lngText,
                                onValueChange = { lngText = it },
                                label = { Text("Longitude") },
                                placeholder = { Text("90.41") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val lat = latText.trim().toDoubleOrNull()
                                    val lng = lngText.trim().toDoubleOrNull()
                                    if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                                        locMessage = "Enter valid coordinates (lat −90…90, lng −180…180)."
                                    } else {
                                        scope.launch {
                                            prefs.setLat(lat)
                                            prefs.setLng(lng)
                                            locMessage = "Location saved."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeenGold,
                                    contentColor = DeenOnGold
                                )
                            ) {
                                Text("Save location")
                            }
                            OutlinedButton(
                                onClick = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            ) {
                                Text("Use current location", color = DeenGold)
                            }
                        }
                        locMessage?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = DeenEmerald
                            )
                        }
                    }
                }
                item {
                    SettingsSection("About") {
                        AboutRow("Deen Ilm", "v1.0")
                        AboutRow("Quran data", "alquran.cloud + The Quran Project")
                        AboutRow("Hadith data", "fawazahmed0/hadith-api")
                        AboutRow("Prayer times", "adhan (batoulapps)")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = DeenGold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = DeenSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeenGold,
                    selectedLabelColor = DeenOnGold,
                    containerColor = DeenSurfaceVariant,
                    labelColor = DeenCream
                )
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = DeenCream)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DeenCream.copy(alpha = 0.6f)
        )
    }
}
