package com.deenilm.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deenilm.app.data.Prefs
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.theme.ArabicFont
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenSurface
import com.deenilm.app.ui.theme.DeenSurfaceVariant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class DhikrPreset(val name: String, val arabic: String, val target: Int)

private val PRESETS = listOf(
    DhikrPreset("SubhanAllah", "سبحان الله", 33),
    DhikrPreset("Alhamdulillah", "الحمد لله", 33),
    DhikrPreset("Allahu Akbar", "الله أكبر", 34),
    DhikrPreset("La ilaha illa Allah", "لا إله إلا الله", 100),
    DhikrPreset("Astaghfirullah", "أستغفر الله", 100)
)

@Suppress("DEPRECATION")
@Composable
fun TasbihScreen() {
    val context = LocalContext.current
    val prefs = remember(context) { Prefs(context) }
    val scope = rememberCoroutineScope()
    val vibrator = remember(context) {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    var selected by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    val preset = PRESETS[selected]

    val storedDate by prefs.tasbihDate.collectAsState(initial = "")
    val todayTotal by prefs.tasbihTotal.collectAsState(initial = 0)

    LaunchedEffect(storedDate) {
        val today = LocalDate.now().toString()
        if (storedDate != today) {
            prefs.setTasbihDate(today)
            prefs.setTasbihTotal(0)
        }
    }

    fun buzz() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(30)
        }
    }

    val complete = count >= preset.target

    Box(modifier = Modifier.fillMaxSize().background(DeenBg)) {
        EightPointStarBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tasbih",
                style = MaterialTheme.typography.headlineSmall,
                color = DeenGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Today's total: $todayTotal",
                style = MaterialTheme.typography.bodyMedium,
                color = DeenCream.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(PRESETS) { i, p ->
                    FilterChip(
                        selected = selected == i,
                        onClick = {
                            if (selected != i) {
                                selected = i
                                count = 0
                            }
                        },
                        label = { Text(p.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DeenSurface,
                            labelColor = DeenCream,
                            selectedContainerColor = DeenGold.copy(alpha = 0.25f),
                            selectedLabelColor = DeenGold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = (count.toFloat() / preset.target).coerceIn(0f, 1f),
                    modifier = Modifier.size(230.dp),
                    strokeWidth = 10.dp,
                    color = DeenGold,
                    trackColor = DeenSurface
                )
                Button(
                    onClick = {
                        buzz()
                        count++
                        scope.launch {
                            val cur = prefs.tasbihTotal.first()
                            prefs.setTasbihTotal(cur + 1)
                        }
                    },
                    modifier = Modifier.size(200.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = DeenSurfaceVariant)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = preset.arabic,
                            fontFamily = ArabicFont,
                            fontSize = 26.sp,
                            color = DeenCream,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$count",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeenGold
                        )
                        Text(
                            text = "$count / ${preset.target}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeenCream.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (complete) {
                Text(
                    text = "MashaAllah! Target complete",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeenGold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { count = 0 }) {
                    Text("Reset")
                }
            } else {
                Text(
                    text = "Tap the circle for each dhikr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeenCream.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
