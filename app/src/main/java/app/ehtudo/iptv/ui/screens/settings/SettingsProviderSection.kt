package app.ehtudo.iptv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import app.ehtudo.iptv.R
import app.ehtudo.iptv.ui.design.AppColors
import app.ehtudo.iptv.ui.interaction.TvButton
import app.ehtudo.iptv.ui.theme.OnSurfaceDim
import app.ehtudo.domain.model.Provider
import app.ehtudo.domain.model.ProviderType

internal fun LazyListScope.providerSection(
    uiState: SettingsUiState,
    onEditProvider: (Provider) -> Unit,
    onNavigateToParentalControl: (Long) -> Unit,
    viewModel: SettingsViewModel,
    providerState: SettingsProviderSectionState
) {
    if (uiState.providers.isEmpty()) {
        item {
            QuickXtreamProviderCard(
                username = uiState.quickXtreamUsername,
                password = uiState.quickXtreamPassword,
                isLoading = uiState.isAddingQuickXtream,
                error = uiState.quickXtreamError,
                onUsernameChange = viewModel::setQuickXtreamUsername,
                onPasswordChange = viewModel::setQuickXtreamPassword,
                onSave = viewModel::addQuickXtreamProvider
            )
        }
    } else {
        item {
            var selectedProviderId by rememberSaveable(uiState.providers, uiState.activeProviderId) {
                mutableStateOf(uiState.activeProviderId ?: uiState.providers.first().id)
            }
            LaunchedEffect(uiState.providers, uiState.activeProviderId) {
                val availableIds = uiState.providers.map { it.id }.toSet()
                if (selectedProviderId !in availableIds) {
                    selectedProviderId = uiState.activeProviderId ?: uiState.providers.first().id
                }
            }
            val selectedProvider = uiState.providers.firstOrNull { it.id == selectedProviderId }
                ?: uiState.providers.first()

            Text(
                text = stringResource(R.string.settings_provider_selector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 14.dp)
            ) {
                items(uiState.providers, key = { it.id }) { provider ->
                    ProviderSelectorTab(
                        provider = provider,
                        isSelected = provider.id == selectedProvider.id,
                        isActive = provider.id == uiState.activeProviderId,
                        onClick = { selectedProviderId = provider.id }
                    )
                }
            }
            ProviderSettingsCard(
                provider = selectedProvider,
                isActive = selectedProvider.id == uiState.activeProviderId,
                isSyncing = uiState.isSyncing,
                xtreamLiveOnboardingPhase = uiState.xtreamLiveOnboardingPhaseByProvider[selectedProvider.id],
                xtreamLiveOnboarding = uiState.xtreamLiveOnboardingByProvider[selectedProvider.id],
                xtreamIndexSectionStatuses = uiState.xtreamIndexSectionStatusByProvider[selectedProvider.id].orEmpty(),
                diagnostics = uiState.diagnosticsByProvider[selectedProvider.id],
                databaseMaintenance = uiState.databaseMaintenance,
                syncWarnings = uiState.syncWarningsByProvider[selectedProvider.id].orEmpty(),
                onRetryWarningAction = { action -> viewModel.retryWarningAction(selectedProvider.id, action) },
                onConnect = { viewModel.setActiveProvider(selectedProvider.id) },
                onRefresh = {
                    providerState.pendingSyncProviderId = selectedProvider.id
                    providerState.customSyncSelections = buildSet {
                        add(ProviderSyncSelection.TV)
                        add(ProviderSyncSelection.MOVIES)
                        add(ProviderSyncSelection.EPG)
                        if (selectedProvider.type == ProviderType.XTREAM_CODES) {
                            add(ProviderSyncSelection.SERIES)
                        }
                    }
                    providerState.showProviderSyncDialog = true
                },
                onDelete = { providerState.pendingDeleteProviderId = selectedProvider.id },
                onEdit = { onEditProvider(selectedProvider) },
                onParentalControl = { onNavigateToParentalControl(selectedProvider.id) },
                onToggleM3uVodClassification = { enabled ->
                    viewModel.setM3uVodClassificationEnabled(selectedProvider.id, enabled)
                },
                onRefreshM3uClassification = {
                    viewModel.refreshProviderClassification(selectedProvider.id)
                }
            )
        }
    }
}

@Composable
private fun QuickXtreamProviderCard(
    username: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            MaterialText(
                text = stringResource(R.string.welcome_brand_title),
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            MaterialText(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                enabled = !isLoading,
                placeholder = { MaterialText(stringResource(R.string.welcome_username_hint)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = AppColors.TextPrimary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = quickXtreamTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                enabled = !isLoading,
                placeholder = { MaterialText(stringResource(R.string.welcome_password_hint)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = AppColors.TextPrimary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                colors = quickXtreamTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                MaterialText(
                    text = error,
                    color = AppColors.Live,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TvButton(
                onClick = onSave,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.colors(
                    containerColor = AppColors.BrandStrong,
                    contentColor = Color.White
                )
            ) {
                MaterialText(
                    text = if (isLoading) {
                        stringResource(R.string.welcome_loading_title)
                    } else {
                        stringResource(R.string.welcome_save)
                    },
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun quickXtreamTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Brand,
    unfocusedBorderColor = AppColors.Outline,
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedPlaceholderColor = AppColors.TextTertiary,
    unfocusedPlaceholderColor = AppColors.TextTertiary,
    cursorColor = AppColors.Brand
)
