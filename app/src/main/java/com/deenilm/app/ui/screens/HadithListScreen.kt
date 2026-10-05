package com.deenilm.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deenilm.app.data.CacheStore
import com.deenilm.app.data.Hadith
import com.deenilm.app.data.HadithEditions
import com.deenilm.app.data.HadithRepository
import com.deenilm.app.data.HadithSection
import com.deenilm.app.data.Prefs
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.components.IslamicArchHeader
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenEmerald
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOnGold
import com.deenilm.app.ui.theme.DeenSurface
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

@Composable
fun HadithListScreen(bookSlug: String, nav: NavController) {
    val context = LocalContext.current
    val cache = remember { CacheStore(context) }
    val prefs = remember { Prefs(context) }
    val lang by prefs.lang.collectAsState(initial = "en")
    val prefix = HadithEditions.prefixFor(lang, bookSlug)
    val bookName = HadithEditions.BOOKS[bookSlug] ?: bookSlug
    val scope = rememberCoroutineScope()

    var sections by remember { mutableStateOf<List<HadithSection>?>(null) }
    var sectionsError by remember { mutableStateOf<String?>(null) }
    var sectionsReload by remember { mutableStateOf(0) }
    var selectedSection by remember { mutableStateOf<HadithSection?>(null) }

    var hadiths by remember { mutableStateOf<List<Hadith>>(emptyList()) }
    var nextFrom by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var pageLoading by remember { mutableStateOf(false) }
    var hadithsError by remember { mutableStateOf<String?>(null) }

    // Load chapter list; reset selection on book/lang change.
    LaunchedEffect(bookSlug, lang, sectionsReload) {
        selectedSection = null
        sections = null
        sectionsError = null
        try {
            sections = HadithRepository.getSections(bookSlug, prefix, cache)
        } catch (e: Exception) {
            sectionsError = e.message ?: "Failed to load chapters"
        }
    }

    fun loadNextPage() {
        val s = selectedSection ?: return
        if (pageLoading || !hasMore) return
        scope.launch {
            pageLoading = true
            hadithsError = null
            try {
                val end = if (s.last > 0) minOf(nextFrom + PAGE_SIZE - 1, s.last) else nextFrom + PAGE_SIZE - 1
                val requested = end - nextFrom + 1
                val page = HadithRepository.getHadithRange(bookSlug, prefix, nextFrom, end, cache)
                hadiths = hadiths + page
                nextFrom = end + 1
                hasMore = page.size == requested && (s.last <= 0 || end < s.last)
            } catch (e: Exception) {
                hadithsError = e.message ?: "Failed to load hadiths"
            } finally {
                pageLoading = false
            }
        }
    }

    fun openSection(s: HadithSection, index: Int, all: List<HadithSection>) {
        val start = when {
            s.first > 0 -> s.first
            index > 0 && all[index - 1].last > 0 -> all[index - 1].last + 1
            else -> 1
        }
        hadiths = emptyList()
        hadithsError = null
        hasMore = true
        nextFrom = start
        selectedSection = s
        loadNextPage()
    }

    BackHandler(enabled = selectedSection != null) { selectedSection = null }

    val section = selectedSection
    Box(modifier = Modifier.fillMaxSize().background(DeenBg)) {
        EightPointStarBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            IslamicArchHeader(
                title = bookName,
                subtitle = if (section == null) "Chapters" else "Chapter ${section.number}: ${section.name}"
            )

            if (section == null) {
                // ---- Phase 1: chapter list ----
                when {
                    sectionsError != null -> HadithErrorState(
                        message = sectionsError!!,
                        onRetry = { sectionsReload++ }
                    )
                    sections == null -> HadithLoadingState()
                    sections!!.isEmpty() -> HadithEmptyState("No chapters found for this book.")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(sections!!) { index, s ->
                            val rangeText = when {
                                s.first > 0 && s.last > 0 -> "Hadith ${s.first}\u2013${s.last}"
                                s.first > 0 -> "From hadith ${s.first}"
                                else -> "Browse hadiths"
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    openSection(s, index, sections!!)
                                },
                                colors = CardDefaults.cardColors(containerColor = DeenSurface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = s.number,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeenGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(44.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DeenCream,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = rangeText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DeenCream.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ---- Phase 2: hadith list for the selected chapter ----
                TextButton(
                    onClick = { selectedSection = null },
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chapters", tint = DeenGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All chapters", color = DeenGold)
                }
                if (hadiths.isNotEmpty()) {
                    Text(
                        text = "${hadiths.size} hadith${if (hadiths.size == 1) "" else "s"} loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeenCream.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                when {
                    hadiths.isEmpty() && pageLoading -> HadithLoadingState()
                    hadiths.isEmpty() && hadithsError == null -> HadithEmptyState("No hadiths found in this chapter.")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hadiths, key = { it.number }) { h ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    nav.navigate("hadith/book/$bookSlug/hadith/${h.number}")
                                },
                                colors = CardDefaults.cardColors(containerColor = DeenSurface)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(
                                        text = h.displayNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeenGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(52.dp)
                                    )
                                    Text(
                                        text = h.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeenCream,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        item {
                            when {
                                pageLoading -> Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = DeenGold) }
                                hadithsError != null -> Column(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = hadithsError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DeenCream.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { loadNextPage() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DeenGold,
                                            contentColor = DeenOnGold
                                        )
                                    ) { Text("Retry") }
                                }
                                hasMore -> Box(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { loadNextPage() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DeenEmerald,
                                            contentColor = DeenOnGold
                                        )
                                    ) { Text("Load more") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator(color = DeenGold) }
}

@Composable
private fun HadithErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DeenCream.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeenGold,
                    contentColor = DeenOnGold
                )
            ) { Text("Retry") }
        }
    }
}

@Composable
private fun HadithEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = DeenCream.copy(alpha = 0.7f)
        )
    }
}
