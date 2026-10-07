package com.streamvault.app.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.streamvault.app.BuildConfig
import com.streamvault.app.R
import com.streamvault.app.ui.components.shell.StatusPill
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.interaction.TvButton
import com.streamvault.data.sync.SyncProgressBus
import com.streamvault.data.sync.SyncProgressAggregate
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.sync.Section
import com.streamvault.domain.usecase.M3uProviderSetupCommand
import com.streamvault.domain.usecase.ValidateAndAddProvider
import com.streamvault.domain.usecase.ValidateAndAddProviderResult
import com.streamvault.domain.usecase.XtreamProviderSetupCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val validateAndAddProvider: ValidateAndAddProvider,
    syncProgressBus: SyncProgressBus
) : ViewModel() {

    private val _hasProviders = MutableStateFlow<Boolean?>(null)
    val hasProviders: StateFlow<Boolean?> = _hasProviders.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _syncCompleted = MutableStateFlow(false)
    /**
     * True once the initial post-login sync has finished (so the welcome screen can show
     * the sync progress until the user actually lands on a populated Home). The signal
     * flips on the first non-null → null transition of [syncProgress] after a provider
     * has been added. It never resets to false, so the LaunchedEffect only fires once.
     */
    val syncCompleted: StateFlow<Boolean> = _syncCompleted.asStateFlow()

    private val acceptingProgress = MutableStateFlow(true)

    val syncProgress: StateFlow<SyncProgressAggregate?> =
        combine(syncProgressBus.aggregate, acceptingProgress) { progress, accept ->
            if (accept) progress else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            maybeSeedDevProvider()
            providerRepository.getProviders()
                .map { it.isNotEmpty() }
                .collect { _hasProviders.value = it }
        }
        viewModelScope.launch {
            _hasProviders
                .filterNotNull()
                .first()
            acceptingProgress.value = false
        }
        // Track the sync progress so the welcome can show the sync screen until done.
        // SyncProgressBus.aggregate is null when no sync is active, non-null while syncing.
        // We flip syncCompleted on the first non-null → null transition (sync finished).
        viewModelScope.launch {
            var hasSeenProgress = false
            syncProgress.collect { progress ->
                if (progress != null) {
                    hasSeenProgress = true
                } else if (hasSeenProgress && _hasProviders.value == true) {
                    _syncCompleted.value = true
                }
            }
        }
    }

    fun setUsername(value: String) {
        _username.value = value
        if (_error.value != null) _error.value = null
    }

    fun setPassword(value: String) {
        _password.value = value
        if (_error.value != null) _error.value = null
    }

    fun loginXtream() {
        val username = _username.value.trim()
        val password = _password.value
        when {
            username.isBlank() -> { _error.value = USERNAME_REQUIRED; return }
            password.isBlank() -> { _error.value = PASSWORD_REQUIRED; return }
        }
        _error.value = null
        _isLoading.value = true
        viewModelScope.launch {
            val result = validateAndAddProvider.loginXtream(
                XtreamProviderSetupCommand(
                    serverUrl = BuildConfig.XTREAM_DEFAULT_URL,
                    username = username,
                    password = password,
                    name = BuildConfig.XTREAM_DEFAULT_PROVIDER_NAME,
                    xtreamFastSyncEnabled = true
                )
            )
            _isLoading.value = false
            _error.value = when (result) {
                is ValidateAndAddProviderResult.Success -> null
                is ValidateAndAddProviderResult.SavedWithWarning -> null
                is ValidateAndAddProviderResult.ValidationError -> result.message
                is ValidateAndAddProviderResult.TransportConsentRequired -> null
                is ValidateAndAddProviderResult.VerificationInconclusive -> result.message
                is ValidateAndAddProviderResult.Error -> result.message
            }
        }
    }

    private suspend fun maybeSeedDevProvider() {
        if (providerRepository.getProviders().first().isNotEmpty()) return

        val xtreamServer = BuildConfig.XTREAM_DEV_SERVER
        val xtreamUser = BuildConfig.XTREAM_DEV_USERNAME
        val xtreamPass = BuildConfig.XTREAM_DEV_PASSWORD
        if (xtreamServer.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank()) {
            validateAndAddProvider.loginXtream(
                XtreamProviderSetupCommand(
                    serverUrl = xtreamServer,
                    username = xtreamUser,
                    password = xtreamPass,
                    name = BuildConfig.XTREAM_DEV_NAME.ifBlank { "Dev (seeded)" },
                    xtreamFastSyncEnabled = true
                )
            )
            return
        }

        val m3uUrl = BuildConfig.M3U_DEV_URL
        if (m3uUrl.isNotBlank()) {
            validateAndAddProvider.addM3u(
                M3uProviderSetupCommand(
                    url = m3uUrl,
                    name = BuildConfig.M3U_DEV_NAME.ifBlank { "Dev M3U (seeded)" }
                )
            )
        }
    }

    companion object {
        // Sentinel keys for error messages; the actual localized strings live in strings.xml.
        // Using sentinel constants lets the composable read the right R.string.* via a small mapping.
        const val USERNAME_REQUIRED = "username_required"
        const val PASSWORD_REQUIRED = "password_required"
    }
}

@Composable
fun WelcomeScreen(
    onNavigateToHome: () -> Unit,
    startupReady: Boolean = true,
    @Suppress("UNUSED_PARAMETER") onNavigateToSetup: () -> Unit = {},
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val hasProviders by viewModel.hasProviders.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val syncCompleted by viewModel.syncCompleted.collectAsStateWithLifecycle()

    // Navigate to Home only after both: (a) a provider exists, (b) the initial sync has
    // completed. This keeps the WelcomeLoadingCard (with section progress) visible during
    // the post-login sync instead of flashing past it.
    LaunchedEffect(hasProviders, syncCompleted, startupReady) {
        if (hasProviders == true && syncCompleted && startupReady) {
            onNavigateToHome()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.22f),
                            AppColors.HeroTop,
                            AppColors.HeroBottom
                        )
                    )
                )
        )

        when {
            isLoading -> WelcomeLoadingCard(
                syncProgress = syncProgress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
            hasProviders == false -> WelcomeStartCard(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
            else -> WelcomeLoadingCard(
                syncProgress = syncProgress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
        }
    }
}

@Composable
private fun WelcomeLoadingCard(
    syncProgress: SyncProgressAggregate?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val representativeProgress = syncProgress?.representative?.progress
            val pillLabel = if (representativeProgress != null) {
                stringResource(sectionLabelRes(representativeProgress.section))
            } else {
                stringResource(R.string.app_name)
            }
            val pillColor = if (representativeProgress != null) {
                sectionColor(representativeProgress.section)
            } else {
                AppColors.BrandMuted
            }
            StatusPill(
                label = pillLabel,
                containerColor = pillColor
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (representativeProgress == null) {
                CircularProgressIndicator(color = AppColors.Brand)
                Spacer(modifier = Modifier.height(18.dp))
            }
            Text(
                text = stringResource(R.string.welcome_loading_title),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val subtitle = if (representativeProgress != null && representativeProgress.currentLabel.isNotBlank()) {
                representativeProgress.currentLabel
            } else {
                stringResource(R.string.welcome_loading_subtitle)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary
            )
            if (representativeProgress != null) {
                Spacer(modifier = Modifier.height(14.dp))
                if (representativeProgress.total > 0) {
                    LinearProgressIndicator(
                        progress = { representativeProgress.current.toFloat() / representativeProgress.total.toFloat() },
                        modifier = Modifier.width(260.dp),
                        color = AppColors.Brand,
                        trackColor = AppColors.BrandMuted
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.width(260.dp),
                        color = AppColors.Brand,
                        trackColor = AppColors.BrandMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        R.string.sync_items_indexed_format,
                        representativeProgress.itemsIndexed
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WelcomeStartCard(
    viewModel: WelcomeViewModel,
    modifier: Modifier = Modifier
) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorText = when (error) {
        WelcomeViewModel.USERNAME_REQUIRED -> stringResource(R.string.welcome_username_required)
        WelcomeViewModel.PASSWORD_REQUIRED -> stringResource(R.string.welcome_password_required)
        else -> error
    }
    Surface(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp, vertical = 34.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusPill(
                label = stringResource(R.string.welcome_brand_title),
                containerColor = AppColors.BrandMuted
            )
            OutlinedTextField(
                value = username,
                onValueChange = viewModel::setUsername,
                label = { Text(stringResource(R.string.welcome_username_hint)) },
                singleLine = true,
                enabled = !isLoading,
                textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.TextPrimary),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    disabledTextColor = AppColors.TextSecondary,
                    focusedBorderColor = AppColors.Brand,
                    unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.45f),
                    focusedLabelColor = AppColors.Brand,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    cursorColor = AppColors.Brand
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = viewModel::setPassword,
                label = { Text(stringResource(R.string.welcome_password_hint)) },
                singleLine = true,
                enabled = !isLoading,
                // No PasswordVisualTransformation on purpose: single-tenant reseller, customers
                // paste the cred they got from support. See docs/skill/simplify-welcome-onboarding.md.
                textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.TextPrimary),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    disabledTextColor = AppColors.TextSecondary,
                    focusedBorderColor = AppColors.Brand,
                    unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.45f),
                    focusedLabelColor = AppColors.Brand,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    cursorColor = AppColors.Brand
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Live,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TvButton(
                onClick = viewModel::loginXtream,
                enabled = !isLoading,
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = AppColors.Brand,
                    contentColor = Color.White,
                    focusedContainerColor = AppColors.BrandStrong,
                    focusedContentColor = Color.White,
                    pressedContainerColor = AppColors.BrandStrong,
                    pressedContentColor = Color.White,
                    disabledContainerColor = AppColors.BrandMuted,
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.welcome_save),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun sectionColor(section: Section): Color = when (section) {
    Section.LIVE -> AppColors.Brand
    Section.VOD -> AppColors.Success
    Section.SERIES -> AppColors.Warning
}

private fun sectionLabelRes(section: Section): Int = when (section) {
    Section.LIVE -> R.string.sync_section_live
    Section.VOD -> R.string.sync_section_vod
    Section.SERIES -> R.string.sync_section_series
}
