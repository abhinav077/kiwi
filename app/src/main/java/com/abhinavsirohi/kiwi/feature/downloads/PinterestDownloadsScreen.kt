package com.abhinavsirohi.kiwi.feature.downloads

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.ui.theme.KiwiButter
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiForest
import com.abhinavsirohi.kiwi.ui.theme.KiwiPeach
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing

@Composable
fun PinterestDownloadsRoute(
    initialSharedText: String?,
    onSharedTextConsumed: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val downloadsViewModel: PinterestDownloadsViewModel = viewModel(
        factory = PinterestDownloadsViewModel.Factory(
            extractor = remember { PublicPinterestExtractor() },
            downloadGateway = remember { AndroidPinterestDownloadGateway(context.applicationContext) },
        ),
    )
    val state by downloadsViewModel.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) downloadsViewModel.download() else downloadsViewModel.storagePermissionDenied()
    }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            downloadsViewModel.acceptSharedText(initialSharedText)
            onSharedTextConsumed()
        }
    }

    PinterestDownloadsScreen(
        state = state,
        onUrlChanged = downloadsViewModel::updateUrl,
        onPaste = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            downloadsViewModel.updateUrl(text)
        },
        onPreview = downloadsViewModel::preview,
        onDownload = {
            if (
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                downloadsViewModel.download()
            }
        },
        onBack = onBack,
    )
}

@Composable
fun PinterestDownloadsScreen(
    state: PinterestDownloadsUiState,
    onUrlChanged: (String) -> Unit,
    onPaste: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onBack: () -> Unit,
) {
    KiwiBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(DOWNLOADS_LIST_TEST_TAG),
            contentPadding = PaddingValues(
                start = KiwiSpacing.lg,
                end = KiwiSpacing.lg,
                top = KiwiSpacing.md,
                bottom = KiwiSpacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.xl),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack, modifier = Modifier.size(width = 72.dp, height = 48.dp)) {
                        Text("Back")
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Downloads", style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Text(
                    "Keep an inspiring find close",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(KiwiSpacing.xs))
                Text(
                    "Preview a public Pinterest video, then let Android save it to your device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = KiwiPeach.copy(alpha = 0.72f)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(KiwiSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md),
                    ) {
                        OutlinedTextField(
                            value = state.url,
                            onValueChange = onUrlChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Pinterest Pin link") },
                            supportingText = { Text("Public pinterest.com and pin.it links only") },
                            singleLine = true,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.sm),
                        ) {
                            OutlinedButton(onClick = onPaste, modifier = Modifier.weight(1f)) { Text("Paste link") }
                            Button(
                                onClick = onPreview,
                                enabled = !state.loading,
                                modifier = Modifier.weight(1f),
                            ) { Text("Preview") }
                        }
                    }
                }
            }
            if (state.loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Opening public Pinterest preview"
                        },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.size(KiwiSpacing.md))
                        Text("Opening the public Pin…")
                    }
                }
            }
            state.media?.let { media ->
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(initialScale = 0.96f),
                    ) {
                        PinterestPreview(media, state.downloadQueued, onDownload)
                    }
                }
            }
            state.message?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.downloadQueued) KiwiButter.copy(alpha = 0.78f)
                            else MaterialTheme.colorScheme.surface,
                        ),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text(
                            message,
                            modifier = Modifier.fillMaxWidth().padding(KiwiSpacing.lg),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            item {
                Text(
                    "Save only content you own or have permission to use. Kiwi reads public page metadata without signing in to Pinterest, and never sends downloaded videos to Supabase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PinterestPreview(
    media: PinterestMedia,
    downloadQueued: Boolean,
    onDownload: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(KiwiSpacing.lg)) {
            val image: @Composable (Modifier) -> Unit = { modifier ->
                if (media.previewUrl != null) {
                    AsyncImage(
                        model = media.previewUrl,
                        contentDescription = "Preview image for ${media.title}",
                        modifier = modifier.clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = modifier.clip(RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Video preview", color = KiwiCharcoal)
                    }
                }
            }
            val details: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier, verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md)) {
                    Text(
                        media.title,
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Source: ${media.attribution}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Video · ${media.mimeType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onDownload,
                        enabled = !downloadQueued,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(if (downloadQueued) "Download started" else "Save to device")
                    }
                }
            }
            if (maxWidth >= 600.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.lg)) {
                    image(Modifier.weight(1f).aspectRatio(1f).then(Modifier))
                    details(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.lg)) {
                    image(Modifier.fillMaxWidth().aspectRatio(4f / 3f))
                    details(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

internal const val DOWNLOADS_LIST_TEST_TAG = "pinterest-downloads-list"
