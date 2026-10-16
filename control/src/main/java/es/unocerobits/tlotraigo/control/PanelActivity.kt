package es.unocerobits.tlotraigo.control

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.webkit.WebViewAssetLoader
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** Panel de control (superadministrador): la interfaz del programa de Windows dentro de un WebView seguro. */
class PanelActivity : Activity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            setBackgroundColor(0xFF1F4D3F.toInt())
            addJavascriptInterface(Bridge(this@PanelActivity, this), "AndroidTLT")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) = loader.shouldInterceptRequest(request.url)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.host == "appassets.androidplatform.net") return false
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }; return true
                }
            }
        }
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/panel/index.html")
        checkUpdate()
    }

    /** Al abrir: si hay una versión nueva del panel en GitHub, la ofrece y la instala. */
    private fun checkUpdate() = Executors.newSingleThreadExecutor().execute {
        runCatching {
            val http = OkHttpClient()
            val rel = http.newCall(Request.Builder().url("https://api.github.com/repos/Xito-Development/T-lo-traigo/releases/latest")
                .header("User-Agent", "TLoTraigo-Control").build()).execute().use { JSONObject(it.body!!.string()) }
            val tag = rel.getString("tag_name").removePrefix("v")
            val assets = rel.getJSONArray("assets")
            val url = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name") == "TLoTraigo-Control.apk" }?.getString("browser_download_url") ?: return@runCatching
            val cur = packageManager.getPackageInfo(packageName, 0).versionName ?: "0"
            if (!newer(tag, cur)) return@runCatching
            runOnUiThread {
                android.app.AlertDialog.Builder(this).setTitle("Nueva versión del panel: $tag").setMessage("Tienes la $cur. ¿Actualizar ahora?")
                    .setPositiveButton("Actualizar") { _, _ -> download(url, tag) }.setNegativeButton("Más tarde", null).show()
            }
        }
    }
    private fun newer(a: String, b: String): Boolean {
        val x = a.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        val y = b.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        for (i in 0..2) if (x[i] != y[i]) return x[i] > y[i]
        return false
    }
    private fun download(url: String, tag: String) {
        val dlg = android.app.ProgressDialog(this).apply { setTitle("Descargando $tag…"); setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL); max = 100; setCancelable(false); show() }
        Executors.newSingleThreadExecutor().execute {
            val res = runCatching {
                val dir = java.io.File(cacheDir, "updates").apply { mkdirs() }; val f = java.io.File(dir, "TLoTraigo-Control.apk")
                OkHttpClient().newCall(Request.Builder().url(url).header("User-Agent", "TLoTraigo-Control").build()).execute().use { r ->
                    val total = r.body!!.contentLength(); var done = 0L
                    r.body!!.byteStream().use { inp -> f.outputStream().use { out -> val buf = ByteArray(65536)
                        while (true) { val n = inp.read(buf); if (n < 0) break; out.write(buf, 0, n); done += n
                            if (total > 0) runOnUiThread { dlg.progress = (done * 100 / total).toInt() } } } }
                }
                f
            }
            runOnUiThread {
                dlg.dismiss()
                res.onSuccess { f ->
                    if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                        android.widget.Toast.makeText(this, "Permite instalar actualizaciones y vuelve a pulsar «Actualizar»", android.widget.Toast.LENGTH_LONG).show()
                        startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))); return@onSuccess
                    }
                    val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.files", f)
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { android.widget.Toast.makeText(this, "No se pudo descargar: ${it.message}", android.widget.Toast.LENGTH_LONG).show() }
            }
        }
    }

    @Deprecated("Botón atrás: cierra modales o vuelve al resumen antes de salir")
    override fun onBackPressed() {
        web.evaluateJavascript("(window.__tltBack && window.__tltBack()) ? 'si' : 'no'") { r -> if (r?.contains("si") != true) finish() }
    }
}

/** Puente con la interfaz: mismas funciones que el preload del programa de Windows (window.tlt). */
class Bridge(private val act: Activity, private val web: WebView) {
    private val io = Executors.newFixedThreadPool(4)
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()
    private val prefs = act.getSharedPreferences("panel", Context.MODE_PRIVATE)

    private fun reply(id: String, json: String) = act.runOnUiThread { web.evaluateJavascript("window.__tltCb && window.__tltCb(${JSONObject.quote(id)}, $json)", null) }

    // ---- Sesión cifrada con el almacén de claves de Android (AES-GCM)
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry("panel_sesion", null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("panel_sesion", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @JavascriptInterface fun sessionSave(json: String): Boolean = runCatching {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val out = c.iv + c.doFinal(json.toByteArray())
        prefs.edit().putString("sesion", Base64.encodeToString(out, Base64.NO_WRAP)).apply(); true
    }.getOrDefault(false)
    @JavascriptInterface fun sessionLoad(): String? = runCatching {
        val b = Base64.decode(prefs.getString("sesion", null) ?: return null, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b, 0, 12)) }
        String(c.doFinal(b, 12, b.size - 12))
    }.getOrNull()
    @JavascriptInterface fun sessionClear() { prefs.edit().remove("sesion").apply() }

    // ---- Ajustes
    @JavascriptInterface fun prefsGet(): String = prefs.getString("ajustes", "{}") ?: "{}"
    @JavascriptInterface fun prefsSet(json: String) { prefs.edit().putString("ajustes", json).apply() }

    // ---- Comprobaciones de red (sin limitaciones del navegador)
    @JavascriptInterface fun http(id: String, url: String, optsJson: String) {
        io.execute {
            val t0 = System.currentTimeMillis()
            val res = runCatching {
                val o = JSONObject(optsJson.ifBlank { "{}" }); val m = o.optString("method", "GET")
                val rb = Request.Builder().url(url)
                o.optJSONObject("headers")?.let { h -> h.keys().forEach { k -> rb.header(k, h.getString(k)) } }
                rb.method(m, if (m == "GET" || m == "HEAD") null else ByteArray(0).toRequestBody(null))
                http.newCall(rb.build()).execute().use { r -> JSONObject().put("ok", true).put("status", r.code).put("ms", System.currentTimeMillis() - t0) }
            }.getOrElse { e -> JSONObject().put("ok", false).put("error", e.message ?: "Sin conexión").put("ms", System.currentTimeMillis() - t0) }
            reply(id, res.toString())
        }
    }
    @JavascriptInterface fun ssl(id: String, host: String) {
        io.execute {
            val res = runCatching {
                (SSLSocketFactory.getDefault().createSocket(host, 443) as SSLSocket).use { s ->
                    s.soTimeout = 8000; s.startHandshake()
                    val c = s.session.peerCertificates.first() as X509Certificate
                    val org = Regex("O=([^,]+)").find(c.issuerX500Principal.name)?.groupValues?.get(1)
                    JSONObject().put("ok", true).put("validTo", SimpleDateFormat("MMM d HH:mm:ss yyyy 'GMT'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("GMT") }.format(c.notAfter))
                        .put("issuer", org).put("authorized", true)
                }
            }.getOrElse { e -> JSONObject().put("ok", false).put("error", e.message ?: "Sin respuesta") }
            reply(id, res.toString())
        }
    }

    // ---- Avisos y utilidades
    @JavascriptInterface fun notify(title: String, body: String) {
        val nm = act.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("avisos", "Avisos del panel", NotificationManager.IMPORTANCE_HIGH))
        val pi = PendingIntent.getActivity(act, 0, Intent(act, PanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        runCatching {
            @Suppress("MissingPermission")
            NotificationManagerCompat.from(act).notify((System.currentTimeMillis() % 100000).toInt(), NotificationCompat.Builder(act, "avisos")
                .setSmallIcon(R.drawable.ic_launcher_monochrome).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pi).build())
        }
    }
    @JavascriptInterface fun open(url: String) { if (url.startsWith("https://") || url.startsWith("mailto:")) act.runOnUiThread { runCatching { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } } }
    @JavascriptInterface fun version(): String = act.packageManager.getPackageInfo(act.packageName, 0).versionName ?: "1.0"
    @JavascriptInterface fun win(a: String) { if (a == "close") act.runOnUiThread { act.finish() } }
}
