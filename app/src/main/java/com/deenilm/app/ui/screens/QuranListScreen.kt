package com.deenilm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.deenilm.app.data.CacheStore
import com.deenilm.app.data.QuranRepository
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.components.IslamicArchHeader
import com.deenilm.app.ui.theme.ArabicFont
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOutline
import com.deenilm.app.ui.theme.DeenSurface
import com.deenilm.app.util.toArabicIndic

@Composable
fun QuranListScreen(nav: NavController) {
    val context = LocalContext.current
    val cache = remember { CacheStore(context) }
    var query by remember { mutableStateOf("") }
    // Bundled list shows instantly; refreshed from api.alquran.cloud when online.
    var surahs by remember { mutableStateOf(QuranRepository.SURAHS) }

    LaunchedEffect(Unit) {
        surahs = QuranRepository.surahMetas(cache)
    }

    val filtered = remember(query, surahs) {
        val q = query.trim()
        if (q.isEmpty()) {
            surahs
        } else {
            surahs.filter { s ->
                s.englishName.contains(q, ignoreCase = true) ||
                    s.arabicName.contains(q) ||
                    s.number.toString() == q
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeenBg)
    ) {
        EightPointStarBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            IslamicArchHeader(
                title = "القرآن الكريم",
                subtitle = "The Holy Quran — 114 Surahs"
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or number…") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = DeenGold)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeenGold,
                    unfocusedBorderColor = DeenOutline,
                    focusedTextColor = DeenCream,
                    unfocusedTextColor = DeenCream,
                    cursorColor = DeenGold
                )
            )
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No surahs match \"$query\"",
                        color = DeenCream.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 24.dp
                    )
                ) {
                    items(filtered, key = { it.number }) { surah ->
                        Card(
                            onClick = { nav.navigate("quran/surah/${surah.number}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = DeenSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.5.dp, DeenGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = surah.number.toArabicIndic(),
                                        color = DeenGold,
                                        fontFamily = ArabicFont,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = surah.englishName,
                                        color = DeenCream,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${surah.ayahs} ayahs",
                                        color = DeenCream.copy(alpha = 0.55f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = surah.arabicName,
                                    fontFamily = ArabicFont,
                                    fontSize = 24.sp,
                                    color = DeenGold,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
