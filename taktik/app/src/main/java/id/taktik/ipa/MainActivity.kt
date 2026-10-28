package id.taktik.ipa

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * TakTik IPA untuk Android.
 *
 * Aplikasi ini membuka versi web TakTik IPA (BuildConfig.APP_URL). Karena isinya diambil dari web,
 * setiap perubahan di GitHub langsung sampai ke HP tanpa memasang APK baru. Kalau HP belum pernah
 * online, salinan aplikasi yang dibawa di dalam APK yang dibuka.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var memakaiSalinan = false
    private var halamanSiap = false

    private val appHost: String = Uri.parse(BuildConfig.APP_URL).host ?: ""

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private val pilihBerkas = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        fileCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Halaman pembuka: logo di atas warna TakTik, tampil sampai halaman pertama selesai dimuat
        // (paling lama 3 detik, supaya HP yang lambat tidak tertahan).
        installSplashScreen().setKeepOnScreenCondition { !halamanSiap }
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(ContextCompat.getColor(this, R.color.bg))
        setContentView(web)
        web.postDelayed({ halamanSiap = true }, 3000)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            userAgentString = "$userAgentString TaktikApp/${BuildConfig.VERSION_NAME}"
        }
        web.addJavascriptInterface(AppBridge(), "TaktikApp")
        web.addJavascriptInterface(TtsBridge(), "TaktikTTS")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, TTS_SHIM, setOf("*"))
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if ((uri.scheme == "https" || uri.scheme == "http") && (uri.host == appHost || uri.host == ASSET_HOST)) return false
                bukaLuar(uri)
                return true
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            override fun onPageFinished(view: WebView, url: String?) {
                halamanSiap = true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) view.evaluateJavascript(TTS_SHIM, null)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) bukaSalinan()
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame && response.statusCode >= 400) bukaSalinan()
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = cb
                return try {
                    pilihBerkas.launch("*/*"); true
                } catch (e: ActivityNotFoundException) {
                    fileCallback = null; false
                }
            }
        }
        web.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("http")) bukaLuar(Uri.parse(url))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.__taktikBack&&window.__taktikBack())?1:0") { r ->
                    if (r != "1") finish()
                }
            }
        })

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                val id = Locale("id", "ID")
                if ((tts?.isLanguageAvailable(id) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE) tts?.language = id
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {}
                    override fun onDone(utteranceId: String) = keWeb("window.__ttsEnd&&__ttsEnd(" + JSONObject.quote(utteranceId) + ")")
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String) = keWeb("window.__ttsErr&&__ttsErr(" + JSONObject.quote(utteranceId) + ")")
                })
                keWeb("window.__ttsVoices&&__ttsVoices()")
            }
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(BuildConfig.APP_URL)
    }

    /** Belum pernah online atau situs belum tersedia: pakai salinan di dalam APK. */
    private fun bukaSalinan() {
        if (memakaiSalinan) return
        memakaiSalinan = true
        web.post { web.loadUrl("https://$ASSET_HOST/assets/www/index.html") }
    }

    private fun bukaLuar(uri: Uri) {
        try {
            val intent = if (uri.scheme == "intent") Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            else Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
        }
    }

    private fun keWeb(js: String) {
        web.post { web.evaluateJavascript(js, null) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        tts?.shutdown()
        web.destroy()
        super.onDestroy()
    }

    inner class AppBridge {
        @JavascriptInterface
        fun versi(): String = BuildConfig.VERSION_NAME
    }

    /** Pengganti speechSynthesis, karena WebView Android tidak punya fitur suara bawaan. */
    inner class TtsBridge {
        @JavascriptInterface
        fun ready(): Boolean = ttsReady

        @JavascriptInterface
        fun voices(): String {
            val arr = JSONArray()
            val t = tts
            if (ttsReady && t != null) {
                val id = Locale("id", "ID")
                if (t.isLanguageAvailable(id) >= TextToSpeech.LANG_AVAILABLE) arr.put(JSONObject().put("lang", "id-ID").put("name", "Bahasa Indonesia"))
                arr.put(JSONObject().put("lang", t.defaultVoice?.locale?.toLanguageTag() ?: "und").put("name", "Suara bawaan"))
            }
            return arr.toString()
        }

        @JavascriptInterface
        fun speak(id: String, text: String, rate: Float) {
            val t = tts
            if (!ttsReady || t == null) {
                keWeb("window.__ttsErr&&__ttsErr(" + JSONObject.quote(id) + ",'not-ready')"); return
            }
            t.setSpeechRate(if (rate > 0) rate else 1f)
            t.speak(text, TextToSpeech.QUEUE_ADD, null, id)
        }

        @JavascriptInterface
        fun stop() {
            tts?.stop()
        }
    }

    companion object {
        private const val ASSET_HOST = "appassets.androidplatform.net"

        private val TTS_SHIM = """
            (function(){
              if(window.__taktikTts||!window.TaktikTTS)return;window.__taktikTts=1;
              var q={},n=0,ls=[];
              function U(t){this.text=String(t==null?'':t);this.lang='id-ID';this.rate=1;this.voice=null;this.onend=null;this.onerror=null;}
              function left(){return Object.keys(q).length;}
              var ss={speaking:false,pending:false,onvoiceschanged:null,
                getVoices:function(){try{return JSON.parse(TaktikTTS.voices());}catch(e){return [];}},
                speak:function(u){var id='u'+(++n);q[id]=u;ss.speaking=true;ss.pending=left()>1;TaktikTTS.speak(id,u.text,+u.rate||1);},
                cancel:function(){q={};ss.speaking=false;ss.pending=false;TaktikTTS.stop();},
                pause:function(){},resume:function(){},
                addEventListener:function(t,f){if(t==='voiceschanged')ls.push(f);},
                removeEventListener:function(){}};
              function fin(id,err){var u=q[id];if(!u)return;delete q[id];ss.speaking=left()>0;ss.pending=left()>1;
                try{if(err){if(u.onerror)u.onerror({error:err});}else if(u.onend)u.onend({});}catch(e){}}
              window.__ttsEnd=function(id){fin(id,null);};
              window.__ttsErr=function(id,e){fin(id,e||'synthesis-failed');};
              window.__ttsVoices=function(){ls.forEach(function(f){try{f();}catch(e){}});if(ss.onvoiceschanged)try{ss.onvoiceschanged();}catch(e){}};
              window.SpeechSynthesisUtterance=U;
              try{Object.defineProperty(window,'speechSynthesis',{value:ss,configurable:true});}catch(e){window.speechSynthesis=ss;}
            })();
        """.trimIndent()
    }
}
