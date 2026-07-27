package app.ehtudo.iptv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ehtudo.iptv.R
import app.ehtudo.domain.model.LiveStreamFormatMode

internal fun LazyListScope.settingsPlaybackSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    lastSpeedTestLabel: String,
    lastSpeedTestSummary: String,
    speedTestRecommendationLabel: String
) {
    item {
        var showLiveStreamFormatDialog by rememberSaveable { mutableStateOf(false) }
        val liveStreamFormatMode by viewModel.playerLiveStreamFormatMode.collectAsStateWithLifecycle()
        val liveStreamFormatOptions = remember {
            listOf(
                LiveStreamFormatMode.AUTO,
                LiveStreamFormatMode.HLS,
                LiveStreamFormatMode.MPEG_TS
            )
        }
        if (showLiveStreamFormatDialog) {
            PremiumSelectionDialog(
                title = stringResource(R.string.settings_live_stream_format),
                onDismiss = { showLiveStreamFormatDialog = false }
            ) {
                liveStreamFormatOptions.forEachIndexed { index, mode ->
                    LevelOption(
                        level = index,
                        text = formatLiveStreamFormatModeLabel(mode),
                        currentLevel = if (liveStreamFormatMode == mode) index else -1,
                        onSelect = {
                            viewModel.setPlayerLiveStreamFormatMode(mode)
                            showLiveStreamFormatDialog = false
                        }
                    )
                }
            }
        }
        ClickableSettingsRow(
            label = stringResource(R.string.settings_live_stream_format),
            value = formatLiveStreamFormatModeLabel(liveStreamFormatMode),
            onClick = { showLiveStreamFormatDialog = true }
        )
    }

    item {
        InternetSpeedTestCard(
            valueLabel = lastSpeedTestLabel,
            summary = lastSpeedTestSummary,
            recommendationLabel = speedTestRecommendationLabel,
            isRunning = uiState.isRunningInternetSpeedTest,
            canApplyRecommendation = uiState.lastSpeedTest != null,
            onRunTest = viewModel::runInternetSpeedTest,
            onApplyWifi = viewModel::applySpeedTestRecommendationToWifi,
            onApplyEthernet = viewModel::applySpeedTestRecommendationToEthernet
        )
    }
}
