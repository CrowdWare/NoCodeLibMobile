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
package at.crowdware.nocodelibmobile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import at.crowdware.nocodelibmobile.utils.App
import at.crowdware.nocodelibmobile.utils.DeploymentElement
import at.crowdware.nocodelibmobile.utils.FileElement
import at.crowdware.nocodelibmobile.utils.UIElement.ButtonElement
import at.crowdware.nocodelibmobile.utils.UIElement.ColumnElement
import at.crowdware.nocodelibmobile.utils.UIElement.ImageElement
import at.crowdware.nocodelibmobile.utils.UIElement.MarkdownElement
import at.crowdware.nocodelibmobile.utils.Padding
import at.crowdware.nocodelibmobile.utils.Page
import at.crowdware.nocodelibmobile.utils.UIElement.RowElement
import at.crowdware.nocodelibmobile.utils.UIElement.SoundElement
import at.crowdware.nocodelibmobile.utils.UIElement.SpacerElement
import at.crowdware.nocodelibmobile.utils.UIElement.TextElement
import at.crowdware.nocodelibmobile.utils.UIElement
import at.crowdware.nocodelibmobile.utils.UIElement.VideoElement
import at.crowdware.nocodelibmobile.utils.UIElement.YoutubeElement
import at.crowdware.nocodelibmobile.utils.parsePadding
import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.utils.Tuple7
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


sealed class PropertyValue {
    data class StringValue(val value: String) : PropertyValue()
    data class IntValue(val value: Int) : PropertyValue()
    data class FloatValue(val value: Float) : PropertyValue()
    data class BooleanValue(val value: Boolean) : PropertyValue()
}

val identifier: Token = regexToken("[a-zA-Z_][a-zA-Z0-9_]*")
val lBrace: Token = literalToken("{")
val rBrace: Token = literalToken("}")
val colon: Token = literalToken(":")
val stringLiteral: Token = regexToken("\"[^\"]*\"")
val whitespace: Token = regexToken("\\s+")
val integerLiteral: Token = regexToken("\\d+")
val floatLiteral = regexToken("\\d+\\.\\d+")
val booleanLiteral: Token = regexToken("true|false")

val lineComment: Token = regexToken("//.*")
val blockComment: Token = regexToken(Regex("/\\*[\\s\\S]*?\\*/", RegexOption.DOT_MATCHES_ALL))

object SmlGrammar : Grammar<List<Any>>() {
    //val whitespaceParser = zeroOrMore(whitespace)
    val commentParser = lineComment or blockComment
    val ignoredParser = zeroOrMore(whitespace or commentParser)
    val stringParser = stringLiteral.map { PropertyValue.StringValue(it.text.removeSurrounding("\"")) }
    val integerParser = integerLiteral.map { PropertyValue.IntValue(it.text.toInt()) }
    val floatParser = floatLiteral.map { PropertyValue.FloatValue(it.text.toFloat()) }
    val booleanParser = booleanLiteral.map { PropertyValue.BooleanValue(it.text.toBoolean()) }
    val propertyValue = floatParser or integerParser or booleanParser or stringParser
    val property by (ignoredParser and identifier and ignoredParser and colon and ignoredParser and propertyValue).map { (_, id, _, _, _, value) ->
        id.text to value
    }
    val elementContent: Parser<List<Any>> = zeroOrMore(property or parser { element })
    val element: Parser<Any> by ignoredParser and identifier and ignoredParser and lBrace and elementContent and ignoredParser and rBrace

    override val tokens: List<Token> = listOf(
        booleanLiteral, identifier, lBrace, rBrace, colon, stringLiteral, floatLiteral, integerLiteral,
        whitespace, lineComment, blockComment
    )
    override val rootParser: Parser<List<Any>> = (oneOrMore(element) and ignoredParser).map { (elements, _) -> elements }
}

@RequiresApi(Build.VERSION_CODES.O)
fun deserializeApp(parsedResult: List<Any>): App {
    val app = App()

    parsedResult.forEach { tuple ->
        when (tuple) {
            is Tuple7<*, *, *, *, *, *, *> -> {
                val elementName = (tuple.t2 as? TokenMatch)?.text
                val properties = extractProperties(tuple)

                when (elementName) {
                    "App" -> {
                        app.id = (properties["id"] as? PropertyValue.StringValue)?.value ?: ""
                        app.icon = (properties["icon"] as? PropertyValue.StringValue)?.value ?: ""
                        app.name = (properties["name"] as? PropertyValue.StringValue)?.value ?: ""
                        app.description = (properties["description"] as? PropertyValue.StringValue)?.value ?: ""
                        app.smlVersion = (properties["smlVersion"] as? PropertyValue.StringValue)?.value ?: ""
                        parseNestedAppElements(extractChildElements(tuple), app)
                    }
                }
            }
        }
    }
    return app
}

fun extractProperties(element: Any): Map<String, PropertyValue> {
    if (element is Tuple7<*, *, *, *, *, *, *>) {
        return (element.t5 as? List<*>)?.filterIsInstance<Pair<String, PropertyValue>>()?.toMap() ?: emptyMap()
    }
    return emptyMap()
}

fun extractChildElements(element: Any): List<Any> {
    if (element is Tuple7<*, *, *, *, *, *, *>) {
        return (element.t5 as? List<*>)?.filterIsInstance<Tuple7<*, *, *, *, *, *, *>>() ?: emptyList()
    }
    return emptyList()
}

fun deserializePage(parsedResult: List<Any>): Page {
    val page = Page(color = "", backgroundColor = "", padding = Padding(0, 0, 0, 0), false, elements = mutableListOf())

    parsedResult.forEach { tuple ->
        when (tuple) {
            is Tuple7<*, *, *, *, *, *, *> -> {
                val elementName = (tuple.t2 as? TokenMatch)?.text
                val properties = extractProperties(tuple)

                when (elementName) {
                    "Page" -> {
                        page.color = (properties["color"] as? PropertyValue.StringValue)?.value ?: ""
                        page.backgroundColor = (properties["backgroundColor"] as? PropertyValue.StringValue)?.value ?: ""
                        page.padding = parsePadding((properties["padding"] as? PropertyValue.StringValue)?.value ?: "0")
                        page.scrollable = (properties["scrollable"] as? PropertyValue.BooleanValue)?.value ?: false
                        parseNestedElements(extractChildElements(tuple), page.elements as MutableList<UIElement>)
                    }
                }
            }
        }
    }

    return page
}

fun parseNestedElements(nestedElements: List<Any>, elements: MutableList<UIElement>) {
    nestedElements.forEach { element ->
        when (element) {
            is Tuple7<*, *, *, *, *, *, *> -> {
                val elementName = (element.t2 as? TokenMatch)?.text
                val properties = extractProperties(element)
                when (elementName) {
                    "Text" -> {
                        elements.add(
                            TextElement(
                                width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                                height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                                weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            text = (properties["text"] as? PropertyValue.StringValue)?.value ?: "",
                            color = (properties["color"] as? PropertyValue.StringValue)?.value ?: "",
                            fontSize = ((properties["fontSize"] as? PropertyValue.IntValue)?.value ?: 14).sp,
                            fontWeight = when((properties["fontWeight"] as? PropertyValue.StringValue)?.value ?: "") {
                                "bold" -> { FontWeight.Bold }
                                "black" -> { FontWeight.Black }
                                "thin" -> { FontWeight.Thin }
                                "extrabold" -> { FontWeight.ExtraBold }
                                "extralight" -> { FontWeight.ExtraLight }
                                "light" -> { FontWeight.Light }
                                "medium" -> { FontWeight.Medium }
                                "semibold" -> { FontWeight.SemiBold }
                                else -> { FontWeight.Normal }
                            },
                            textAlign = when((properties["textAlign"] as? PropertyValue.StringValue)?.value ?: "") {
                                "left" -> { TextAlign.Start }
                                "center" -> { TextAlign.Center }
                                "right" -> { TextAlign.End }
                                else -> { TextAlign.Unspecified }
                            })
                        )
                    }
                    "Column" -> {
                        val col = ColumnElement(
                            padding = parsePadding((properties["padding"] as? PropertyValue.StringValue)?.value ?: "0"),
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            )
                        parseNestedElements(extractChildElements(element), col.uiElements as MutableList<UIElement>)
                        elements.add(col)
                    }
                    "Row" -> {
                        val row = RowElement(
                            padding = parsePadding((properties["padding"] as? PropertyValue.StringValue)?.value ?: "0"),
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            )
                        parseNestedElements(extractChildElements(element), row.uiElements as MutableList<UIElement>)
                        elements.add(row)
                    }
                    "Box" -> {
                        val box = UIElement.BoxElement(
                            padding = parsePadding(
                                (properties["padding"] as? PropertyValue.StringValue)?.value ?: "0"
                            ),
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                        )
                        parseNestedElements(extractChildElements(element), box.uiElements as MutableList<UIElement>)
                        elements.add(box)
                    }
                    "Markdown" -> {
                        val md = ((properties["text"] as? PropertyValue.StringValue)?.value ?: "").split("\n").joinToString("\n") { it.trim() }
                        val ele = MarkdownElement(
                            text = md,
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            color = (properties["color"] as? PropertyValue.StringValue)?.value ?: "",
                            fontSize = ((properties["fontSize"] as? PropertyValue.IntValue)?.value ?: 14).sp,
                            fontWeight = when((properties["fontWeight"] as? PropertyValue.StringValue)?.value ?: "") {
                                "bold" -> { FontWeight.Bold }
                                "black" -> { FontWeight.Black }
                                "thin" -> { FontWeight.Thin }
                                "extrabold" -> { FontWeight.ExtraBold }
                                "extralight" -> { FontWeight.ExtraLight }
                                "light" -> { FontWeight.Light }
                                "medium" -> { FontWeight.Medium }
                                "semibold" -> { FontWeight.SemiBold }
                                else -> { FontWeight.Normal }
                            },
                            textAlign = when((properties["textAlign"] as? PropertyValue.StringValue)?.value ?: "") {
                                "left" -> { TextAlign.Start }
                                "center" -> { TextAlign.Center }
                                "right" -> { TextAlign.End }
                                else -> { TextAlign.Unspecified }
                            })
                        elements.add(ele)
                    }
                    "Button" -> {
                        val btn = ButtonElement(
                            label = (properties["label"] as? PropertyValue.StringValue)?.value ?: "",
                            link = (properties["link"] as? PropertyValue.StringValue)?.value ?: "",
                            color = (properties["color"] as? PropertyValue.StringValue)?.value ?: "",
                            backgroundColor = (properties["backgroundColor"] as? PropertyValue.StringValue)?.value ?: "",
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                        )
                        elements.add(btn)
                    }
                    "Sound" -> {
                        val snd = SoundElement(src = (properties["src"] as? PropertyValue.StringValue)?.value ?: "")
                        elements.add(snd)
                    }
                    "Image" -> {
                        val img = ImageElement(
                            src = (properties["src"] as? PropertyValue.StringValue)?.value ?: "",
                            padding = parsePadding((properties["padding"] as? PropertyValue.StringValue)?.value ?: "0"),
                            scale = (properties["scale"] as? PropertyValue.StringValue)?.value ?: "1",
                            link = (properties["link"] as? PropertyValue.StringValue)?.value ?: "",
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight =  (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            align = (properties["align"] as? PropertyValue.StringValue)?.value ?: ""
                        )
                        elements.add(img)
                    }
                    "AsyncImage" -> {
                        val img = UIElement.AsyncImageElement(
                            src = (properties["src"] as? PropertyValue.StringValue)?.value ?: "",
                            padding = parsePadding((properties["padding"] as? PropertyValue.StringValue)?.value ?: "0"),
                            scale = (properties["scale"] as? PropertyValue.StringValue)?.value ?: "1",
                            link = (properties["link"] as? PropertyValue.StringValue)?.value ?: "",
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            align = (properties["align"] as? PropertyValue.StringValue)?.value ?: ""
                        )
                        elements.add(img)
                    }
                    "Spacer" -> {
                        val sp = SpacerElement(
                            amount = (properties["amount"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0
                        )
                        elements.add(sp)
                    }
                    "Video" -> {
                        val vid = VideoElement(
                            src = (properties["src"] as? PropertyValue.StringValue)?.value ?: "",
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight =  (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0
                        )
                        elements.add(vid)
                    }
                    "Youtube" -> {
                        val yt = YoutubeElement(
                            id = (properties["id"] as? PropertyValue.StringValue)?.value ?: "",
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight =  (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0
                        )
                        elements.add(yt)
                    }
                    "Scene" -> {
                        val scene = UIElement.SceneElement(
                            width = (properties["width"] as? PropertyValue.IntValue)?.value ?: 0,
                            height = (properties["height"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                            model = (properties["model"] as? PropertyValue.StringValue)?.value ?: "",
                            skybox = (properties["skybox"] as? PropertyValue.StringValue)?.value ?: "",
                            ibl = (properties["ibl"] as? PropertyValue.StringValue)?.value ?: "",
                        )
                        elements.add(scene)
                    }
                    "LazyColumn" -> {
                        // TODO: Cached SML file are not updated

                        val lc = UIElement.LazyColumnElement(
                            padding = parsePadding((properties["padding"] as? PropertyValue.StringValue)?.value ?: "0"),
                            datasource = (properties["datasource"] as? PropertyValue.StringValue)?.value ?: "",
                            filter = (properties["filter"] as? PropertyValue.StringValue)?.value ?: "",
                            order = (properties["order"] as? PropertyValue.StringValue)?.value ?: "",
                            limit = (properties["limit"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                        )
                        parseNestedElements(extractChildElements(element), lc.uiElements as MutableList<UIElement>)
                        elements.add(lc)
                    }
                    "LazyRow" -> {
                        // TODO: Cached SML file are not updated

                        val lc = UIElement.LazyRowElement(
                            padding = parsePadding((properties["padding"] as? PropertyValue.StringValue)?.value ?: "0"),
                            datasource = (properties["datasource"] as? PropertyValue.StringValue)?.value ?: "",
                            filter = (properties["filter"] as? PropertyValue.StringValue)?.value ?: "",
                            order = (properties["order"] as? PropertyValue.StringValue)?.value ?: "",
                            limit = (properties["limit"] as? PropertyValue.IntValue)?.value ?: 0,
                            weight = (properties["weight"] as? PropertyValue.IntValue)?.value ?: 0,
                        )
                        parseNestedElements(extractChildElements(element), lc.uiElements as MutableList<UIElement>)
                        elements.add(lc)
                    }
                    "LazyContent" -> {
                        val lc = UIElement.LazyContentElement()
                        parseNestedElements(extractChildElements(element), lc.uiElements as MutableList<UIElement>)
                        elements.add(lc)
                    }
                    "LazyNoContent" -> {
                        val lnc = UIElement.LazyNoContentElement()
                        parseNestedElements(extractChildElements(element), lnc.uiElements as MutableList<UIElement>)
                        elements.add(lnc)
                    }
                    else -> {
                        println("⚠️ Unhandled element type: $elementName")
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseNestedAppElements(nestedElements: List<Any>, app: App) {
    nestedElements.forEach { element ->
        when (element) {
            is Tuple7<*, *, *, *, *, *, *> -> {
                val elementName = (element.t2 as? TokenMatch)?.text
                val properties = extractProperties(element)

                println("parse: $elementName")
                when (elementName) {
                    "RestDatasource" -> {
                        app.restDatasourceId = (properties["id"] as? PropertyValue.StringValue)?.value ?: ""
                        app.restDatasourceUrl = (properties["url"] as? PropertyValue.StringValue)?.value ?: ""
                    }
                    "Deployment" -> {
                        parseNestedDeployElements(extractChildElements(element), app.deployment)
                    }
                    "Theme" -> {
                        app.theme.error = (properties["error"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.scrim = (properties["scrim"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onError = (properties["onError"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.background = (properties["background"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.errorContainer = (properties["errorContainer"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.inverseOnSurface = (properties["inverseOnSurface"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.inversePrimary = (properties["inversePrimary"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.inverseSurface = (properties["inverseSurface"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onBackground = (properties["onBackground"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onErrorContainer = (properties["onErrorContainer"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onPrimary = (properties["onPrimary"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onPrimaryContainer = (properties["onPrimaryContainer"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onSecondary = (properties["onSecondary"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onSecondaryContainer = (properties["onSecondaryContainer"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onSurface = (properties["onSurface"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onSurfaceVariant = (properties["onSurfaceVariant"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onTertiary = (properties["onTertiary"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onTertiaryContainer = (properties["onTertiaryContainer"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.outline = (properties["outline"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.outlineVariant = (properties["outlineVariant"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.primary = (properties["primary"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onPrimaryContainer = (properties["error"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.secondary = (properties["onPrimaryContainer"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.onSecondaryContainer = (properties["error"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.surfaceTint = (properties["onSecondaryContainer"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.surfaceVariant = (properties["surfaceVariant"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.tertiary = (properties["tertiary"] as? PropertyValue.StringValue)?.value ?: ""
                        app.theme.tertiaryContainer = (properties["tertiaryContainer"] as? PropertyValue.StringValue)?.value ?: ""
                    }
                    else -> {
                        println("⚠\uFE0F Unhandled element type: $elementName")
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseNestedDeployElements(nestedElements: List<Any>, deployment: DeploymentElement) {
    nestedElements.forEach { element ->
        when (element) {
            is Tuple7<*, *, *, *, *, *, *> -> {
                val elementName = (element.t2 as? TokenMatch)?.text
                val properties = extractProperties(element)

                when (elementName) {
                    "File" -> {
                        val path = (properties["path"] as? PropertyValue.StringValue)?.value ?: ""
                        val date = (properties["time"] as? PropertyValue.StringValue)?.value ?: ""
                        val type = (properties["type"] as? PropertyValue.StringValue)?.value ?: ""
                        val title = (properties["title"] as? PropertyValue.StringValue)?.value ?: ""
                        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss")
                        val dateTime = LocalDateTime.parse(date, formatter)
                        deployment.files.add(FileElement(path, dateTime, type))
                    }
                }
            }
        }
    }
}

fun parsePage(sml: String, name: String): Page? {
    if(sml.isEmpty()) {
        println("An error occurred while parsing a page [$name]. Content is empty.")
        return null
    }
    try {
        val result = SmlGrammar.parseToEnd(sml)
        return deserializePage(result)
    } catch (e: Exception) {
        println("An error occurred while parsing a page: ${e.message}\nSML: $sml")
        return null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseApp(sml: String): App? {
    if(sml.isEmpty()) {
        println("An error occurred while parsing a app. Content is empty.")
        return null
    }
    try {
        val result = SmlGrammar.parseToEnd(sml)
        return deserializeApp(result)
    } catch (e: Exception) {
        println("An error occurred while parsing an app: ${e.message}\nSML: $sml")
        return null
    }
}

fun convertTupleToSmlNode(tuple: Any): SmlNode? {
    if (tuple !is Tuple7<*, *, *, *, *, *, *>) return null

    val nameToken = tuple.t2
    val content = tuple.t5 as? List<*> ?: return null

    val name = (nameToken as? TokenMatch)?.text ?: return null

    val properties = mutableMapOf<String, PropertyValue>()
    val children = mutableListOf<SmlNode>()

    content.forEach {
        when (it) {
            is Pair<*, *> -> {
                val key = it.first as? String ?: return@forEach
                val value = it.second as? PropertyValue ?: return@forEach
                properties[key] = value
            }
            is Tuple7<*, *, *, *, *, *, *> -> {
                convertTupleToSmlNode(it)?.let { node -> children.add(node) }
            }
        }
    }

    return SmlNode(name, properties, children)
}

fun String.lineWrap(maxLen: Int): String =
    this.chunked(maxLen).joinToString("\n")

fun parseSML(sml: String): Pair<SmlNode?, String?> {
    val rootList = try {
        SmlGrammar.parseToEnd(sml)
    } catch (e: Exception) {
        return null to "ParseError: ${e.message?.lineWrap(100)}"
    }
    return rootList.firstOrNull()?.let { convertTupleToSmlNode(it) } to null
}

data class SmlNode(
    val name: String,
    val properties: Map<String, PropertyValue>,
    val children: List<SmlNode>
)

fun getStringValue(node: SmlNode, key: String, default: String): String {
    val value = node.properties[key]
    return when {
        value is PropertyValue.StringValue -> value.value
        value is PropertyValue -> {
            val type = value.javaClass.simpleName
            println("Warning: The value for '$key' is not a StringValue (found: $type). Returning default value: \"$default\"")
            default
        }
        else -> default
    }
}

fun getFloatValue(node: SmlNode, key: String, default: Float): Float {
    val value = node.properties[key]
    return when {
        value is PropertyValue.FloatValue -> value.value
        value is PropertyValue -> {
            val type = value.javaClass.simpleName
            println("Warning: The value for '$key' is not a FloatValue (found: $type). Returning default: $default")
            default
        }
        else -> default
    }
}

fun getIntValue(node: SmlNode, key: String, default: Int): Int {
    val value = node.properties[key]
    return when {
        value is PropertyValue.IntValue -> value.value
        value is PropertyValue -> {
            val type = value.javaClass.simpleName
            println("Warning: The value for '$key' is not an IntValue (found: $type). Returning default value: $default")
            default
        }
        else -> default
    }
}
fun getBoolValue(node: SmlNode, key: String, default: Boolean): Boolean {
    val value = node.properties[key]
    return when {
        value is PropertyValue.BooleanValue -> value.value
        value is PropertyValue -> {
            val type = value.javaClass.simpleName
            println("Warning: The value for '$key' is not a BooleanValue (found: $type). Returning default value: $default")
            default
        }
        else -> default
    }
}