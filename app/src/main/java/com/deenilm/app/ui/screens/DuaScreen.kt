package com.deenilm.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deenilm.app.data.DuaCategory
import com.deenilm.app.data.DuaDetail
import com.deenilm.app.data.DuaListItem
import com.deenilm.app.data.DuaRepository
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.theme.ArabicFont
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenEmerald
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOnGold
import com.deenilm.app.ui.theme.DeenSurface

private sealed interface DuaLevel {
    data object Categories : DuaLevel
    data class DuaList(val category: DuaCategory) : DuaLevel
    data class DuaDetailView(val category: DuaCategory, val item: DuaListItem) : DuaLevel
}

@Composable
fun DuaScreen() {
    val context = LocalContext.current
    var level by remember { mutableStateOf<DuaLevel>(DuaLevel.Categories) }
    var categories by remember { mutableStateOf<List<DuaCategory>?>(null) }
    var duaItems by remember { mutableStateOf<List<DuaListItem>?>(null) }
    var duaDetail by remember { mutableStateOf<DuaDetail?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableStateOf(0) }

    fun loadDuaList(category: DuaCategory) {
        loading = true
        error = null
        duaItems = null
        duaDetail = null
        level = DuaLevel.DuaList(category)
    }

    fun loadDetail(category: DuaCategory, item: DuaListItem) {
        loading = true
        error = null
        duaDetail = null
        level = DuaLevel.DuaDetailView(category, item)
    }

    LaunchedEffect(level, retryNonce) {
        try {
            when (val l = level) {
                is DuaLevel.Categories -> categories = DuaRepository.getCategories(context)
                is DuaLevel.DuaList -> duaItems = DuaRepository.getDuasInCategory(context, l.category.id)
                is DuaLevel.DuaDetailView -> duaDetail = DuaRepository.getDuaDetail(context, l.item.globalId)
            }
            loading = false
        } catch (e: Exception) {
            loading = false
            error = e.message ?: "Something went wrong."
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeenBg)) {
        EightPointStarBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            DuaTopBar(
                level = level,
                onBack = {
                    level = when (val l = level) {
                        is DuaLevel.DuaDetailView -> DuaLevel.DuaList(l.category)
                        is DuaLevel.DuaList -> DuaLevel.Categories
                        is DuaLevel.Categories -> DuaLevel.Categories
                    }
                }
            )
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = DeenGold)
                            Spacer(Modifier.height(12.dp))
                            Text(text = "Loading duas…", color = DeenCream.copy(alpha = 0.7f))
                        }
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = DeenGold.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = error ?: "Something went wrong.",
                                color = DeenCream,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    error = null
                                    loading = true
                                    retryNonce += 1
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeenGold,
                                    contentColor = DeenOnGold
                                )
                            ) {
                                Text(text = "Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                else -> when (val l = level) {
                    is DuaLevel.Categories -> CategoryList(
                        categories = categories.orEmpty(),
                        onSelect = { loadDuaList(it) }
                    )
                    is DuaLevel.DuaList -> DuaListPane(
                        items = duaItems.orEmpty(),
                        onSelect = { loadDetail(l.category, it) }
                    )
                    is DuaLevel.DuaDetailView -> DuaDetailPane(detail = duaDetail)
                }
            }
        }
    }
}

@Composable
private fun DuaTopBar(level: DuaLevel, onBack: () -> Unit) {
    val title = when (level) {
        is DuaLevel.Categories -> "Duas & Azkar"
        is DuaLevel.DuaList -> level.category.name
        is DuaLevel.DuaDetailView -> level.item.name.ifBlank { "Dua" }
    }
    val subtitle = when (level) {
        is DuaLevel.Categories -> "18 categories · Hisnul Muslim (Bangla)"
        is DuaLevel.DuaList -> "${level.category.duaCount} duas"
        is DuaLevel.DuaDetailView -> level.category.name
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (level !is DuaLevel.Categories) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = DeenGold)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = DeenGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DeenCream.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<DuaCategory>,
    onSelect: (DuaCategory) -> Unit
) {
    if (categories.isEmpty()) {
        EmptyMessage("No categories found.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it.id }) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(cat) },
                colors = CardDefaults.cardColors(containerColor = DeenSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = DeenGold
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = DeenCream,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${cat.duaCount} duas",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeenCream.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = DeenGold.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DuaListPane(
    items: List<DuaListItem>,
    onSelect: (DuaListItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyMessage("No duas found in this category.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.globalId }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
                colors = CardDefaults.cardColors(containerColor = DeenSurface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = item.name.ifBlank { "Dua" },
                        style = MaterialTheme.typography.titleMedium,
                        color = DeenCream,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.chapter.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.chapter,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeenCream.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaDetailPane(detail: DuaDetail?) {
    if (detail == null) {
        EmptyMessage("Dua not found.")
        return
    }
    if (detail.segments.isEmpty()) {
        EmptyMessage("No content for this dua.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        detail.segments.forEachIndexed { index, seg ->
            item(key = "seg_$index") {
                DuaSegmentCard(seg, index + 1, detail.segments.size > 1)
            }
        }
    }
}

@Composable
private fun DuaSegmentCard(seg: com.deenilm.app.data.DuaSegment, number: Int, showNumber: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeenSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            if (showNumber) {
                Text(
                    text = "Part $number",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeenEmerald,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (seg.top.isNotBlank()) {
                Text(
                    text = seg.top,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeenCream.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(10.dp))
            }
            if (seg.arabic.isNotBlank()) {
                Text(
                    text = seg.arabic,
                    fontFamily = ArabicFont,
                    fontSize = 24.sp,
                    lineHeight = 42.sp,
                    color = DeenCream,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
            if (seg.transliteration.isNotBlank()) {
                Text(
                    text = "উচ্চারণ",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeenGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = seg.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeenCream.copy(alpha = 0.8f),
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            if (seg.translation.isNotBlank()) {
                Text(
                    text = "অনুবাদ",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeenGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = seg.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeenCream,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            if (seg.bottom.isNotBlank()) {
                Text(
                    text = seg.bottom,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeenCream.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            if (seg.reference.isNotBlank()) {
                Text(
                    text = "রেফারেন্স: ${seg.reference}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeenEmerald
                )
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = DeenCream.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
