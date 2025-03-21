/****************************************************************************
 * Copyright (C) 2025 CrowdWare
 *
 * This file is part of NoCodeLibMobile.
 *
 *  NoCodeLibMobile is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  NoCodeLibMobile is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with NoCodeLibMobile.  If not, see <http://www.gnu.org/licenses/>.
 *
 ****************************************************************************/
package at.crowdware.nocodelibmobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import at.crowdware.nocodelibmobile.utils.ThemeElement
import at.crowdware.nocodelibmobile.utils.hexToColor

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary
)

@Composable
fun NoCodeLibMobileTheme(
    theme: ThemeElement = ThemeElement(),
    content: @Composable () -> Unit
) {

    val scheme = ColorScheme(
        hexToColor(theme.primary, MaterialTheme.colorScheme.primary),
        hexToColor(theme.onPrimary, MaterialTheme.colorScheme.onPrimary),
        hexToColor(theme.primaryContainer, MaterialTheme.colorScheme.primaryContainer),
        hexToColor(theme.onPrimaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        hexToColor(theme.inversePrimary, MaterialTheme.colorScheme.inversePrimary),
        hexToColor(theme.secondary, MaterialTheme.colorScheme.secondary),
        hexToColor(theme.onSecondary, MaterialTheme.colorScheme.onSecondary),
        hexToColor(theme.secondaryContainer, MaterialTheme.colorScheme.secondaryContainer),
        hexToColor(theme.onSecondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
        hexToColor(theme.tertiary, MaterialTheme.colorScheme.tertiary),
        hexToColor(theme.onTertiary, MaterialTheme.colorScheme.onTertiary),
        hexToColor(theme.tertiaryContainer, MaterialTheme.colorScheme.tertiaryContainer),
        hexToColor(theme.onTertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer),
        hexToColor(theme.background, MaterialTheme.colorScheme.background),
        hexToColor(theme.onBackground, MaterialTheme.colorScheme.onBackground),
        hexToColor(theme.surface, MaterialTheme.colorScheme.surface),
        hexToColor(theme.onSurface, MaterialTheme.colorScheme.onSurface),
        hexToColor(theme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant),
        hexToColor(theme.onSurfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant),
        hexToColor(theme.surfaceTint, MaterialTheme.colorScheme.surfaceTint),
        hexToColor(theme.inverseSurface, MaterialTheme.colorScheme.inverseSurface),
        hexToColor(theme.inverseOnSurface, MaterialTheme.colorScheme.inverseOnSurface),
        hexToColor(theme.error, MaterialTheme.colorScheme.error),
        hexToColor(theme.onError, MaterialTheme.colorScheme.onError),
        hexToColor(theme.errorContainer, MaterialTheme.colorScheme.errorContainer),
        hexToColor(theme.onErrorContainer, MaterialTheme.colorScheme.onErrorContainer),
        hexToColor(theme.outline, MaterialTheme.colorScheme.outline),
        hexToColor(theme.outlineVariant, MaterialTheme.colorScheme.outlineVariant),
        hexToColor(theme.scrim, MaterialTheme.colorScheme.scrim))

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}