package com.glowup.ai.feature.insights

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.LockedCard
import com.glowup.ai.core.ui.ShimmerSkeleton
import com.glowup.ai.domain.model.DermExport

@Composable
fun DermExportScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: DermExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { GlowTopBar(title = "Dermatologist export", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ScreenState.Loading -> {
                    Column(modifier = Modifier.padding(GlowSpacing.md)) {
                        ShimmerSkeleton(height = 48.dp)
                        ShimmerSkeleton(height = 400.dp, modifier = Modifier.padding(top = GlowSpacing.sm))
                    }
                }

                ScreenState.Locked -> {
                    Box(modifier = Modifier.padding(GlowSpacing.md)) {
                        LockedCard(
                            title = "Dermatologist export is Premium",
                            body = "Generate a printable summary of your tracked history to bring to an appointment.",
                            onUnlock = onUpgrade,
                        )
                    }
                }

                is ScreenState.Error -> {
                    Box(modifier = Modifier.padding(GlowSpacing.md)) {
                        ErrorState(message = current.message, onRetry = viewModel::load)
                    }
                }

                is ScreenState.Empty -> {
                    Box(modifier = Modifier.padding(GlowSpacing.md)) {
                        EmptyState(title = current.title, body = current.body, ctaLabel = "Refresh", onCtaClick = viewModel::load)
                    }
                }

                is ScreenState.Content -> {
                    DermExportContent(current.value)
                }
            }
        }
    }
}

@Composable
private fun DermExportContent(export: DermExport) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        DisclaimerNote(
            modifier = Modifier.padding(GlowSpacing.md),
            text =
                export.disclaimer.ifBlank {
                    "This is a cosmetic tracking summary, not a diagnosis. Share it with a licensed dermatologist for clinical interpretation."
                },
        )
        Text(
            text = "${export.captureCount} captures · generated ${export.generatedAt}",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = LocalGlowColors.current.ink600,
            modifier = Modifier.padding(horizontal = GlowSpacing.md),
        )

        // `printable_html` is a plain HTML string, not a downloadable file — render it directly.
        AndroidView(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(GlowSpacing.md),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    webViewRef = this
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, export.printableHtml, "text/html", "utf-8", null)
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(GlowSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
        ) {
            GlowButton(
                modifier = Modifier.weight(1f),
                text = "Print",
                variant = GlowButtonVariant.Secondary,
                onClick = { webViewRef?.let { printExport(context, it) } },
            )
            GlowButton(
                modifier = Modifier.weight(1f),
                text = "Share",
                onClick = { shareExport(context, export.printableHtml) },
            )
        }
    }
}

private fun printExport(
    context: Context,
    webView: WebView,
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val jobName = "GlowUp AI export"
    val adapter = webView.createPrintDocumentAdapter(jobName)
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

private fun shareExport(
    context: Context,
    html: String,
) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_SUBJECT, "GlowUp AI dermatologist export")
            putExtra(Intent.EXTRA_TEXT, html)
        }
    context.startActivity(Intent.createChooser(intent, "Share export"))
}
