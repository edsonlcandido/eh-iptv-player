package app.ehtudo.iptv.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ehtudo.domain.model.Provider

@Composable
internal fun SettingsContentPane(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    context: Context,
    screenLabels: SettingsScreenLabels,
    dialogState: SettingsScreenDialogState,
    providerState: SettingsProviderSectionState,
    onEditProvider: (Provider) -> Unit,
    onNavigateToParentalControl: (Long) -> Unit,
    onChooseRecordingFolder: () -> Unit,
    onUseUsbRecordingStorage: (() -> Unit)?,
    onCreateBackup: () -> Unit,
    onCreateBackupUsb: (() -> Unit)?,
    onRestoreBackupUsb: (() -> Unit)?,
    onShareBackup: () -> Unit,
    onViewCrashReport: () -> Unit,
    onShareCrashReport: () -> Unit,
    onDeleteCrashReport: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDriveSignIn: () -> Unit,
    onDriveSignOut: () -> Unit,
    onDrivePush: () -> Unit,
    onDrivePull: () -> Unit,
    onOpenUri: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 76.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = !uiState.isSyncing
    ) {
        if (dialogState.selectedCategory == 0) {
            providerSection(
                uiState = uiState,
                onEditProvider = onEditProvider,
                onNavigateToParentalControl = onNavigateToParentalControl,
                viewModel = viewModel,
                providerState = providerState
            )
        } else if (dialogState.selectedCategory == 1) {
            settingsPlaybackSection(
                uiState = uiState,
                viewModel = viewModel,
                lastSpeedTestLabel = screenLabels.lastSpeedTestLabel,
                lastSpeedTestSummary = screenLabels.lastSpeedTestSummary,
                speedTestRecommendationLabel = screenLabels.speedTestRecommendationLabel
            )
        } else if (dialogState.selectedCategory == 2) {
            settingsPrivacySection(
                uiState = uiState,
                viewModel = viewModel,
                onShowClearHistoryDialogChange = { dialogState.showClearHistoryDialog = it }
            )
        } else if (dialogState.selectedCategory == 3) {
            settingsAboutSection(
                uiState = uiState,
                context = context,
                buildVerificationLabel = screenLabels.buildVerificationLabel,
                onOpenUri = onOpenUri,
                onCheckForUpdates = viewModel::checkForAppUpdates,
                onInstallDownloadedUpdate = viewModel::installDownloadedUpdate,
                onDownloadLatestUpdate = viewModel::downloadLatestUpdate,
                onSetAutoCheckAppUpdates = viewModel::setAutoCheckAppUpdates,
                onSetAutoDownloadAppUpdates = viewModel::setAutoDownloadAppUpdates,
                onRefreshDownloadState = viewModel::refreshDownloadState,
                onViewCrashReport = onViewCrashReport,
                onShareCrashReport = onShareCrashReport,
                onDeleteCrashReport = onDeleteCrashReport
            )
        }
    }
}
