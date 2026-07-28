package app.ehtudo.iptv.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.ehtudo.iptv.BuildConfig
import app.ehtudo.iptv.R
import app.ehtudo.iptv.ui.interaction.TvClickableSurface
import app.ehtudo.iptv.ui.theme.OnSurface
import app.ehtudo.iptv.ui.theme.OnSurfaceDim
import app.ehtudo.iptv.ui.theme.Primary
import app.ehtudo.iptv.ui.theme.Secondary
import app.ehtudo.domain.manager.DriveAuthState

internal fun LazyListScope.settingsBackupSection(
    onCreateBackup: () -> Unit,
    onShareBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onCreateBackupUsb: (() -> Unit)? = null,
    onRestoreBackupUsb: (() -> Unit)? = null
) {
    item {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackupActionCard(
                    icon = "\u2191",
                    title = stringResource(R.string.settings_backup_data),
                    subtitle = stringResource(R.string.settings_backup_subtitle),
                    accent = Primary,
                    onClick = onCreateBackup,
                    modifier = Modifier.weight(1f)
                )
                BackupActionCard(
                    icon = "\u21aa",
                    title = stringResource(R.string.settings_backup_share_data),
                    subtitle = stringResource(R.string.settings_backup_share_subtitle),
                    accent = Primary,
                    onClick = onShareBackup,
                    modifier = Modifier.weight(1f)
                )
            }
            BackupActionCard(
                icon = "\u2193",
                title = stringResource(R.string.settings_restore_data),
                subtitle = stringResource(R.string.settings_restore_subtitle),
                accent = Secondary,
                onClick = onRestoreBackup,
                modifier = Modifier.fillMaxWidth()
            )
            if (onCreateBackupUsb != null && onRestoreBackupUsb != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BackupActionCard(
                        icon = "\u2191",
                        title = stringResource(R.string.settings_backup_usb_data),
                        subtitle = stringResource(R.string.settings_backup_usb_subtitle),
                        accent = Primary,
                        onClick = onCreateBackupUsb,
                        modifier = Modifier.weight(1f)
                    )
                    BackupActionCard(
                        icon = "\u2193",
                        title = stringResource(R.string.settings_restore_usb_data),
                        subtitle = stringResource(R.string.settings_restore_usb_subtitle),
                        accent = Secondary,
                        onClick = onRestoreBackupUsb,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

internal fun LazyListScope.settingsDriveBackupSection(
    uiState: SettingsUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit
) {
    item(key = "settings_drive_section") {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_drive_section_title),
                subtitle = stringResource(R.string.settings_drive_section_subtitle)
            )
            when (val auth = uiState.driveAuthState) {
                is DriveAuthState.SignedOut, is DriveAuthState.Pending -> {
                    BackupActionCard(
                        icon = "☁",
                        title = stringResource(R.string.settings_drive_signin),
                        subtitle = stringResource(R.string.settings_drive_signin_description),
                        accent = Primary,
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is DriveAuthState.SignedIn -> {
                    val accountLabel = auth.account.email
                        ?: auth.account.displayName
                        ?: stringResource(R.string.settings_drive_signin)
                    DriveAccountRow(
                        accountLabel = accountLabel,
                        lastPushAtMs = uiState.driveSyncStatus.lastPushAtMs,
                        lastPullAtMs = uiState.driveSyncStatus.lastPullAtMs,
                        onSignOut = onSignOut
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BackupActionCard(
                            icon = "↑",
                            title = stringResource(R.string.settings_drive_push),
                            subtitle = stringResource(R.string.settings_drive_push_subtitle),
                            accent = Primary,
                            onClick = onPush,
                            modifier = Modifier.weight(1f)
                        )
                        BackupActionCard(
                            icon = "↓",
                            title = stringResource(R.string.settings_drive_pull),
                            subtitle = stringResource(R.string.settings_drive_pull_subtitle),
                            accent = Secondary,
                            onClick = onPull,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DriveAccountRow(
    accountLabel: String,
    lastPushAtMs: Long?,
    lastPullAtMs: Long?,
    onSignOut: () -> Unit
) {
    val syncSummary = formatLastSync(lastPushAtMs, lastPullAtMs)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accountLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = syncSummary,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
        }
        TvClickableSurface(
            onClick = onSignOut,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.06f),
                focusedContainerColor = Color.White.copy(alpha = 0.18f)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Text(
                text = stringResource(R.string.settings_drive_signout),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurface
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun formatLastSync(pushMs: Long?, pullMs: Long?): String {
    if (pushMs == null && pullMs == null) {
        return stringResource(R.string.settings_drive_never_synced)
    }
    val df = java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.SHORT,
        java.text.DateFormat.SHORT
    )
    val parts = mutableListOf<String>()
    pushMs?.let { parts += stringResource(R.string.settings_drive_last_push, df.format(java.util.Date(it))) }
    pullMs?.let { parts += stringResource(R.string.settings_drive_last_pull, df.format(java.util.Date(it))) }
    return parts.joinToString("  ·  ")
}

@androidx.compose.runtime.Composable
private fun BackupActionCard(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvClickableSurface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = accent.copy(alpha = 0.12f),
            focusedContainerColor = accent.copy(alpha = 0.28f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = accent, textAlign = TextAlign.Center)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim, textAlign = TextAlign.Center)
        }
    }
}

internal fun LazyListScope.settingsAboutSection(
    onOpenUri: (String) -> Unit
) {
    item {
        SettingsRow(
            label = stringResource(R.string.settings_app_version),
            value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        ClickableSettingsRow(
            label = stringResource(R.string.settings_site),
            value = stringResource(R.string.settings_site_url),
            onClick = { onOpenUri(EH_IPTV_SITE_URL) }
        )
        ClickableSettingsRow(
            label = stringResource(R.string.settings_acknowledgment),
            value = stringResource(R.string.settings_acknowledgment_url),
            onClick = { onOpenUri(STREAMVAULT_REPO_URL) }
        )
    }
}

private const val EH_IPTV_SITE_URL = "https://iptv.ehtudo.app/"

private const val STREAMVAULT_REPO_URL = "https://github.com/Davidona/StreamVault-IPTV"
