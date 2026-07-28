package app.ehtudo.iptv.ui.screens.settings

import app.ehtudo.iptv.ui.model.LiveTvChannelMode
import app.ehtudo.iptv.ui.model.LiveTvQuickFilterVisibilityMode
import app.ehtudo.iptv.ui.model.VodViewMode
import app.ehtudo.domain.manager.BackupImportPlan
import app.ehtudo.domain.manager.BackupPreview
import app.ehtudo.domain.manager.DriveAuthState
import app.ehtudo.domain.manager.DriveSignInRequest
import app.ehtudo.domain.manager.DriveSyncStatus
import app.ehtudo.domain.manager.ProviderCredentials
import app.ehtudo.domain.model.ActiveLiveSource
import app.ehtudo.domain.model.AppHomeDashboardShelf
import app.ehtudo.domain.model.AppLandingDestination
import app.ehtudo.domain.model.AppTopLevelDestination
import app.ehtudo.domain.model.AppTimeFormat
import app.ehtudo.domain.model.AudioOutputPreference
import app.ehtudo.domain.model.Category
import app.ehtudo.domain.model.CategorySortMode
import app.ehtudo.domain.model.ChannelNumberingMode
import app.ehtudo.domain.model.CombinedM3uProfile
import app.ehtudo.domain.model.ContentType
import app.ehtudo.domain.model.DecoderMode
import app.ehtudo.domain.model.EpgResolutionSummary
import app.ehtudo.domain.model.GroupedChannelLabelMode
import app.ehtudo.domain.model.LiveChannelGroupingMode
import app.ehtudo.domain.model.LiveVariantPreferenceMode
import app.ehtudo.domain.model.PlaybackBufferMode
import app.ehtudo.domain.model.VodDuplicateHandlingMode
import app.ehtudo.domain.model.VodHttpProtocolMode
import app.ehtudo.domain.model.ExternalPlaybackMode
import app.ehtudo.domain.model.PlayerSurfaceMode
import app.ehtudo.domain.model.Provider
import app.ehtudo.domain.model.RecordingItem
import app.ehtudo.domain.model.RecordingStorageState
import app.ehtudo.domain.model.RemoteShortcutPreferences
import app.ehtudo.domain.model.TimeshiftBackendPreference
import app.ehtudo.domain.model.VodVariantPreferenceMode

data class CrashReportUiModel(
    val timestamp: String = "",
    val exception: String = "",
    val fileName: String = "",
    val content: String = ""
) {
    val hasReport: Boolean
        get() = content.isNotBlank()
}

data class SettingsUiState(
    val providers: List<Provider> = emptyList(),
    val quickXtreamUsername: String = "",
    val quickXtreamPassword: String = "",
    val isAddingQuickXtream: Boolean = false,
    val quickXtreamError: String? = null,
    val combinedProfiles: List<CombinedM3uProfile> = emptyList(),
    val availableM3uProviders: List<Provider> = emptyList(),
    val activeProviderId: Long? = null,
    val activeLiveSource: ActiveLiveSource? = null,
    val isSyncing: Boolean = false,
    val syncProgress: String? = null,
    val syncingProviderName: String? = null,
    val syncStartedAt: Long = 0L,
    val syncSectionLabel: String? = null,
    val syncCanCancel: Boolean = false,
    val userMessage: String? = null,
    val syncWarningsByProvider: Map<Long, List<String>> = emptyMap(),
    val xtreamLiveOnboardingPhaseByProvider: Map<Long, String> = emptyMap(),
    val xtreamLiveOnboardingByProvider: Map<Long, XtreamLiveOnboardingUiModel> = emptyMap(),
    val xtreamIndexSectionStatusByProvider: Map<Long, Map<String, ProviderCatalogCountStatus>> = emptyMap(),
    val diagnosticsByProvider: Map<Long, ProviderDiagnosticsUiModel> = emptyMap(),
    val databaseMaintenance: DatabaseMaintenanceUiModel? = null,
    val parentalControlLevel: Int = 0,
    val hasParentalPin: Boolean = false,
    val adultContentEnabled: Boolean = false,
    val appLanguage: String = "system",
    val appLandingDestination: AppLandingDestination = AppLandingDestination.LIVE_TV,
    val appTopLevelDestinations: List<AppTopLevelDestination> = AppTopLevelDestination.defaultOrder,
    val appHomeDashboardShelves: List<AppHomeDashboardShelf> = AppHomeDashboardShelf.defaultOrder,
    val appTimeFormat: AppTimeFormat = AppTimeFormat.SYSTEM,
    val preferredAudioLanguage: String = "auto",
    val playerMediaSessionEnabled: Boolean = true,
    val playerFastRetryOnTransientFailures: Boolean = false,
    val playerAudioDecoderMode: DecoderMode = DecoderMode.AUTO,
    val playerVideoDecoderMode: DecoderMode = DecoderMode.AUTO,
    val playerPlaybackBufferMode: PlaybackBufferMode = PlaybackBufferMode.AUTO,
    val playerAudioOutputPreference: AudioOutputPreference = AudioOutputPreference.AUTO,
    val playerCompatibilityMemoryEnabled: Boolean = true,
    val playerSurfaceMode: PlayerSurfaceMode = PlayerSurfaceMode.AUTO,
    val playerVodHttpProtocolMode: VodHttpProtocolMode = VodHttpProtocolMode.COMPATIBILITY_HTTP1,
    val playerPlaybackSpeed: Float = 1f,
    val playerExternalPlaybackMode: ExternalPlaybackMode = ExternalPlaybackMode.INTERNAL_PLAYER,
    val playerAudioVideoSyncEnabled: Boolean = false,
    val playerAudioVideoOffsetMs: Int = 0,
    val centerTwoSlotMultiviewLayout: Boolean = false,
    val multiViewRespectProviderConnectionLimit: Boolean = true,
    val playerControlsTimeoutSeconds: Int = 5,
    val playerLiveOverlayTimeoutSeconds: Int = 4,
    val playerNoticeTimeoutSeconds: Int = 6,
    val playerDiagnosticsTimeoutSeconds: Int = 15,
    val subtitleTextScale: Float = 1f,
    val subtitleTextColor: Int = 0xFFFFFFFF.toInt(),
    val subtitleBackgroundColor: Int = 0x80000000.toInt(),
    val playerLiveTranslationEnabled: Boolean = false,
    val playerLiveTranslationEndpoint: String = "http://10.0.2.2:8765",
    val wifiMaxVideoHeight: Int? = null,
    val ethernetMaxVideoHeight: Int? = null,
    val playerTimeshiftEnabled: Boolean = false,
    val playerTimeshiftDepthMinutes: Int = 30,
    val playerTimeshiftBackend: TimeshiftBackendPreference = TimeshiftBackendPreference.AUTOMATIC,
    val defaultStopPlaybackTimerMinutes: Int = 0,
    val defaultIdleStandbyTimerMinutes: Int = 0,
    val lastSpeedTest: InternetSpeedTestUiModel? = null,
    val isRunningInternetSpeedTest: Boolean = false,
    val isDeletingProvider: Boolean = false,
    val deleteProviderProgressMessage: String? = null,
    val deleteProviderProgressFraction: Float? = null,
    val isImportingBackup: Boolean = false,
    val backupPreview: BackupPreview? = null,
    val pendingBackupUri: String? = null,
    val backupImportPlan: BackupImportPlan = BackupImportPlan(),
    // --- Drive sync (M2) ---
    val driveAuthState: DriveAuthState = DriveAuthState.SignedOut,
    val driveSyncStatus: DriveSyncStatus = DriveSyncStatus(),
    val driveLastPushAt: Long? = null,
    val driveLastPullAt: Long? = null,
    val drivePendingSignIn: DriveSignInRequest? = null,
    val driveIsBusy: Boolean = false,
    // M3 — credentials downloaded by pullBackup, waiting to be applied
    // to providers once the import confirm completes.
    val pendingDriveCredentials: List<ProviderCredentials>? = null,
    val recordingItems: List<RecordingItem> = emptyList(),
    val recordingStorageState: RecordingStorageState = RecordingStorageState(),
    val wifiOnlyRecording: Boolean = false,
    val recordingPaddingBeforeMinutes: Int = 0,
    val recordingPaddingAfterMinutes: Int = 0,
    val isIncognitoMode: Boolean = false,
    val useXtreamTextClassification: Boolean = true,
    val xtreamBase64TextCompatibility: Boolean = false,
    val liveTvChannelMode: LiveTvChannelMode = LiveTvChannelMode.PRO,
    val showLiveSourceSwitcher: Boolean = false,
    val showFavoritesCategory: Boolean = true,
    val showAllChannelsCategory: Boolean = true,
    val showRecentChannelsCategory: Boolean = true,
    val remoteShortcutPreferences: RemoteShortcutPreferences = RemoteShortcutPreferences(),
    val liveTvCategoryFilters: List<String> = emptyList(),
    val liveTvQuickFilterVisibilityMode: LiveTvQuickFilterVisibilityMode = LiveTvQuickFilterVisibilityMode.ALWAYS_VISIBLE,
    val hideDecorativeLiveRows: Boolean = true,
    val liveChannelNumberingMode: ChannelNumberingMode = ChannelNumberingMode.GROUP,
    val liveChannelGroupingMode: LiveChannelGroupingMode = LiveChannelGroupingMode.RAW_VARIANTS,
    val groupedChannelLabelMode: GroupedChannelLabelMode = GroupedChannelLabelMode.HYBRID,
    val liveVariantPreferenceMode: LiveVariantPreferenceMode = LiveVariantPreferenceMode.BALANCED,
    val vodViewMode: VodViewMode = VodViewMode.MODERN,
    val vodInfiniteScroll: Boolean = true,
    val vodDuplicateHandlingMode: VodDuplicateHandlingMode = VodDuplicateHandlingMode.SHOW_ALL,
    val vodVariantPreferenceMode: VodVariantPreferenceMode = VodVariantPreferenceMode.BALANCED,
    val guideDefaultCategoryId: Long = app.ehtudo.domain.model.VirtualCategoryIds.FAVORITES,
    val guideDefaultCategoryOptions: List<Category> = emptyList(),
    val preventStandbyDuringPlayback: Boolean = true,
    val zapAutoRevert: Boolean = true,
    val autoPlayNextEpisode: Boolean = true,
    val categorySortModes: Map<ContentType, CategorySortMode> = emptyMap(),
    val hiddenCategories: List<Category> = emptyList(),
    val epgSources: List<app.ehtudo.domain.model.EpgSource> = emptyList(),
    val epgSourceAssignments: Map<Long, List<app.ehtudo.domain.model.ProviderEpgSourceAssignment>> = emptyMap(),
    val epgResolutionSummaries: Map<Long, EpgResolutionSummary> = emptyMap(),
    val refreshingEpgSourceIds: Set<Long> = emptySet(),
    val epgPendingDeleteSourceId: Long? = null,
    val epgTimeShiftMinutesByProvider: Map<Long, Int> = emptyMap(),
    val autoCheckAppUpdates: Boolean = true,
    val autoDownloadAppUpdates: Boolean = false,
    val isCheckingForUpdates: Boolean = false,
    val appUpdate: AppUpdateUiModel = AppUpdateUiModel(),
    val crashReport: CrashReportUiModel = CrashReportUiModel(),
    val viewedCrashReport: CrashReportUiModel? = null
)
