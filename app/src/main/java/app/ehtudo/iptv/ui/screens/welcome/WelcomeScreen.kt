package app.ehtudo.iptv.ui.screens.welcome

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import app.ehtudo.iptv.BuildConfig
import app.ehtudo.iptv.R
import app.ehtudo.iptv.ui.components.shell.StatusPill
import app.ehtudo.iptv.ui.design.AppColors
import app.ehtudo.iptv.ui.interaction.TvButton
import app.ehtudo.data.sync.SyncProgressBus
import app.ehtudo.domain.repository.ProviderRepository
import app.ehtudo.domain.sync.Section
import app.ehtudo.domain.sync.SyncProgress
import app.ehtudo.domain.usecase.M3uProviderSetupCommand
import app.ehtudo.domain.usecase.ValidateAndAddProvider
import app.ehtudo.domain.usecase.ValidateAndAddProviderResult
import app.ehtudo.domain.usecase.XtreamProviderSetupCommand
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

// ??? Hardcoded Xtream defaults ??????????????????????????????????????????????
// End-user onboarding: the Xtream server is fixed, so the welcome form only
// asks for the playlist username and password. The password field stays a
// normal text input (no VisualTransformation / no password mask) per spec.
private const val HARDCODED_XTREAM_URL = "http://dnstv.top/"
private const val DEFAULT_PROVIDER_NAME = "Eh! IPTV"

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val validateAndAddProvider: ValidateAndAddProvider,
    syncProgressBus: SyncProgressBus
) : ViewModel() {

    private val _hasProviders = MutableStateFlow<Boolean?>(null)
    val hasProviders: StateFlow<Boolean?> = _hasProviders.asStateFlow()

    private val acceptingProgress = MutableStateFlow(true)

    val syncProgress: StateFlow<SyncProgress?> =
        combine(syncProgressBus.flow, acceptingProgress) { progress, accept ->
            if (accept) progress else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun setUsername(value: String) {
        _username.value = value
        if (_error.value != null) _error.value = null
    }

    fun setPassword(value: String) {
        _password.value = value
        if (_error.value != null) _error.value = null
    }

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
    }

    fun loginXtream() {
        val username = _username.value.trim()
        val password = _password.value
        when {
            username.isBlank() -> {
                _error.value = WELCOME_USERNAME_REQUIRED
                return
            }
            password.isBlank() -> {
                _error.value = WELCOME_PASSWORD_REQUIRED
                return
            }
        }
        _error.value = null
        _isLoading.value = true
        viewModelScope.launch {
            val result = validateAndAddProvider.loginXtream(
                XtreamProviderSetupCommand(
                    serverUrl = HARDCODED_XTREAM_URL,
                    username = username,
                    password = password,
                    name = DEFAULT_PROVIDER_NAME,
                    xtreamFastSyncEnabled = true
                )
            )
            _isLoading.value = false
            _error.value = when (result) {
                is ValidateAndAddProviderResult.Success -> null
                is ValidateAndAddProviderResult.SavedWithWarning -> null
                is ValidateAndAddProviderResult.ValidationError -> result.message
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

    private companion object {
        const val WELCOME_USERNAME_REQUIRED = "Digite seu usuário"
        const val WELCOME_PASSWORD_REQUIRED = "Digite sua senha"
    }
}

@Composable
fun WelcomeScreen(
    onNavigateToHome: () -> Unit,
    startupReady: Boolean = true,
    onNavigateToSetup: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val hasProviders by viewModel.hasProviders.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()

    LaunchedEffect(hasProviders, isLoading, startupReady) {
        when {
            hasProviders == true && !isLoading && startupReady -> onNavigateToHome()
            else -> Unit
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
                username = username,
                password = password,
                error = error,
                onUsernameChange = viewModel::setUsername,
                onPasswordChange = viewModel::setPassword,
                onSave = viewModel::loginXtream,
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
    syncProgress: SyncProgress?,
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
            val pillLabel = if (syncProgress != null) {
                stringResource(sectionLabelRes(syncProgress.section))
            } else {
                stringResource(R.string.app_name)
            }
            val pillColor = if (syncProgress != null) {
                sectionColor(syncProgress.section)
            } else {
                AppColors.BrandMuted
            }
            StatusPill(
                label = pillLabel,
                containerColor = pillColor
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (syncProgress == null) {
                CircularProgressIndicator(color = AppColors.Brand)
                Spacer(modifier = Modifier.height(18.dp))
            }
            Text(
                text = stringResource(R.string.welcome_loading_title),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val subtitle = if (syncProgress != null && syncProgress.currentLabel.isNotBlank()) {
                syncProgress.currentLabel
            } else {
                stringResource(R.string.welcome_loading_subtitle)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary
            )
            if (syncProgress != null) {
                Spacer(modifier = Modifier.height(14.dp))
                if (syncProgress.total > 0) {
                    LinearProgressIndicator(
                        progress = { syncProgress.current.toFloat() / syncProgress.total.toFloat() },
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
                        syncProgress.itemsIndexed
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
    username: String,
    password: String,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_brand_title),
                style = TextStyle(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = { Text(stringResource(R.string.welcome_username_hint)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = AppColors.TextPrimary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Brand,
                    unfocusedBorderColor = AppColors.Outline,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedPlaceholderColor = AppColors.TextTertiary,
                    unfocusedPlaceholderColor = AppColors.TextTertiary,
                    cursorColor = AppColors.Brand
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { Text(stringResource(R.string.welcome_password_hint)) },
                singleLine = true,
                // Plain text input per spec — no VisualTransformation / password mask.
                textStyle = TextStyle(fontSize = 18.sp, color = AppColors.TextPrimary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Brand,
                    unfocusedBorderColor = AppColors.Outline,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedPlaceholderColor = AppColors.TextTertiary,
                    unfocusedPlaceholderColor = AppColors.TextTertiary,
                    cursorColor = AppColors.Brand
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error,
                    color = AppColors.Live,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TvButton(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.colors(
                    containerColor = AppColors.BrandStrong,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.welcome_save),
                    color = Color.White
                )
            }

            Text(
                text = stringResource(R.string.welcome_whatsapp_link),
                color = AppColors.Brand,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(EH_IPTV_WHATSAPP_URL))
                            )
                        }
                    }
                    .padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.welcome_manage_hint),
                color = AppColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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

private const val EH_IPTV_WHATSAPP_URL = "http://wa.me/+5511932055173"
