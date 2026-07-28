package app.ehtudo.iptv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.ehtudo.iptv.R
import app.ehtudo.iptv.ui.interaction.TvClickableSurface
import app.ehtudo.iptv.ui.theme.OnBackground
import app.ehtudo.iptv.ui.theme.OnSurface
import app.ehtudo.iptv.ui.theme.Primary

internal fun LazyListScope.settingsPrivacySection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onShowClearHistoryDialogChange: (Boolean) -> Unit
) {
    item {
        AdultContentToggleRow(
            enabled = uiState.adultContentEnabled,
            onToggle = viewModel::setAdultContentEnabled
        )
    }
    item {
        HorizontalDivider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(vertical = 4.dp))
        TvClickableSurface(
            onClick = { onShowClearHistoryDialogChange(true) },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Primary.copy(alpha = 0.15f)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.settings_clear_history), style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    Text(text = stringResource(R.string.settings_clear_history_subtitle), style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.6f))
                }
                Text(text = stringResource(R.string.settings_clear_history_confirm), style = MaterialTheme.typography.labelLarge, color = Primary)
            }
        }
    }
}

@Composable
private fun AdultContentToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    TvClickableSurface(
        onClick = { onToggle(!enabled) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Primary.copy(alpha = 0.15f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_adult_content), style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                Text(text = stringResource(R.string.settings_adult_content_subtitle), style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.6f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (enabled) R.string.settings_adult_content_status_locked
                        else R.string.settings_adult_content_status_hidden
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) Primary else OnBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 12.dp)
                )
                Switch(checked = enabled, onCheckedChange = null)
            }
        }
    }
}