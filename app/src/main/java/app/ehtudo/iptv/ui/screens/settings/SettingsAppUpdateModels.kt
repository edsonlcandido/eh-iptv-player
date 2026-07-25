package app.ehtudo.iptv.ui.screens.settings

import app.ehtudo.iptv.update.AppUpdateActionState
import app.ehtudo.iptv.update.AppUpdateDownloadState
import app.ehtudo.iptv.update.AppUpdateDownloadStatus
import app.ehtudo.iptv.update.GitHubReleaseInfo
import app.ehtudo.iptv.update.isRemoteVersionNewer
import app.ehtudo.iptv.update.latestAppUpdateAction

data class AppUpdateUiModel(
    val latestVersionName: String? = null,
    val latestVersionCode: Int? = null,
    val releaseUrl: String? = null,
    val downloadUrl: String? = null,
    val downloadSha256: String? = null,
    val releaseNotes: String = "",
    val publishedAt: String? = null,
    val isUpdateAvailable: Boolean = false,
    val lastCheckedAt: Long? = null,
    val errorMessage: String? = null,
    val downloadStatus: AppUpdateDownloadStatus = AppUpdateDownloadStatus.Idle,
    val downloadedVersionName: String? = null,
    val installPermissionRequired: Boolean = false
)

internal fun AppUpdateUiModel.toReleaseInfoOrNull(): GitHubReleaseInfo? {
    val versionName = latestVersionName ?: return null
    val releaseUrl = releaseUrl ?: return null
    return GitHubReleaseInfo(
        versionName = versionName,
        versionCode = latestVersionCode,
        releaseUrl = releaseUrl,
        downloadUrl = downloadUrl,
        downloadSha256 = downloadSha256,
        releaseNotes = releaseNotes,
        publishedAt = publishedAt
    )
}

internal fun AppUpdateUiModel.withDownloadState(downloadState: AppUpdateDownloadState): AppUpdateUiModel {
    return copy(
        downloadStatus = downloadState.status,
        downloadedVersionName = downloadState.versionName,
        installPermissionRequired = downloadState.installPermissionRequired
    )
}

internal fun AppUpdateUiModel.toDownloadState(): AppUpdateDownloadState {
    return AppUpdateDownloadState(
        status = downloadStatus,
        versionName = downloadedVersionName,
        installPermissionRequired = installPermissionRequired
    )
}

internal fun AppUpdateUiModel.latestActionState(): AppUpdateActionState {
    return latestAppUpdateAction(
        latestVersionName = latestVersionName,
        downloadUrl = downloadUrl,
        isUpdateAvailable = isUpdateAvailable,
        downloadState = toDownloadState()
    )
}

internal fun SettingsPreferenceSnapshot.toCachedAppUpdateUiModel(): AppUpdateUiModel {
    val versionName = cachedAppUpdateVersionName
    return AppUpdateUiModel(
        latestVersionName = versionName,
        latestVersionCode = cachedAppUpdateVersionCode,
        releaseUrl = cachedAppUpdateReleaseUrl,
        downloadUrl = cachedAppUpdateDownloadUrl,
        downloadSha256 = cachedAppUpdateDownloadSha256,
        releaseNotes = cachedAppUpdateReleaseNotes,
        publishedAt = cachedAppUpdatePublishedAt,
        isUpdateAvailable = versionName?.let {
            isRemoteVersionNewer(cachedAppUpdateVersionCode, it, cachedAppUpdatePublishedAt)
        } ?: false,
        lastCheckedAt = lastAppUpdateCheckAt
    )
}
