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

package at.crowdware.nocodelibmobile.utils

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import java.time.LocalDateTime


data class App(
    var name: String = "",
    var icon: String = "",
    var id: String = "",
    var smlVersion: String = "",
    var description: String = "",
    // only temporary used
    var restDatasourceId: String = "",
    var restDatasourceUrl: String = "",
    // only temporary end
    var theme: ThemeElement = ThemeElement(),
    var deployment: DeploymentElement = DeploymentElement()
)

data class ThemeElement(
    var primary: String = "",
    var onPrimary: String = "",
    var primaryContainer: String = "",
    var onPrimaryContainer: String = "",
    var secondary: String = "",
    var onSecondary: String = "",
    var secondaryContainer: String = "",
    var onSecondaryContainer: String = "",
    var tertiary: String = "",
    var onTertiary: String = "",
    var tertiaryContainer: String = "",
    var onTertiaryContainer: String = "",
    var error: String = "",
    var errorContainer: String = "",
    var onError: String = "",
    var onErrorContainer: String = "",
    var background: String = "",
    var onBackground: String = "",
    var surface: String = "",
    var onSurface: String = "",
    var surfaceVariant: String = "",
    var onSurfaceVariant: String = "",
    var outline: String = "",
    var inverseOnSurface: String = "",
    var inverseSurface: String = "",
    var inversePrimary: String = "",
    var surfaceTint: String = "",
    var outlineVariant: String = "",
    var scrim: String = ""
)

data class DeploymentElement(
    val files: MutableList<FileElement> = mutableListOf()
)

data class FileElement(val path: String, val time: LocalDateTime, val type: String)

data class PageElement (val src: String)


data class Page(
    var color: String,
    var backgroundColor: String,
    var padding: Padding,
    var scrollable: Boolean,
    val elements: MutableList<UIElement>)

sealed class UIElement {
    data object Zero : UIElement()
    data class TextElement(
        val text: String,
        val color: String,
        val fontSize: TextUnit,
        val fontWeight: FontWeight,
        val textAlign: TextAlign,
        val weight: Int,
        val width: Int,
        val height: Int
    ) : UIElement()
    data class ButtonElement(
        val label: String,
        val backgroundColor: String,
        val color: String,
        val link: String,
        val weight: Int,
        val width: Int,
        val height: Int) : UIElement()
    data class ImageElement(
        val src: String,
        val padding: Padding,
        val scale: String,
        val weight: Int,
        val width: Int,
        val height: Int,
        val align: String,
        val link: String) : UIElement()
    data class AsyncImageElement(
        val src: String,
        val padding: Padding,
        val scale: String,
        val weight: Int,
        val width: Int,
        val height: Int,
        val align: String,
        val link: String) : UIElement()
    data class SpacerElement(
        val amount: Int,
        val weight: Int) : UIElement()
    data class VideoElement(
        val src: String,
        val weight: Int,
        val width: Int,
        val height: Int) : UIElement()
    data class YoutubeElement(
        val id: String,
        val weight: Int,
        val width: Int,
        val height: Int) : UIElement()
    data class SoundElement(
        val src: String) : UIElement()
    data class RowElement(
        val padding: Padding,
        val weight: Int,
        val width: Int,
        val height: Int,
        val uiElements: MutableList<UIElement> = mutableListOf()
    ) : UIElement()
    data class ColumnElement(
        val padding: Padding,
        val weight: Int,
        val width: Int,
        val height: Int,
        val uiElements: MutableList<UIElement> = mutableListOf()
    ) : UIElement()
    data class BoxElement(
        val padding: Padding,
        val weight: Int,
        val width: Int,
        val height: Int,
        val uiElements: MutableList<UIElement> = mutableListOf()
    ) : UIElement()
    data class MarkdownElement(
        val text: String,
        val color: String,
        val fontSize: TextUnit,
        val fontWeight: FontWeight,
        val textAlign: TextAlign,
        val weight: Int,
        val width: Int,
        val height: Int) : UIElement()
    data class SceneElement(
        val weight: Int,
        val width: Int,
        val height: Int,
        val model: String,
        val ibl: String,
        val skybox: String
    ) : UIElement()
    data class LazyColumnElement(
        val padding: Padding,
        val datasource: String,
        val filter: String,
        val limit: Int,
        val order: String,
        val weight: Int,
        val uiElements: MutableList<UIElement> = mutableListOf()
    ) : UIElement()
    data class LazyRowElement(
        val padding: Padding,
        val datasource: String,
        val filter: String,
        val limit: Int,
        val order: String,
        val weight: Int,
        val uiElements: MutableList<UIElement> = mutableListOf()
    ) : UIElement()
    data class LazyContentElement(
        val uiElements: MutableList<UIElement> = mutableListOf()
    ) : UIElement()
    data class LazyNoContentElement(
        val uiElements: MutableList<UIElement> = mutableListOf()
    ) : UIElement()
}

data class Padding(val top: Int, val right: Int, val bottom: Int, val left: Int)
