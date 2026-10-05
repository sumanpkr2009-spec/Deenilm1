package com.deenilm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import com.deenilm.app.data.Ayah
import com.deenilm.app.data.CacheStore
import com.deenilm.app.data.Prefs
import com.deenilm.app.data.Pronunciation
import com.deenilm.app.data.QuranAudio
import com.deenilm.app.data.QuranEditions
import com.deenilm.app.data.QuranRepository
import com.deenilm.app.data.Reciter
import kotlinx.coroutines.launch
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.components.IslamicArchHeader
import com.deenilm.app.ui.theme.ArabicFont
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOnGold
import com.deenilm.app.ui.theme.DeenOutline
import com.deenilm.app.ui.theme.DeenSurface
import com.deenilm.app.util.toArabicIndic

private sealed interface ReaderState {
    data object Loading : ReaderState
    data object Empty : ReaderState
    data class Ready(
        val arabic: List<Ayah>,
        val translation: List<Ayah>,
        val transliteration: List<Ayah> = emptyList()
    ) : ReaderState
    data class Error(val message: String) : ReaderState
}

private fun langDisplayName(lang: String): String = when (lang) {
    "en" -> "English"
    "ur" -> "Urdu"
    "bn" -> "Bangla"
    "hi" -> "Hindi"
    else -> lang
}

@Composable
fun SurahReaderScreen(surahId: Int, nav: NavController? = null) {
    val context = LocalContext.current
    val cache = remember { CacheStore(context) }
    val prefs = remember { Prefs(context) }
    val lang by prefs.lang.collectAsState(initial = "en")
    var retryCount by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<ReaderState>(ReaderState.Loading) }

    val surah = QuranRepository.SURAHS.getOrNull(surahId - 1)

    // ---- pronunciation state ----
    var showPronunciation by remember { mutableStateOf(true) }
    val pronOverride by prefs.pronScript.collectAsState(initial = "")
    val pronScript = pronOverride.ifBlank { Pronunciation.defaultScriptFor(lang) }

    // ---- audio state ----
    val reciter by prefs.reciter.collectAsState(initial = QuranEditions.DEFAULT_RECITER)
    var reciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var audioMode by remember { mutableStateOf("tilawat") } // tilawat | urdu | bangla
    var playingVerse by remember { mutableStateOf<Int?>(null) }
    var playAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        reciters = QuranRepository.reciters(cache)
    }

    // stop any playback when the reciter or mode changes
    LaunchedEffect(reciter, audioMode) {
        QuranAudio.stop()
        playingVerse = null
        playAll = false
    }

    // release the player when leaving the screen
    DisposableEffect(Unit) {
        onDispose { QuranAudio.stop() }
    }

    fun audioUrlFor(index: Int): String? {
        val s = state as? ReaderState.Ready ?: return null
        val ayah = s.arabic.getOrNull(index) ?: return null
        return when (audioMode) {
            "urdu" -> QuranEditions.urduAudioUrl(surahId, ayah.verse)
            "bangla" -> QuranEditions.banglaAudioUrl(surahId, ayah.verse)
            else -> {
                if (ayah.globalNumber <= 0) null
                else QuranEditions.arabicAudioUrl(reciter, ayah.globalNumber)
            }
        }
    }

    fun playIndex(index: Int) {
        val s = state as? ReaderState.Ready
        val ayah = s?.arabic?.getOrNull(index)
        val url = if (ayah == null) null else audioUrlFor(index)
        if (ayah == null || url == null) {
            Toast.makeText(context, "Audio not available for this ayah.", Toast.LENGTH_SHORT).show()
            playingVerse = null
            playAll = false
            return
        }
        playingVerse = ayah.verse
        QuranAudio.play(
            url,
            onDone = {
                val cur = state as? ReaderState.Ready
                if (playAll && cur != null && index + 1 < cur.arabic.size) {
                    playIndex(index + 1)
                } else {
                    playingVerse = null
                    playAll = false
                }
            },
            onError = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                playingVerse = null
                playAll = false
            }
        )
    }

    fun stopAudio() {
        QuranAudio.stop()
        playingVerse = null
        playAll = false
    }

    val shownReciters = remember(reciters, reciter) {
        if (reciters.any { it.id == reciter }) reciters
        else listOf(Reciter(reciter, reciter)) + reciters
    }

    LaunchedEffect(surahId, lang, retryCount) {
        if (surah == null) {
            state = ReaderState.Error("Invalid surah number: $surahId")
            return@LaunchedEffect
        }
        state = ReaderState.Loading
        state = try {
            val arabic = QuranRepository.arabicAyahs(context, surahId, cache)
            if (arabic.isEmpty()) {
                ReaderState.Empty
            } else {
                val translation = QuranRepository.translationAyahs(context, surahId, lang, cache)
                val transliteration = try {
                    QuranRepository.transliterationAyahs(context, surahId, cache)
                } catch (_: Exception) { emptyList() }
                ReaderState.Ready(arabic, translation, transliteration)
            }
        } catch (e: Exception) {
            ReaderState.Error(e.message ?: "Failed to load the surah. Check your connection and try again.")
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
                title = surah?.arabicName ?: "…",
                subtitle = if (surah != null)
                    "${surah.englishName} • ${surah.ayahs} ayahs • Translation: ${langDisplayName(lang)}"
                else null
            )
            // ---- audio controls ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (playingVerse != null || playAll) stopAudio()
                            else {
                                playAll = true
                                playIndex(0)
                            }
                        },
                        enabled = state is ReaderState.Ready,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeenGold,
                            contentColor = DeenOnGold
                        )
                    ) {
                        Text(if (playingVerse != null || playAll) "Stop" else "Play full surah")
                    }
                    FilterChip(
                        selected = audioMode == "tilawat",
                        onClick = { audioMode = "tilawat" },
                        label = { Text("Tilawat") }
                    )
                    FilterChip(
                        selected = audioMode == "urdu",
                        onClick = { audioMode = "urdu" },
                        label = { Text("Urdu") }
                    )
                    FilterChip(
                        selected = audioMode == "bangla",
                        onClick = { audioMode = "bangla" },
                        label = { Text("Bangla") }
                    )
                }
                if (audioMode == "tilawat" && shownReciters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shownReciters, key = { it.id }) { r ->
                            FilterChip(
                                selected = r.id == reciter,
                                onClick = { scope.launch { prefs.setReciter(r.id) } },
                                label = { Text(r.name, maxLines = 1) }
                            )
                        }
                    }
                }
                // ---- pronunciation (transliteration) controls ----
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showPronunciation,
                        onClick = { showPronunciation = !showPronunciation },
                        label = { Text("Pronunciation") }
                    )
                    if (showPronunciation) {
                        val scripts = listOf("en" to "Roman", "hi" to "हिन्दी", "bn" to "বাংলা", "ur" to "اردو")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(scripts, key = { it.first }) { (code, label) ->
                                FilterChip(
                                    selected = pronScript == code,
                                    onClick = { scope.launch { prefs.setPronScript(code) } },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }
            when (val s = state) {
                ReaderState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = DeenGold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Loading ${surah?.englishName ?: "surah"}…",
                                color = DeenCream.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                ReaderState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No ayahs found for this surah.",
                            color = DeenCream.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                is ReaderState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = s.message,
                                color = DeenCream.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { retryCount++ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeenGold,
                                    contentColor = DeenOnGold
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is ReaderState.Ready -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(s.arabic, key = { _, ayah -> ayah.verse }) { index, ayah ->
                            AyahCard(
                                ayah = ayah,
                                translation = s.translation.getOrNull(index)?.text,
                                pronunciation = if (showPronunciation)
                                    s.transliteration.getOrNull(index)?.text
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { Pronunciation.localize(it, pronScript) }
                                else null,
                                isPlaying = playingVerse == ayah.verse,
                                onPlayAudio = {
                                    if (playingVerse == ayah.verse) stopAudio()
                                    else {
                                        playAll = false
                                        playIndex(index)
                                    }
                                },
                                onTafsir = if (nav != null) {
                                    { nav.navigate("tafsir/$surahId/${ayah.verse}") }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahCard(
    ayah: Ayah,
    translation: String?,
    pronunciation: String? = null,
    isPlaying: Boolean = false,
    onPlayAudio: (() -> Unit)? = null,
    onTafsir: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DeenSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, DeenGold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = ayah.verse.toArabicIndic(),
                        color = DeenGold,
                        fontFamily = ArabicFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (onPlayAudio != null) {
                    TextButton(onClick = onPlayAudio) {
                        Text(
                            if (isPlaying) "Playing…" else "▶ Play",
                            color = DeenGold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = ayah.text,
                    fontFamily = ArabicFont,
                    fontSize = 23.sp,
                    lineHeight = 40.sp,
                    color = DeenCream,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!pronunciation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pronunciation,
                    color = DeenGold.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (!translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DeenOutline.copy(alpha = 0.6f), thickness = 0.75.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = translation,
                    color = DeenCream.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp
                )
            }
            if (onTafsir != null) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = onTafsir) {
                    Text("Tafsir Ibn Kathir", color = DeenGold)
                }
            }
        }
    }
}
