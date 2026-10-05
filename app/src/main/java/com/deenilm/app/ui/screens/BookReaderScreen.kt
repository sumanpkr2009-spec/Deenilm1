package com.deenilm.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.deenilm.app.data.BookCatalog
import com.deenilm.app.data.BookEntry
import com.deenilm.app.ui.components.EightPointStarBackground
import com.deenilm.app.ui.theme.DeenBg
import com.deenilm.app.ui.theme.DeenCream
import com.deenilm.app.ui.theme.DeenGold
import com.deenilm.app.ui.theme.DeenOnGold
import com.deenilm.app.ui.theme.DeenSurface
import java.net.URLEncoder

@Composable
fun BookReaderScreen(index: Int, navController: NavController) {
    val context = LocalContext.current
    var book by remember { mutableStateOf<BookEntry?>(null) }
    var invalid by remember { mutableStateOf(false) }

    LaunchedEffect(index) {
        try {
            val b = BookCatalog.load(context).getOrNull(index)
            if (b == null) invalid = true else book = b
        } catch (e: Exception) {
            invalid = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeenBg)) {
        EightPointStarBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar: back + title + download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeenSurface)
                    .padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeenGold
                    )
                }
                Text(
                    text = book?.title?.replace('-', ' ')?.replace('_', ' ') ?: "Book",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeenCream,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (book != null) {
                    Button(
                        onClick = { downloadPdf(context, book!!) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeenGold,
                            contentColor = DeenOnGold
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(text = "Download", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            when {
                invalid -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "This book could not be opened.",
                                color = DeenCream,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeenGold,
                                    contentColor = DeenOnGold
                                )
                            ) {
                                Text(text = "Back to library", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                book == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DeenGold)
                    }
                }
                else -> BookWebView(book = book!!)
            }
        }
    }
}

@Composable
private fun BookWebView(book: BookEntry) {
    var webError by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    val viewerUrl = remember(book.url) {
        "https://drive.google.com/viewerng/viewer?embedded=true&url=" +
            URLEncoder.encode(book.url, "UTF-8")
    }
    val webView = remember {
        mutableStateOf<WebView?>(null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (webError) {
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
                        text = "The preview failed to load.\nTry again, or use Download to read the PDF offline.",
                        color = DeenCream,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            webError = false
                            webView.value?.loadUrl(viewerUrl)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeenGold,
                            contentColor = DeenOnGold
                        )
                    ) {
                        Text(text = "Retry preview", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                progress = 100
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame) {
                                    webError = true
                                }
                            }
                        }
                        loadUrl(viewerUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                webView.value = view
            }
            if (progress < 100) {
                LinearProgressIndicator(
                    progress = progress.coerceIn(0, 100) / 100f,
                    color = DeenGold,
                    trackColor = DeenBg,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Enqueues a DownloadManager request for the book's direct Drive URL. */
private fun downloadPdf(context: Context, book: BookEntry) {
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val safeName = book.title
            .replace(Regex("[^A-Za-z0-9\\u0980-\\u09FF._-]"), "_")
            .take(100) + ".pdf"
        val request = DownloadManager.Request(Uri.parse(book.url))
            .setTitle(book.title)
            .setDescription("Deen Ilm Books")
            .setMimeType("application/pdf")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
        try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
        } catch (e: Exception) {
            // fall back to the manager's default destination
        }
        dm.enqueue(request)
        Toast.makeText(context, "Download started: ${book.title}", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
