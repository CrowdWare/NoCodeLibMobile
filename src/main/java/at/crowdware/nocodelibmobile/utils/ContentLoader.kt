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
import android.os.Build
import androidx.annotation.RequiresApi
import at.crowdware.nocodelibmobile.BaseComposeActivity
import at.crowdware.nocodelibmobile.logic.LocaleManager
import at.crowdware.nocodelibmobile.parseApp
import at.crowdware.nocodelibmobile.parsePage
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class Link(val titel: String, val url: String)

class ContentLoader {
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var context: BaseComposeActivity
    var app: App? = null
    lateinit var appUrl: String
    private var appLoaded = false
    val links: MutableList<Link> = mutableListOf()

    // Initialize the OkHttp client and setup cache directory
    fun init(ctx: BaseComposeActivity) {
        context = ctx
        okHttpClient = OkHttpClient.Builder().build()

        val directory = File(ctx.filesDir, "ContentCache")
        if (!directory.exists()) {
            directory.mkdir()
        }

        // load link list
        val file = File(context.filesDir, "links.txt")
        if(file.exists()) {
            val list = file.readLines()
            for (line in list) {
                val values = line.split("|")
                links.add(Link(values[0], values[1]))
            }
        }
    }

    suspend fun fetchJsonData(url: String): List<Any> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response: Response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val jsonData = response.body?.string()
            val jsonArray = JSONArray(jsonData)
            val dataList = mutableListOf<Any>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val dataMap = mutableMapOf<String, Any>()
                jsonObject.keys().forEach { key ->
                    dataMap[key] = jsonObject.get(key)
                }
                dataList.add(dataMap)
            }
            dataList
        } else {
            emptyList()
        }
    }

    fun addLink(title: String, url: String) {
        val file = File(context.filesDir, "links.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.appendText("$title|$url\n")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun removeLink(title: String, url: String) {
        links.removeIf { it.titel == title && it.url == url }
        val file = File(context.filesDir, "links.txt")
        file.delete()
        file.createNewFile()
        for(link in links) {
            file.writeText("${link.titel}|${link.url}\n")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadAsset(name: String, subdir: String, isExternal: Boolean = false): String {
        if(app == null) {
            return ""
        }
        val url = "$appUrl/$subdir/$name"
        if (isExternal) {
            // in this case name is an URL
            val fileName = name.substringAfterLast("/")
            val storageName = ("ContentCache/" + appUrl.substringAfter("://") + "/$subdir/").replace(".", "_")
                    .replace(":", "_") + fileName
            val file = File(context.filesDir, storageName)
            var ret = true
            if (!file.exists()) {
                ret = loadAndCacheAsset(name, storageName, LocalDateTime.now())
            }
            return if (ret)
                storageName
            else
                ""
        } else {
            val result = app!!.deployment.files.find { it.path == name }
            if (result == null) {
                return ""
            }
            val fileName =
                ("ContentCache/" + appUrl.substringAfter("://") + "/$subdir/").replace(".", "_")
                    .replace(":", "_") + name
            val file = File(context.filesDir, fileName)
            var ret = true
            if (file.exists()) {
                val lastModifiedMillis = file.lastModified()
                val lastModifiedDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastModifiedMillis),
                    ZoneId.systemDefault()
                )
                if (result.time.isAfter(lastModifiedDateTime)) {
                    // web server version is newer
                    ret = loadAndCacheAsset(url, fileName, result.time)
                }
            } else {
                ret = loadAndCacheAsset(url, fileName, result.time)
            }
            return if (ret)
                fileName
            else
                ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadPart(name: String): String {
        if (app == null) {
            return ""
        }
        val lang = LocaleManager.getLanguage()
        val url = "$appUrl/parts/$name-$lang.md"
        val result = app!!.deployment.files.find { it.path == "$name-$lang.md" && it.type == "part"}
        if (result == null) {
            return ""
        }
        val fileName =
            ("ContentCache/" + appUrl.substringAfter("://") + "/parts/").replace(".", "_")
                .replace(":", "_") + name + "-" + lang + ".md"

        val file = File(context.filesDir, fileName)
        var ret = true
        if (file.exists()) {
            val lastModifiedMillis = file.lastModified()
            val lastModifiedDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(lastModifiedMillis),
                ZoneId.systemDefault()
            )
            if (result.time.isAfter(lastModifiedDateTime)) {
                // web server version is newer
                ret = loadAndCacheAsset(url, fileName, result.time)
            }
        } else {
            ret = loadAndCacheAsset(url, fileName, result.time)
        }
        return if (ret)
            fileName
        else
            ""
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun switchApp(url: String) {
        if(url != appUrl) {
            val app = loadApp(url+ "/app.sml")
            if(app != null) {
                context.setNewApp(app)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadPage(name: String, activity: BaseComposeActivity): Page? {
        val lang = LocaleManager.getLanguage()
        var fileContent = ""
        val url = "$appUrl/pages/$name.sml"
        if (app == null)
            return null
        val result = app!!.deployment.files.find { it.path == "$name.sml" && it.type == "page"}
        if (result == null) {
            return null
        }
        val fileName = ("ContentCache/" + appUrl.substringAfter("://") + "/pages/$name").replace(".", "_").replace(":", "_") + ".sml"
        val file = File(context.filesDir, fileName)
        fileContent = if (file.exists()) {
            val lastModifiedMillis = file.lastModified()
            val lastModifiedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModifiedMillis), ZoneId.systemDefault())
            if (result.time.isAfter(lastModifiedDateTime)) {
                // web server version is newer
                println("loadAndCache: $url, $fileName")
                loadAndCacheSml(url, fileName, result.time) ?: ""
            } else {
                // use cache version instead
                file.readText()
            }
        } else {
            println("loadAndCache: $url, $fileName")
            loadAndCacheSml(url, fileName, result.time) ?: ""
        }
        var page = parsePage(fileContent, "$name/$lang")
        if (page == null) {
            page = parsePage(
                "Page { Column { padding: \"16\" Text { color: \"#FF0000\" fontSize: 18 text:\"An error occurred loading the home page.\"}}}", name)
        }
        return page
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadAndCacheSml(url: String, fileName: String, time: LocalDateTime): String? {
        val sml = withContext(Dispatchers.IO) {
            downloadSml(url)
        }
        if (sml != null) {
            val cache = File(context.filesDir, fileName)
            // Make sure the parent directories exist
            val parentDir = cache.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            cache.writeText(sml)
            val millis = time
                .atZone(ZoneId.systemDefault()) // Convert to ZonedDateTime in the system's default time zone
                .toInstant() // Convert to Instant (which represents a moment in time)
                .toEpochMilli()
            cache.setLastModified(millis)
        }
        return sml
    }

    // Suspend function to load the app asynchronously
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadApp(url: String): App? = withContext(Dispatchers.IO) {
        var fileContent = ""

        appUrl = url.substringBefore("/app.sml")
        val fileName = "ContentCache/" + appUrl.substringAfter("://").replace(".", "_").replace(":", "_") + "/app.sml"
        val file = File(context.filesDir, fileName)
        // Make sure the parent directories exist
        val parentDir = file.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        // load last saved version in case webserver is offline
        if (file.exists()) {
            fileContent = file.readText()
        }

        if(fileContent.contains("precached")) {
            app = parseApp(fileContent)
            appLoaded = true
        } else {
            // Download content from the URL
            var appContent = downloadSml(url)
            if (appContent != null) {
                if (fileContent != appContent) {
                    // Write new content to the cache if it has changed, in case web server is offline
                    file.writeText(appContent)
                }
            } else {
                // Use cached content if available
                if (fileContent.isNotEmpty()) {
                    appContent = fileContent
                }
            }

            // Parse the app content
            if (appContent != null && appContent.isNotEmpty()) {
                app = parseApp(appContent)
                appLoaded = true
            }
        }
        return@withContext app
    }

    // Suspend function to download content asynchronously
    suspend fun downloadSml(url: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        return@withContext try {
            // Use OkHttp's enqueue for asynchronous call
            val response = okHttpClient.newCall(request).await()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadAsset(url: String): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        return@withContext try {
            val response = okHttpClient.newCall(request).await()
            if (response.isSuccessful) {
                // Read the response body as a ByteArray
                response.body?.byteStream()?.use { inputStream ->
                    inputStream.readBytes()
                }
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Extension function to convert OkHttp Call into a suspending function
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isCancelled) return
                continuation.resumeWith(Result.success(response))
            }
        })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Handle cancellation exception
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadAndCacheAsset(url: String, fileName: String, time: LocalDateTime): Boolean {
        val bytes = withContext(Dispatchers.IO) {
            downloadAsset(url)
        }
        if (bytes != null) {
            val cache = File(context.filesDir, fileName)
            val parentDir = cache.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            cache.writeBytes(bytes)
            val millis = time
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            cache.setLastModified(millis)
        }
        return true
    }
}
