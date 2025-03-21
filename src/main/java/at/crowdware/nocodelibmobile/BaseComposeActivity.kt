package at.crowdware.nocodelibmobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import at.crowdware.nocodelibmobile.logic.LocaleManager
import at.crowdware.nocodelibmobile.utils.App
import at.crowdware.nocodelibmobile.utils.ContentLoader
import com.google.android.filament.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

abstract class BaseComposeActivity : ComponentActivity() {
    val contentLoader = ContentLoader()
    var app: App? by mutableStateOf(null)
    var loading by mutableStateOf(false)
    var cameraDistance: Float = 0F


    open fun getBaseUrl(): String {
        return "https://crowdware.github.io/FreeBookReader/app.sml"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        installCacheFromAssets()

        contentLoader.init(this)
        lifecycleScope.launch(Dispatchers.Main) {
            // load the dynamic app, we can change the content on the web server
            if (!loading) {
                loading = true
                app = contentLoader.loadApp(getBaseUrl())
            }
            if (app != null) {
                enableEdgeToEdge()
                setContent {
                    ComposeRoot()
                }
            }
        }
    }

    @Composable
    abstract fun ComposeRoot()

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    fun setNewApp(ap: App) {
        app = ap
    }

    fun openWebPage( url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun sendToAnimation(cmd: String) {
        // TODO
        if (cmd == "zoomin") {
            cameraDistance += 1.0F
            //zoomCamera(cameraDistance)
        }
        else if(cmd == "zoomout") {
            cameraDistance -= 1.0F
            //zoomCamera(cameraDistance)
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.wrapContext(newBase!!))
    }

    // this technique is used to pre install the app to the cache for faster loads
    private fun installCacheFromAssets() {
        val directory = File(this.filesDir, "ContentCache/crowdware_github_io/NoCodeLibMobile")
        var pages: File
        var parts: File
        var images: File
        var sounds: File
        var videos: File
        var models: File
        var textures: File

        if (directory.exists()) {
            return // files exists, nothing to do
        } else {
            try {
                this.assets.open("FreeBookReader/app.sml")
            } catch(e: Exception) {
                return // no pre cached data found, so we have to load via internet
            }
            directory.mkdirs()
            images = File(directory, "images")
            images.mkdir()
            sounds = File(directory, "sounds")
            sounds.mkdir()
            videos = File(directory, "videos")
            videos.mkdir()
            pages = File(directory, "pages")
            pages.mkdir()
            parts = File(directory, "parts")
            parts.mkdir()
            models = File(directory, "models")
            models.mkdir()
            textures = File(directory, "textures")
            textures.mkdir()
        }

        try {
            copyDir("images", images)
            copyDir("sounds", sounds)
            copyDir("videos", videos)
            copyDir("pages", pages)
            copyDir("parts", pages)
            copyDir("models", pages)
            copyDir("textures", pages)
            copyDir("", directory)
        } catch(e: Exception) {
            println("Error in installCacheFromAssets: ${e.message}")
        }
    }

    private fun copyDir(source: String, directory: File) {
        val files = this.assets.list(source)
        if (files != null) {
            for (file in files) {
                if (source == "")
                    copyFile("$file", file, directory)
                else
                    copyFile("$source/$file", file, directory)
            }
        }
    }

    private fun copyFile(source: String, target: String, directory: File) {
        val inputStream = this.assets.open(source)
        val outFile = File(directory, target)
        inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}