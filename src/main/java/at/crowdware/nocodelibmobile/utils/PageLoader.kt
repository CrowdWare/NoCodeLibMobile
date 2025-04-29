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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.view.Choreographer
import android.view.SurfaceView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import at.crowdware.nocodelibmobile.BaseComposeActivity
import at.crowdware.nocodelibmobile.getStringValue
import at.crowdware.nocodelibmobile.logic.LocaleManager
import at.crowdware.nocodelibmobile.parseSML
import at.crowdware.nocodelibmobile.ui.SettingsDialog
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import coil3.compose.AsyncImage



@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LoadPage(
    name: String,
    navhostBackground: MutableState<Color>,
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    data: MutableState<Map<String, List<Any>>>
) {
    var page:Page? by remember { mutableStateOf(Page(color="#FFFFFF", backgroundColor = "#000000", padding = Padding(0,0,0,0), false, elements = mutableListOf()))}
    var isLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val clickCount = remember { mutableStateOf(0) } // used to recompose only in LazyColumns and LazyRow

    LaunchedEffect(Unit) {
        isLoading = true
        page = null
        page = withContext(Dispatchers.IO) {
            mainActivity.contentLoader.loadPage(name, mainActivity)
        }
        isLoading = false
    }
    if (isLoading) {
        Box (modifier = Modifier.fillMaxSize().background(color = navhostBackground.value)){
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    } else {
        if (page != null) {
            val padding = page!!.padding
            val bgColor = page!!.backgroundColor
            navhostBackground.value = hexToColor(bgColor, MaterialTheme.colorScheme.background)
            var modifier = Modifier as Modifier
            if (page!!.scrollable == true) {
                modifier = modifier.verticalScroll(scrollState)
            }
            var showSettingsDialog by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showSettingsDialog = true
                            }
                        )
                    }
            ) {
                if (showSettingsDialog && mainActivity.contentLoader.appUrl != mainActivity.getBaseUrl().substringBefore("/app.sml")) {
                    SettingsDialog(
                        onDismiss = {showSettingsDialog = false},
                        onConfirm = {
                        showSettingsDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                mainActivity.contentLoader.switchApp(mainActivity.getBaseUrl())
                            }
                            NavigationManager.navigate("app.home")
                        }

                    })
                }
                Column(
                    modifier = modifier
                        .padding(
                            start = padding.left.dp,
                            top = padding.top.dp,
                            bottom = padding.bottom.dp,
                            end = padding.right.dp
                        )
                        .fillMaxSize()
                        .background(
                            color = hexToColor(
                                bgColor,
                                MaterialTheme.colorScheme.background
                            )
                        )
                ) {
                    RenderPage(page!!, mainActivity, navController, clickCount, data, "")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RenderPage(
    page: Page,
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    var dataItem: Any = emptyMap<String, Any>() // no data yet

    for (element in page.elements) {
        RenderElement(element, mainActivity, navController, dataItem, false, clickCount, datasources, datasourceId)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RowScope.RenderElement(
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    element: UIElement,
    dataItem: Any,
    isInLazy: Boolean = false,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    when(element) {
        is UIElement.ColumnElement -> {
            renderColumn(mainActivity, navController, if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier, element, dataItem, isInLazy = isInLazy, clickCount, datasources, datasourceId)
        }
        is UIElement.RowElement -> {
            renderRow(mainActivity, navController, element, dataItem, clickCount = clickCount, datasources = datasources, datasourceId = datasourceId)
        }
        is UIElement.BoxElement -> {
            renderBox(mainActivity, navController, element, dataItem, clickCount = clickCount, datasources = datasources, datasourceId = datasourceId)
        }
        is UIElement.LazyColumnElement -> {
            renderLazyColumn(
                if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                mainActivity,
                navController,
                element,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.LazyRowElement -> {
            renderLazyRow(
                if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                mainActivity,
                navController,
                element,
                clickCount,
                datasources
            )
        }
        is UIElement.TextElement -> {
            renderText(element, dataItem, mainActivity)
        }
        is UIElement.MarkdownElement -> {
            renderMarkdown(
                modifier = if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                element,
                dataItem = dataItem,
                mainActivity
            )
        }
        is UIElement.ButtonElement -> {
            renderButton(
                modifier = if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier.weight(1f),
                element = element,
                mainActivity = mainActivity,
                navController = navController,
                dataItem = dataItem,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.ImageElement -> {
            if (isInLazy) {
                dynamicImageFromAssets(
                    modifier = Modifier.width(element.width.dp),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                dynamicImageFromAssets(
                    modifier = if (element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                    mainActivity = mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem = dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            }
        }
        is UIElement.AsyncImageElement -> {
            if (isInLazy) {
                asyncImage(
                    modifier = Modifier.width(element.width.dp),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                asyncImage(
                    modifier = if (element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                    mainActivity = mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem = dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            }
        }
        is UIElement.VideoElement -> {
            dynamicVideofromAssets(modifier = if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier, mainActivity = mainActivity, filename = element.src, dataItem = dataItem)
        }
        is UIElement.SoundElement -> {
            dynamicSoundfromAssets(mainActivity, element.src, dataItem)
        }
        is UIElement.YoutubeElement -> {
            dynamicYoutube(modifier = if(element.weight > 0){Modifier.weight(element.weight.toFloat())} else {Modifier}, videoId = element.id, dataItem = dataItem)
        }
        is UIElement.SceneElement -> {
            dynamicScene(
                modifier = if(element.weight > 0){Modifier.weight(element.weight.toFloat())} else {Modifier},
                element,
                dataItem
            )
        }
        is UIElement.SpacerElement -> {
            var mod = Modifier as Modifier
            if (element.amount > 0)
                mod = mod.then(Modifier.width(element.amount.dp))
            if (element.weight > 0)
                mod = mod.then(Modifier.weight(element.weight.toFloat()))
            Spacer(modifier = mod)
        }
        else -> {}
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ColumnScope.RenderElement(
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    element: UIElement,
    dataItem: Any,
    isInLazy: Boolean = false,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    when (element) {
        is UIElement.ColumnElement -> {
            renderColumn(
                mainActivity,
                navController,
                if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                element,
                dataItem,
                isInLazy = isInLazy,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.RowElement -> {
            renderRow(
                mainActivity,
                navController,
                element,
                dataItem,
                isInLazy = isInLazy,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.BoxElement -> {
            renderBox(
                mainActivity,
                navController,
                element,
                dataItem,
                isInLazy = isInLazy,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.LazyColumnElement -> {
            renderLazyColumn(
                if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                mainActivity,
                navController,
                element,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.LazyRowElement -> {
            renderLazyRow(
                if(element.weight > 0) Modifier.weight(element.weight.toFloat()) else Modifier,
                mainActivity,
                navController,
                element,
                clickCount,
                datasources
            )
        }
        is UIElement.TextElement -> {
            renderText(element, dataItem, mainActivity)
        }
        is UIElement.MarkdownElement -> {
            renderMarkdown(modifier = if(element.weight > 0){Modifier.weight(element.weight.toFloat())} else {Modifier}, element)
        }
        is UIElement.ButtonElement -> {
            renderButton(
                modifier= Modifier.fillMaxWidth(),
                element = element,
                mainActivity = mainActivity,
                navController = navController,
                dataItem = dataItem,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.ImageElement -> {
            if (isInLazy) {
                dynamicImageFromAssets(
                    modifier = Modifier.width(element.width.dp),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                dynamicImageFromAssets(
                    modifier = if (element.weight > 0) {
                        Modifier.weight(element.weight.toFloat())
                    } else {
                        Modifier
                    },
                    mainActivity = mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem = dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            }
        }
        is UIElement.AsyncImageElement -> {
            if (isInLazy) {
                asyncImage(
                    modifier = Modifier.width(element.width.dp),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                asyncImage(
                    modifier = if (element.weight > 0) {
                        Modifier.weight(element.weight.toFloat())
                    } else {
                        Modifier
                    },
                    mainActivity = mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem = dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            }
        }
        is UIElement.VideoElement -> {
            dynamicVideofromAssets(modifier = if(element.weight > 0){Modifier.weight(element.weight.toFloat())} else {Modifier}, mainActivity = mainActivity, filename = element.src, dataItem = dataItem)
        }
        is UIElement.SoundElement -> {
            dynamicSoundfromAssets(mainActivity, element.src, dataItem)
        }
        is UIElement.YoutubeElement -> {
            dynamicYoutube(modifier = if(element.weight > 0){Modifier.weight(element.weight.toFloat())} else {Modifier}, videoId = element.id, dataItem = dataItem)
        }
        is UIElement.SceneElement -> {
            dynamicScene(
                modifier = if(element.weight > 0){Modifier.weight(element.weight.toFloat())} else {Modifier},
                element,
                dataItem
            )
        }
        is UIElement.SpacerElement -> {
            var mod = Modifier as Modifier
            if (element.amount > 0)
                mod = mod.then(Modifier.height(element.amount.dp))
            if (element.weight > 0)
                mod = mod.then(Modifier.weight(element.weight.toFloat()))
            Spacer(modifier = mod)
        }
        else -> {}
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BoxScope.RenderElement(
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    element: UIElement,
    dataItem: Any,
    isInLazy: Boolean = false,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    when(element) {
        is UIElement.ColumnElement -> {
            renderColumn(
                mainActivity,
                navController,
                modifier = Modifier,
                element,
                dataItem,
                isInLazy = isInLazy,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.RowElement -> {
            renderRow(mainActivity, navController, element, dataItem, clickCount = clickCount, datasources = datasources, datasourceId = datasourceId)
        }
        is UIElement.BoxElement -> {
            renderBox(mainActivity, navController, element, dataItem, clickCount = clickCount, datasources = datasources, datasourceId = datasourceId)
        }
        is UIElement.LazyColumnElement -> {
            renderLazyColumn(
                modifier =  Modifier,
                mainActivity,
                navController,
                element,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.LazyRowElement -> {
            renderLazyRow(
                modifier =  Modifier,
                mainActivity,
                navController,
                element,
                clickCount,
                datasources
            )
        }
        is UIElement.TextElement -> {
            renderText(element, dataItem, mainActivity)
        }
        is UIElement.MarkdownElement -> {
            renderMarkdown(modifier = Modifier, element, dataItem = dataItem, mainActivity)
        }
        is UIElement.ButtonElement -> {
            renderButton(
                modifier = Modifier,
                element = element,
                mainActivity = mainActivity,
                navController = navController,
                dataItem = dataItem,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.ImageElement -> {
            val alignment = if (element.align.isNotEmpty()) element.align.lowercase().toAlignment() else Alignment.TopStart
            if (isInLazy) {
                dynamicImageFromAssets(
                    modifier = Modifier.width(element.width.dp).align(alignment),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                dynamicImageFromAssets(
                    modifier = Modifier.width(element.width.dp).align(alignment),
                    mainActivity = mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem = dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            }
        }
        is UIElement.AsyncImageElement -> {
            val alignment = if (element.align.isNotEmpty()) element.align.lowercase().toAlignment() else Alignment.TopStart
            if (isInLazy) {
                asyncImage(
                    modifier = Modifier.width(element.width.dp).align(alignment),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                asyncImage(
                    modifier = Modifier.align(alignment),
                    mainActivity = mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem = dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            }
        }
        is UIElement.VideoElement -> {
            dynamicVideofromAssets(modifier = Modifier, mainActivity = mainActivity, filename = element.src, dataItem = dataItem)
        }
        is UIElement.SoundElement -> {
            dynamicSoundfromAssets(mainActivity, element.src, dataItem)
        }
        is UIElement.YoutubeElement -> {
            dynamicYoutube(modifier = Modifier, videoId = element.id, dataItem = dataItem)
        }
        is UIElement.SceneElement -> {
            dynamicScene(
                modifier = Modifier,
                element,
                dataItem
            )
        }
        is UIElement.SpacerElement -> {
            var mod = Modifier as Modifier
            if (element.amount > 0)
                mod = mod.then(Modifier.width(element.amount.dp))
            Spacer(modifier = mod)
        }
        else -> {}
    }
}

fun String.toAlignment(): Alignment {
    return when (this) {
        "topstart" -> Alignment.TopStart
        "topcenter" -> Alignment.TopCenter
        "topend" -> Alignment.TopEnd
        "centerstart" -> Alignment.CenterStart
        "center" -> Alignment.Center
        "centerend" -> Alignment.CenterEnd
        "bottomstart" -> Alignment.BottomStart
        "bottomcenter" -> Alignment.BottomCenter
        "bottomend" -> Alignment.BottomEnd
        else -> Alignment.TopStart // Default fallback
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun renderColumn(
    mainActivity: BaseComposeActivity,
    navcontroller: NavHostController,
    modifier: Modifier,
    element: UIElement.ColumnElement,
    dataItem: Any,
    isInLazy: Boolean,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    Column (modifier = modifier.padding(
        top = element.padding.top.dp,
        bottom = element.padding.bottom.dp,
        start = element.padding.left.dp,
        end = element.padding.right.dp
    )) {
        for (ele in element.uiElements) {
            RenderElement(mainActivity = mainActivity, navController = navcontroller, element = ele, dataItem = dataItem,
                isInLazy = isInLazy,
                clickCount = clickCount,
                datasources = datasources,
                datasourceId = datasourceId)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun renderRow(
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    element: UIElement.RowElement,
    dataItem: Any,
    isInLazy: Boolean = false,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    Row (horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.padding(
        top = element.padding.top.dp,
        bottom = element.padding.bottom.dp,
        start = element.padding.left.dp,
        end = element.padding.right.dp
    )) {
        for (ele in element.uiElements) {
            RenderElement(mainActivity, navController, ele, dataItem, isInLazy = isInLazy, clickCount, datasources, datasourceId)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun renderBox(
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    element: UIElement.BoxElement,
    dataItem: Any,
    isInLazy: Boolean = false,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    Box(
        modifier = Modifier
            .padding(
                top = element.padding.top.dp,
                bottom = element.padding.bottom.dp,
                start = element.padding.left.dp,
                end = element.padding.right.dp
            )
            .then(if (element.width > 0) Modifier.width(element.width.dp) else Modifier)
            .then(if (element.height > 0) Modifier.height(element.height.dp) else Modifier)
    ) {
        for (ele in element.uiElements) {
            RenderElement(mainActivity, navController, ele, dataItem, isInLazy = isInLazy, clickCount, datasources, datasourceId)
        }
    }
}

fun applyFilter(
    data: List<Any>,
    filter: String?,
    mainActivity: BaseComposeActivity
): List<Any> {
    if (filter == null) return data
    val regex = Regex("""(inList|notInList):(\w+)\[(\w+)]""")
    val match = regex.find(filter) ?: return data

    val (mode, listName, paramName) = match.destructured
    val prefs = mainActivity.getSharedPreferences("nocode_lists", Context.MODE_PRIVATE)
    val values = prefs.getStringSet(listName, emptySet()) ?: emptySet()

    return when (mode) {
        "inList" -> data.filter {
            (it as? Map<*, *>)?.get(paramName).toString() in values
        }
        "notInList" -> data.filter {
            (it as? Map<*, *>)?.get(paramName).toString() !in values
        }
        else -> data
    }
}

fun applyOrder(data: List<Any>, order: String?): List<Any> {
    if (order == null) return data
    val (field, dir) = order.split(" ").let { it[0] to (it.getOrNull(1) ?: "asc") }

    val sorted = data.sortedBy {
        (it as? Map<*, *>)?.get(field) as? Comparable<Any>
    }
    return if (dir == "desc") sorted.reversed() else sorted
}

fun applyLimit(data: List<Any>, limit: Int?): List<Any> {
    return if (limit != null && limit > 0) data.take(limit) else data
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun renderLazyColumn(
    modifier: Modifier,
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    element: UIElement.LazyColumnElement,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    val EmptyDataItem = object {}
    val rawData = datasources.value[element.datasource]

    if (rawData == null) {
        CircularProgressIndicator(modifier = modifier)
        return
    }

    val filteredData = applyFilter(rawData, element.filter, mainActivity)
    val sortedData = applyOrder(filteredData, element.order)
    val finalData = applyLimit(sortedData, element.limit)

    // Animation beim Wechsel der finalData
    AnimatedContent(
        modifier = modifier.padding(element.padding.left.dp, element.padding.top.dp, element.padding.right.dp, element.padding.bottom.dp),
        targetState = clickCount.value to finalData,
        transitionSpec = {
            fadeIn(tween(300)) with fadeOut(tween(300))
        },
        label = "AnimatedLazyColumn"
    ) { (_,animatedList) ->
        if (animatedList.isEmpty()) {
            val emptyBlock = element.uiElements
                .find { it is UIElement.LazyNoContentElement } as? UIElement.LazyNoContentElement

            emptyBlock?.uiElements?.forEach { ele ->
                RenderElement(
                    element = ele,
                    mainActivity = mainActivity,
                    navController = navController,
                    dataItem = EmptyDataItem,
                    isInLazy = false,
                    clickCount = clickCount,
                    datasources = datasources,
                    datasourceId = element.datasource
                )
            }
        } else {
            val contentBlock = element.uiElements
                .find { it is UIElement.LazyContentElement } as? UIElement.LazyContentElement

            LazyColumn() {
                items(animatedList, key = { it.hashCode() }) { dataItem ->
                    Box(modifier = Modifier.animateItemPlacement()) {
                        contentBlock?.uiElements?.forEach { ele ->
                            RenderElement(
                                element = ele,
                                mainActivity = mainActivity,
                                navController = navController,
                                dataItem = dataItem,
                                isInLazy = true,
                                clickCount = clickCount,
                                datasources = datasources,
                                datasourceId = element.datasource
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun renderLazyRow(
    modifier: Modifier,
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    element: UIElement.LazyRowElement,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>
) {
    val EmptyDataItem = object {}
    val rawData = datasources.value[element.datasource]

    if (rawData == null) {
        CircularProgressIndicator(modifier = modifier)
        return
    }

    val filteredData = applyFilter(rawData, element.filter, mainActivity)
    val sortedData = applyOrder(filteredData, element.order)
    val finalData = applyLimit(sortedData, element.limit)


    AnimatedContent(
        modifier = modifier.padding(element.padding.left.dp, element.padding.top.dp, element.padding.right.dp, element.padding.bottom.dp),
        targetState = clickCount.value to finalData,
        transitionSpec = {
            fadeIn(tween(300)) with fadeOut(tween(300))
        },
        label = "AnimatedLazyRow"
    ) { (_,animatedList) ->
        if (animatedList.isEmpty()) {
            val emptyBlock = element.uiElements
                .find { it is UIElement.LazyNoContentElement } as? UIElement.LazyNoContentElement

            emptyBlock?.uiElements?.forEach { ele ->
                RenderElement(
                    element = ele,
                    mainActivity = mainActivity,
                    navController = navController,
                    dataItem = EmptyDataItem,
                    isInLazy = false,
                    clickCount = clickCount,
                    datasources = datasources,
                    datasourceId = element.datasource
                )
            }
        } else {
            val contentBlock = element.uiElements
                .find { it is UIElement.LazyContentElement } as? UIElement.LazyContentElement

            LazyRow() {
                items(animatedList, key = { it.hashCode() }) { dataItem ->
                    Box(modifier = Modifier.animateItemPlacement()) {
                        contentBlock?.uiElements?.forEach { ele ->
                            RenderElement(
                                element = ele,
                                mainActivity = mainActivity,
                                navController = navController,
                                dataItem = dataItem,
                                isInLazy = true,
                                clickCount = clickCount,
                                datasources = datasources,
                                datasourceId = element.datasource
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun renderText(element: UIElement.TextElement, dataItem: Any, mainActivity: BaseComposeActivity) {
    val txt = translate(element.text, mainActivity = mainActivity)
    Text(
        text = element.text.trim(),
        style = TextStyle(
            color = hexToColor(element.color, MaterialTheme.colorScheme.onBackground),
            fontSize = element.fontSize,
            fontWeight = element.fontWeight,
            textAlign = element.textAlign
        )
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RowScope.renderMarkdown(modifier: Modifier, element: UIElement.MarkdownElement) {
    val context = LocalContext.current
    val mainActivity = context as BaseComposeActivity
    var cacheName by remember { mutableStateOf("") }
    var text = ""

    if (element.text.startsWith("part:")) {
        val part = element.text.substringAfter("part:")
        LaunchedEffect(part) {
            cacheName = withContext(Dispatchers.IO) {
                mainActivity.contentLoader.loadPart(part)
            }
        }
        if (cacheName.isNotEmpty()) {
            text = loadTextAssetFromCache(cacheName, context).toString()
            val parsedMarkdown = parseMarkdown(text)

            ClickableText(
                modifier = modifier,
                text = parsedMarkdown,
                onClick = {offset -> parsedMarkdown.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    handleHyperlinkClick(context, annotation.item)
                }},
                style = TextStyle(
                    color = hexToColor(element.color, MaterialTheme.colorScheme.onBackground),
                    fontSize = element.fontSize,
                    fontWeight = element.fontWeight,
                    textAlign = element.textAlign
                )
            )
        }
    } else {
        text = element.text.trim()
        val parsedMarkdown = parseMarkdown(translate(text, mainActivity))
        ClickableText(modifier = modifier,
            text = parsedMarkdown,
            onClick = {offset -> parsedMarkdown.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                handleHyperlinkClick(context, annotation.item)
            }},
            style = TextStyle(
                color = hexToColor(element.color, MaterialTheme.colorScheme.onBackground),
                fontSize = element.fontSize,
                fontWeight = element.fontWeight,
                textAlign = element.textAlign
            )
        )
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ColumnScope.renderMarkdown(modifier: Modifier, element: UIElement.MarkdownElement) {
    val context = LocalContext.current
    val mainActivity = context as BaseComposeActivity
    var cacheName by remember { mutableStateOf("") }
    var text = ""

    if (element.text.startsWith("part:")) {
        val part = element.text.substringAfter("part:")
        LaunchedEffect(part) {
            cacheName = withContext(Dispatchers.IO) {
                mainActivity.contentLoader.loadPart(part)
            }
        }
        if (cacheName.isNotEmpty()) {
            text = loadTextAssetFromCache(cacheName, context).toString()
            val parsedMarkdown = parseMarkdown(text)
            ClickableText(
                modifier = modifier,
                text = parsedMarkdown,
                onClick = {offset -> parsedMarkdown.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    handleHyperlinkClick(context, annotation.item)
                }},
                style = TextStyle(
                    color = hexToColor(element.color, MaterialTheme.colorScheme.onBackground),
                    fontSize = element.fontSize,
                    fontWeight = element.fontWeight,
                    textAlign = element.textAlign
                )
            )
        }
    } else {
        text = element.text.trim()
        val parsedMarkdown = parseMarkdown(translate(text, mainActivity))
        ClickableText(modifier = modifier,
            text = parsedMarkdown,
            onClick = {offset -> parsedMarkdown.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                handleHyperlinkClick(context, annotation.item)
            }},
            style = TextStyle(
                color = hexToColor(element.color, MaterialTheme.colorScheme.onBackground),
                fontSize = element.fontSize,
                fontWeight = element.fontWeight,
                textAlign = element.textAlign
            )
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun renderMarkdown(
    modifier: Modifier,
    element: UIElement.MarkdownElement,
    dataItem: Any,
    mainActivity: BaseComposeActivity
) {
    val context = LocalContext.current
    val mainActivity = context as BaseComposeActivity
    var cacheName by remember { mutableStateOf("") }
    var text = ""

    if (element.text.startsWith("part:")) {
        val part = element.text.substringAfter("part:")
        LaunchedEffect(part) {
            cacheName = withContext(Dispatchers.IO) {
                mainActivity.contentLoader.loadPart(part)
            }
        }
        if (cacheName.isNotEmpty()) {
            text = loadTextAssetFromCache(cacheName, context).toString()
            val parsedMarkdown = parseMarkdown(text)
            ClickableText(
                text = parsedMarkdown,
                onClick = {offset -> parsedMarkdown.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    handleHyperlinkClick(context, annotation.item)
                }},
                style = TextStyle(
                    color = hexToColor(element.color, MaterialTheme.colorScheme.onBackground),
                    fontSize = element.fontSize,
                    fontWeight = element.fontWeight,
                    textAlign = element.textAlign
                )
            )
        }
    } else {
        text = element.text.trim()
        if (text.startsWith("<") && text.endsWith(">")) {
            val fieldName = text.substring(1, text.length - 1)
            if (dataItem is Map<*, *> && fieldName.isNotEmpty()) {
                val des = dataItem[fieldName] as? String
                text = "$des"
            }
        }
        val parsedMarkdown = parseMarkdown(translate(text, mainActivity))
        ClickableText(modifier = modifier,
            text = parsedMarkdown,
            onClick = {offset -> parsedMarkdown.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                handleHyperlinkClick(context, annotation.item)
            }},
            style = TextStyle(
                color = hexToColor(element.color, MaterialTheme.colorScheme.onBackground),
                fontSize = element.fontSize,
                fontWeight = element.fontWeight,
                textAlign = element.textAlign
            )
        )
    }
}

fun handleHyperlinkClick(context: Context, url: String) {
    when {
        url.startsWith("http://") || url.startsWith("https://") -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
        url.startsWith("mailto:") -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        }
        else -> {
            // fallback
            Toast.makeText(context, "Unknown Link: $url", Toast.LENGTH_SHORT).show()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun renderButton(
    modifier: Modifier = Modifier,
    element: UIElement.ButtonElement,
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    dataItem: Any,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    var colors = buttonColors()

    if(element.color.isNotEmpty() && element.backgroundColor.isNotEmpty())
        colors = buttonColors(
            containerColor = hexToColor(element.backgroundColor, Color.Unspecified),
            contentColor = hexToColor(element.color, Color.Unspecified))
    else if(element.color.isNotEmpty())
        colors = buttonColors(contentColor = hexToColor(element.color, Color.Unspecified))
    else if(element.backgroundColor.isNotEmpty())
        colors = buttonColors(
            containerColor = hexToColor(element.backgroundColor, Color.Unspecified))

    val label = translate(element.label, mainActivity)
    Button(
        modifier = modifier
            .then(if(element.width > 0)Modifier.width(element.width.dp) else Modifier)
            .then(if(element.height > 0)Modifier.height(element.height.dp) else Modifier),
        colors = colors,
        onClick =  { handleButtonClick(element.link, mainActivity, navController, dataItem, clickCount, datasources, datasourceId) }) {
        Text(text = label)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RenderElement(
    element: UIElement,
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    dataItem: Any,
    isInLazy: Boolean = false,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    when (element) {
        is UIElement.ColumnElement -> {
            renderColumn(
                mainActivity,
                navController,
                Modifier,
                element,
                dataItem,
                isInLazy = isInLazy,
                clickCount,
                datasources,
                datasourceId
            )
        }
        is UIElement.RowElement -> {
            renderRow(mainActivity, navController, element, dataItem, false, clickCount, datasources, datasourceId)
        }
        is UIElement.BoxElement -> {
            renderBox(mainActivity, navController, element, dataItem, false, clickCount, datasources, datasourceId)
        }
        is UIElement.LazyColumnElement -> {
            renderLazyColumn(Modifier, mainActivity, navController, element, clickCount = clickCount, datasources = datasources, datasourceId = datasourceId)
        }
        is UIElement.LazyRowElement -> {
            renderLazyRow(Modifier, mainActivity, navController, element, clickCount, datasources = datasources)
        }
        is UIElement.TextElement -> {
            renderText(element, dataItem, mainActivity)
        }
        is UIElement.MarkdownElement -> {
           renderMarkdown(modifier= Modifier, element, dataItem, mainActivity)
        }
        is UIElement.ButtonElement -> {
            renderButton(modifier = Modifier.fillMaxWidth(), element = element, mainActivity = mainActivity,
                navController= navController, dataItem, clickCount, datasources, datasourceId)
        }
        is UIElement.ImageElement -> {
            if (isInLazy) {
                dynamicImageFromAssets(
                    modifier = Modifier.width(element.width.dp),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                dynamicImageFromAssets(
                    modifier = Modifier,
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )

            }
        }
        is UIElement.AsyncImageElement -> {
            if (isInLazy) {
                asyncImage(
                    modifier = Modifier.width(element.width.dp),
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            } else {
                asyncImage(
                    modifier = Modifier,
                    mainActivity,
                    navcontroller = navController,
                    element,
                    dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                )
            }
        }
        is UIElement.VideoElement -> {
            dynamicVideofromAssets(modifier = Modifier, mainActivity = mainActivity, filename = element.src, dataItem = dataItem)
        }
        is UIElement.SoundElement -> {
            dynamicSoundfromAssets(mainActivity, element.src, dataItem)
        }
        is UIElement.YoutubeElement -> {
            dynamicYoutube(modifier = Modifier, videoId = element.id, dataItem = dataItem)
        }
        is UIElement.SceneElement -> {
            dynamicScene(modifier = Modifier, element = element, dataItem)
        }
        is UIElement.SpacerElement -> {
            var mod = Modifier as Modifier
            if (element.amount > 0)
                mod = mod.then(Modifier.height(element.amount.dp).then(Modifier.width(element.amount.dp)))
            Spacer(modifier = mod)
        }
        else -> {}
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun handleButtonClick(
    link: String,
    mainActivity: BaseComposeActivity,
    navController: NavHostController,
    dataItem: Any,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    when {
        link.startsWith("page:") -> {
            val pageId = link.removePrefix("page:")
            navController.navigate(pageId)
        }
        link.startsWith("web:") -> {
            val url = link.removePrefix("web:")
            mainActivity.openWebPage(url)
        }
        link.startsWith("animation:") -> {
            val aniType = link.removePrefix("animation:")
            mainActivity.sendToAnimation(aniType)
        }
        link.startsWith("book:") -> {
            val url = link.removePrefix("book:")

            if(url.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    mainActivity.contentLoader.switchApp(url)
                }
            }
            NavigationManager.navigate("home")
        }
        link.startsWith("add:") -> {
            val (listName, uuid) = extractListAndUUID(link.removePrefix("add:"), dataItem)
            val prefs = mainActivity.getSharedPreferences("nocode_lists", Context.MODE_PRIVATE)
            val set = prefs.getStringSet(listName, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            set.add(uuid)
            prefs.edit().putStringSet(listName, set).apply()
            clickCount.value++ // trigger reload
            val current = datasources.value[datasourceId] ?: emptyList()
            val newList = current.toList()
            datasources.value = datasources.value.toMutableMap().apply {
                put(datasourceId, newList)
            }
        }
        link.startsWith("remove:") -> {
            val (listName, uuid) = extractListAndUUID(link.removePrefix("remove:"), dataItem)
            val prefs = mainActivity.getSharedPreferences("nocode_lists", Context.MODE_PRIVATE)
            val set = prefs.getStringSet(listName, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            set.remove(uuid)
            prefs.edit().putStringSet(listName, set).apply()
            clickCount.value++ // trigger reload
            val current = datasources.value[datasourceId] ?: emptyList()
            val newList = current.toList() // ← verändert Inhalt garantiert
            datasources.value = datasources.value.toMutableMap().apply {
                put(datasourceId, newList)
            }

        }
        else -> {
            println("Unknown link type: $link")
        }
    }
}

// ✨ Hilfsfunktion zum Parsen von add:listName[uuid]
fun extractListAndUUID(linkPart: String, dataItem: Any): Pair<String, String> {
    val regex = Regex("""(.*?)\[(.*?)]""")
    val match = regex.find(linkPart)
    val listName = match?.groupValues?.get(1) ?: "default"
    var uuid = match?.groupValues?.get(2) ?: ""

    if (uuid.startsWith("<") && uuid.endsWith(">")) {
        val fieldName = uuid.substring(1, uuid.length - 1)
        if (dataItem is Map<*, *>) {
            uuid = dataItem[fieldName] as? String ?: ""
        }
    }

    return listName to uuid
}

fun getPropertyFromDataItem(dataItem: Any, propertyName: String): String {
    return try {
        val prop = dataItem::class.members.firstOrNull { it.name == propertyName }
        val value = prop?.call(dataItem)?.toString()
        value ?: ""
    } catch (e: Exception) {
        ""
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun dynamicImageFromAssets(
    modifier: Modifier = Modifier,
    mainActivity: BaseComposeActivity,
    navcontroller: NavHostController,
    element: UIElement.ImageElement,
    dataItem: Any,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    var cacheName by remember { mutableStateOf("") }
    var fileName = element.src
    var _link = element.link
    var isExternal  = false
    if (element.src.startsWith("<") && element.src.endsWith(">")) {
        val fieldName = element.src.substring(1, element.src.length - 1)
        if (dataItem is Map<*, *> && fieldName.isNotEmpty()) {
            val url = dataItem[fieldName] as? String
            fileName = "$url"
            isExternal = true
        }
    }
    if(element.link.startsWith("<") && element.link.endsWith(">")) {
        val fieldName = element.link.substring(1, element.link.length - 1)
        if (dataItem is Map<*, *> && fieldName.isNotEmpty()) {
            val value = dataItem[fieldName] as? String
            _link = "$value"
        }
    }

    LaunchedEffect(Unit) {
        cacheName = withContext(Dispatchers.IO) {
            mainActivity.contentLoader.loadAsset(fileName, "images", isExternal)
        }
    }
    if (cacheName.isNotEmpty()) {
        val bitmap = loadImageFromCache(mainActivity, cacheName)
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = when(element.scale.lowercase()) {
                    "crop" -> ContentScale.Crop
                    "fit" -> ContentScale.Fit
                    "inside" -> ContentScale.Inside
                    "fillwidth" -> ContentScale.FillWidth
                    "fillbounds" -> ContentScale.FillBounds
                    "fillheight" -> ContentScale.FillHeight
                    "none" -> ContentScale.None
                    else -> ContentScale.Fit
                },
                modifier = modifier.clickable { handleButtonClick(
                    _link,
                    mainActivity = mainActivity,
                    navController = navcontroller,
                    dataItem = dataItem,
                    clickCount,
                    datasources,
                    datasourceId
                ) }
                    .padding(element.padding.left.dp, element.padding.top.dp, element.padding.right.dp,element.padding.bottom.dp)
            )
        }
    }
}

@Composable
fun asyncImage(
    modifier: Modifier = Modifier,
    mainActivity: BaseComposeActivity,
    navcontroller: NavHostController,
    element: UIElement.AsyncImageElement,
    dataItem: Any,
    clickCount: MutableState<Int>,
    datasources: MutableState<Map<String, List<Any>>>,
    datasourceId: String
) {
    var model = element.src
    var _link = element.link

    if (element.src.startsWith("<") && element.src.endsWith(">")) {
        val fieldName = element.src.substring(1, element.src.length - 1)
        if (dataItem is Map<*, *> && fieldName.isNotEmpty()) {
            val url = dataItem[fieldName] as? String
            model = "$url"
        }
    }
    if (element.link.startsWith("<") && element.link.endsWith(">")) {
        val fieldName = element.link.substring(1, element.link.length - 1)
        if (dataItem is Map<*, *> && fieldName.isNotEmpty()) {
            val value = dataItem[fieldName] as? String
            _link = "$value"
        }
    }

    AsyncImage(
        modifier = modifier.clickable { handleButtonClick(
            _link,
            mainActivity = mainActivity,
            navController = navcontroller,
            dataItem = dataItem,
            clickCount,
            datasources,
            datasourceId
        ) }
            .padding(element.padding.left.dp, element.padding.top.dp, element.padding.right.dp,element.padding.bottom.dp),
        model = model,
        contentScale = when(element.scale.lowercase()) {
            "crop" -> ContentScale.Crop
            "fit" -> ContentScale.Fit
            "inside" -> ContentScale.Inside
            "fillwidth" -> ContentScale.FillWidth
            "fillbounds" -> ContentScale.FillBounds
            "fillheight" -> ContentScale.FillHeight
            "none" -> ContentScale.None
            else -> ContentScale.Fit
        },
        contentDescription = null
    )
}



@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun dynamicSoundfromAssets(mainActivity: BaseComposeActivity, filename: String, dataItem: Any) {
    var cacheName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        cacheName = withContext(Dispatchers.IO) {
            mainActivity.contentLoader.loadAsset(filename ,"sounds")
        }
    }
    if (cacheName.isNotEmpty()) {
        val exoPlayer = remember { ExoPlayer.Builder(mainActivity).build() }
        val mediaItem = remember(filename) {
            if (filename.startsWith("http")) {
                MediaItem.fromUri(Uri.parse(cacheName))
            } else {
                val file = File(mainActivity.filesDir, cacheName)
                val uri = Uri.fromFile(file)
                MediaItem.fromUri(uri)
            }
        }
        LaunchedEffect(mediaItem) {
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun dynamicVideofromAssets(modifier: Modifier = Modifier, mainActivity: BaseComposeActivity, filename: String, dataItem: Any, height: Int = 200) {
    var cacheName by remember { mutableStateOf("") }
    if(filename.startsWith("http")) {
        cacheName = filename
    } else {
        LaunchedEffect(Unit) {
            cacheName = withContext(Dispatchers.IO) {
                mainActivity.contentLoader.loadAsset(filename, "videos")
            }
        }
    }
    if (cacheName.isNotEmpty()) {
        val exoPlayer = remember { ExoPlayer.Builder(mainActivity).build() }
        val mediaItem = remember(filename) {
            if (filename.startsWith("http")) {
                MediaItem.fromUri(Uri.parse(cacheName))
            } else {
                val file = File(mainActivity.filesDir, cacheName)
                val uri = Uri.fromFile(file)
                MediaItem.fromUri(uri)
            }
        }
        LaunchedEffect(mediaItem) {
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                }
            },
            modifier = modifier.fillMaxWidth().height(height.dp)
        )
    }
}

@Composable
fun dynamicYoutube(modifier: Modifier = Modifier, videoId: String, height: Int = 200, dataItem: Any) {
    val ctx = LocalContext.current

    AndroidView(
        modifier = modifier.height(height.dp),
        factory = {
            var view = YouTubePlayerView(it)
            val fragment = view.addYouTubePlayerListener(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(
                        youTubePlayer:
                        YouTubePlayer
                    ) {
                        super.onReady(youTubePlayer)
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }
            )
            view
        }
    )
}

fun loadImageFromCache(context: Context, filename: String): Bitmap? {
    return try {
        val file = File(context.filesDir, filename)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun dynamicScene(modifier: Modifier = Modifier, element: UIElement.SceneElement, dataItem: Any) {
    val context = LocalContext.current
    val mainActivity = context as BaseComposeActivity
    var modelCacheName by remember { mutableStateOf("") }
    var iblCacheName by remember { mutableStateOf("") }
    var skyboxCacheName by remember { mutableStateOf("") }
    val surfaceView = remember { SurfaceView(context) }
    val modelViewer = remember(surfaceView) {
        ModelViewer(surfaceView)
    }

    LaunchedEffect(element.model) {
        modelCacheName = withContext(Dispatchers.IO) {
            mainActivity.contentLoader.loadAsset(element.model, "models")
        }
    }
    LaunchedEffect(element.ibl) {
        iblCacheName = withContext(Dispatchers.IO) {
            mainActivity.contentLoader.loadAsset(element.ibl, "models")
        }
    }
    LaunchedEffect(element.skybox) {
        skyboxCacheName = withContext(Dispatchers.IO) {
            mainActivity.contentLoader.loadAsset(element.skybox, "models")
        }
    }
    if (modelCacheName.isNotEmpty() && iblCacheName.isNotEmpty() && skyboxCacheName.isNotEmpty()) {
        try {
            val modelBuffer = loadAssetFromCache(modelCacheName, context)
            if (element.model.endsWith(".gltf")) {
                modelViewer.loadModelGltf(modelBuffer) { uri ->
                    val assetCacheName = runBlocking(Dispatchers.IO) {
                        if (uri.endsWith(".bin")) {
                            mainActivity.contentLoader.loadAsset(uri, "models")
                        } else {
                            mainActivity.contentLoader.loadAsset(uri, "textures")
                        }
                    }
                    loadAssetFromCache(assetCacheName, context)
                }
            } else {
                modelViewer.loadModelGlb(modelBuffer)
            }
            modelViewer.transformToUnitCube()

            val iblBuffer = loadAssetFromCache(iblCacheName, context)
            KTX1Loader.createIndirectLight(modelViewer.engine, iblBuffer).apply {
                intensity = 50_000f
                modelViewer.scene.indirectLight = this
            }

            val skyboxBuffer = loadAssetFromCache(skyboxCacheName, context)
            KTX1Loader.createSkybox(modelViewer.engine, skyboxBuffer).apply {
                modelViewer.scene.skybox = this
            }
            surfaceView.setOnTouchListener(modelViewer)
        } catch (e: Exception) {
            println("Error on load in dynamic scene: ${e.message}")
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { surfaceView },
        modifier = modifier
            .fillMaxWidth()
            .then(if (element.width > 0) Modifier.width(element.width.dp) else Modifier)
            .then(if (element.height > 0) Modifier.height(element.height.dp) else Modifier)
    )
    val choreographer = remember { Choreographer.getInstance() }
    val startTime = remember { System.nanoTime() }
    val frameCallback = remember {
        object : Choreographer.FrameCallback {
            override fun doFrame(currentTime: Long) {
                try {
                    // Berechne die vergangene Zeit in Sekunden
                    val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
                    choreographer.postFrameCallback(this)

                    // Handle animation and rendering logic
                    modelViewer.animator?.apply {
                        if (animationCount > 0) {
                            applyAnimation(0, seconds.toFloat())
                        }
                        updateBoneMatrices()
                    }
                    modelViewer.render(currentTime)
                } catch(e: Exception) {
                    println("Error in doFrame: ${e.message}")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        choreographer.postFrameCallback(frameCallback)
        onDispose {
            choreographer.removeFrameCallback(frameCallback)
        }
    }
}

fun loadAssetFromCache(assetName: String, context: Context): ByteBuffer {
    return try {
        val asset = File(context.filesDir, assetName)
        if (asset.exists()) {
            val input = asset.inputStream()
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        } else {
            throw FileNotFoundException("Asset not found in cache: $assetName")
        }
    } catch (e: Exception) {
        println("Error loading cached asset [$assetName]: ${e.message}")
        ByteBuffer.allocate(0)
    }
}

fun loadTextAssetFromCache(assetName: String, context: Context): String {
    return try {
        val asset = File(context.filesDir, assetName)
        if (asset.exists()) {
            asset.readText()
        } else {
            throw FileNotFoundException("Asset not found in cache: $assetName")
        }
    } catch (e: Exception) {
        println("Error loading cached asset [$assetName]: ${e.message}")
        ""
    }
}

@Composable
fun translate(
    text: String,
    mainActivity: BaseComposeActivity
): String {
    var cacheName by remember { mutableStateOf("") }
    val lang = LocaleManager.getLanguage()
    val trans = "Strings-$lang.sml"
    var txt = text
    LaunchedEffect(trans) {
        cacheName = withContext(Dispatchers.IO) {
            mainActivity.contentLoader.loadAsset(trans, "translations")
        }
    }
    if (cacheName.isNotEmpty()) {

        val file = File(mainActivity.filesDir, cacheName)
        if (file.exists()) {
            val content = file.readText()
            val (parsedStrings, _) = parseSML(content)
            if (parsedStrings != null) {
                val regex =
                    Regex("""string:([A-Za-z0-9_\-]+)""")
                txt = regex.replace(txt) { matchResult ->
                    getStringValue(parsedStrings, matchResult.groupValues[1], matchResult.value)
                }
            }
        }
    }
    return txt
}
