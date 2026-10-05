package com.deenilm.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.deenilm.app.data.CacheStore
import com.deenilm.app.data.Hadith
import com.deenilm.app.data.HadithEditions
import com.deenilm.app.data.HadithRepository
import com.deenilm.app.data.Prefs
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.components.IslamicArchHeader
import com.deenilm.app.ui.theme.ArabicFont
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOnGold
import com.deenilm.app.ui.theme.DeenOutline
import com.deenilm.app.util.toArabicIndic

@Composable
fun HadithDetailScreen(bookSlug: String, number: Int) {
    val context = LocalContext.current
    val cache = remember { CacheStore(context) }
    val prefs = remember { Prefs(context) }
    val lang by prefs.lang.collectAsState(initial = "en")
    val bookName = HadithEditions.BOOKS[bookSlug] ?: bookSlug

    var n by remember(bookSlug) { mutableStateOf(number) }
    var detail by remember { mutableStateOf<Pair<Hadith, Hadith>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(bookSlug, lang, n, retryKey) {
        loading = true
        error = null
        try {
            detail = HadithRepository.getHadithDetail(bookSlug, lang, n, cache)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load hadith"
        } finally {
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeenBg)) {
        EightPointStarBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            IslamicArchHeader(
                title = bookName,
                subtitle = "Hadith ${n.toArabicIndic()}"
            )

            when {
                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = DeenGold) }

                error != null -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeenCream.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { retryKey++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeenGold,
                                contentColor = DeenOnGold
                            )
                        ) { Text("Retry") }
                        if (n > 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { n-- }) {
                                Text("Back to previous hadith", color = DeenGold)
                            }
                        }
                    }
                }

                detail != null -> {
                    val (arabic, translation) = detail!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "Hadith ${n.toArabicIndic()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeenGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = arabic.text,
                            fontFamily = ArabicFont,
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = TextDirection.Rtl
                            ),
                            textAlign = TextAlign.Start,
                            color = DeenCream
                        )
                        HorizontalDivider(
                            color = DeenOutline,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                        Text(
                            text = translation.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DeenCream
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (n > 1) {
                                OutlinedButton(onClick = { n-- }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous hadith"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Previous")
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Button(
                                onClick = { n++ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeenGold,
                                    contentColor = DeenOnGold
                                )
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next hadith"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
