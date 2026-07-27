package app.ehtudo.iptv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ehtudo.iptv.R
import app.ehtudo.iptv.ui.theme.Primary

private data class SettingsNavEntry(
    val label: String,
    val icon: String,
    val accent: Color
)

@Composable
internal fun SettingsNavigationRail(
    selectedCategory: Int,
    focusRequester: FocusRequester,
    onCategorySelected: (Int) -> Unit
) {
    val entries = listOf(
        SettingsNavEntry(
            label = stringResource(R.string.settings_providers),
            icon = "P",
            accent = Primary
        ),
        SettingsNavEntry(
            label = stringResource(R.string.settings_playback),
            icon = ">",
            accent = Color(0xFF9E8FFF)
        ),
        SettingsNavEntry(
            label = stringResource(R.string.settings_privacy),
            icon = "L",
            accent = Color(0xFFFFB74D)
        ),
        SettingsNavEntry(
            label = stringResource(R.string.settings_about),
            icon = "i",
            accent = Color(0xFF78909C)
        )
    )

    LazyColumn(
        modifier = Modifier
            .width(236.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.25f)),
        contentPadding = PaddingValues(top = 76.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(entries) { index, entry ->
            SettingsNavItem(
                label = entry.label,
                badgeChar = entry.icon,
                accentColor = entry.accent,
                isSelected = selectedCategory == index,
                modifier = if (selectedCategory == index) Modifier.focusRequester(focusRequester) else Modifier,
                onClick = { onCategorySelected(index) }
            )
        }
    }
}