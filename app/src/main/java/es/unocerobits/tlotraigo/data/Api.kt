package es.unocerobits.tlotraigo.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val code: Int = 0) : Exception(message)

@Serializable
data class AuthUser(val id: String, val email: String? = null)

@Serializable
data class Session(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long = 0,
    val user: AuthUser,
)

/** Cliente mínimo de Supabase (GoTrue + PostgREST) sobre OkHttp: compatible con Android 5.0. */
class SupabaseApi(context: Context, private val baseUrl: String, private val key: String) {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    // Conexiones que caducan pronto: al volver de segundo plano no se reutiliza una conexión ya cerrada por el móvil
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(okhttp3.ConnectionPool(5, 20, TimeUnit.SECONDS))
        .build()

    var session: Session? = prefs.getString("s", null)?.let { runCatching { json.decodeFromString<Session>(it) }.getOrNull() }
        private set
    val uid: String get() = session?.user?.id ?: throw ApiException("Sesión caducada", 401)

    private fun save(s: Session?) {
        session = s
        prefs.edit().apply { if (s == null) remove("s") else putString("s", json.encodeToString(s)) }.apply()
    }

    /** Permite escribir solo el usuario (p. ej. "tester") para cuentas internas @tlotraigo.app. */
    fun normalizeEmail(e: String) = e.trim().let { if ('@' in it) it else "$it@tlotraigo.app" }.lowercase()

    suspend fun signIn(email: String, password: String) {
        val body = buildJsonObject { put("email", normalizeEmail(email)); put("password", password) }
        save(json.decodeFromString<Session>(call("POST", "/auth/v1/token?grant_type=password", body.toString(), auth = false)))
    }

    /** Devuelve true si hay sesión inmediata; false si Supabase exige confirmar el correo. */
    suspend fun signUp(email: String, password: String, meta: Map<String, String>): Boolean {
        val body = buildJsonObject {
            put("email", email.trim()); put("password", password)
            putJsonObject("data") { meta.forEach { (k, v) -> put(k, v) } }
        }
        val obj = json.parseToJsonElement(call("POST", "/auth/v1/signup", body.toString(), auth = false)).jsonObject
        return if (obj["access_token"] != null) { save(json.decodeFromJsonElement<Session>(obj)); true } else false
    }

    /** Verifica el código de 6 dígitos enviado por correo (type = "email" para alta, "recovery" para contraseña). */
    suspend fun verifyOtp(email: String, token: String, type: String) {
        val body = buildJsonObject { put("email", email.trim()); put("token", token.trim()); put("type", type) }
        save(json.decodeFromString<Session>(call("POST", "/auth/v1/verify", body.toString(), auth = false)))
    }

    suspend fun resendSignupCode(email: String) {
        val body = buildJsonObject { put("type", "signup"); put("email", email.trim()) }
        call("POST", "/auth/v1/resend", body.toString(), auth = false)
    }

    suspend fun sendRecoveryCode(email: String) {
        val body = buildJsonObject { put("email", normalizeEmail(email)) }
        call("POST", "/auth/v1/recover", body.toString(), auth = false)
    }

    suspend fun updatePassword(password: String) {
        ensureFresh()
        call("PUT", "/auth/v1/user", buildJsonObject { put("password", password) }.toString())
    }

    /** Cambia la contraseña comprobando antes la actual. */
    suspend fun changePassword(current: String, new: String) {
        val email = session?.user?.email ?: throw ApiException("Sin sesión")
        try { call("POST", "/auth/v1/token?grant_type=password", buildJsonObject { put("email", email); put("password", current) }.toString(), auth = false) }
        catch (e: ApiException) { throw ApiException("La contraseña actual no es correcta") }
        updatePassword(new)
    }
    /** Cambia el correo de acceso: Supabase envía un aviso para confirmarlo. */
    suspend fun changeEmail(new: String) { ensureFresh(); call("PUT", "/auth/v1/user", buildJsonObject { put("email", new.trim().lowercase()) }.toString()) }
    /** Cierra la sesión en todos los dispositivos. */
    suspend fun signOutEverywhere() { runCatching { ensureFresh(); call("POST", "/auth/v1/logout?scope=global", "{}") }; save(null) }
    val email: String? get() = session?.user?.email

    fun signOut() = save(null)

    /** Llama a una Edge Function de Supabase (pasarela de pago). Devuelve el JSON de respuesta. */
    suspend fun function(name: String, body: JsonObject): JsonObject {
        ensureFresh()
        return withContext(Dispatchers.IO) {
            val req = Request.Builder().url("$baseUrl/functions/v1/$name").header("apikey", key)
                .header("Authorization", "Bearer ${session?.accessToken ?: key}")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()
            try {
                http.newCall(req).execute().use { r ->
                    val o = runCatching { json.parseToJsonElement(r.body?.string().orEmpty()).jsonObject }.getOrDefault(JsonObject(emptyMap()))
                    if (!r.isSuccessful) throw ApiException(o["error"]?.jsonPrimitive?.content ?: "Error ${r.code}", r.code)
                    o
                }
            } catch (e: IOException) { throw ApiException("Sin conexión. Inténtalo de nuevo.") }
        }
    }

    /** URL temporal (1 h) para ver un archivo privado de Storage (p. ej. la foto del ticket). */
    suspend fun signedUrl(bucket: String, path: String): String? = runCatching {
        ensureFresh()
        val r = call("POST", "/storage/v1/object/sign/$bucket/$path", buildJsonObject { put("expiresIn", 3600) }.toString())
        "$baseUrl/storage/v1" + json.parseToJsonElement(r).jsonObject["signedURL"]!!.jsonPrimitive.content
    }.getOrNull()

    /** Se invoca si la sesión caduca y no se puede renovar (p. ej. usuario eliminado). */
    var onSessionLost: (() -> Unit)? = null

    /** Sube un archivo a Supabase Storage y devuelve su URL pública. */
    suspend fun upload(bucket: String, path: String, bytes: ByteArray, contentType: String): String {
        ensureFresh()
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url("$baseUrl/storage/v1/object/$bucket/$path")
                .header("apikey", key).header("Authorization", "Bearer ${session?.accessToken ?: key}")
                .post(bytes.toRequestBody(contentType.toMediaType())).build()
            try {
                http.newCall(req).execute().use { r -> if (!r.isSuccessful) throw ApiException(friendly(r.body?.string().orEmpty(), r.code), r.code) }
            } catch (e: IOException) { throw ApiException("Sin conexión. No se pudo subir la imagen.") }
        }
        return "$baseUrl/storage/v1/object/public/$bucket/$path"
    }

    /** Depuración: fuerza que el token se considere caducado para probar el refresco. */
    fun debugExpireSession() { session?.let { save(it.copy(expiresAt = 0)) } }

    private suspend fun ensureFresh() = mutex.withLock {
        val s = session ?: return@withLock
        if (s.expiresAt - 60 > System.currentTimeMillis() / 1000) return@withLock
        val body = buildJsonObject { put("refresh_token", s.refreshToken) }
        try {
            save(json.decodeFromString<Session>(call("POST", "/auth/v1/token?grant_type=refresh_token", body.toString(), auth = false)))
        } catch (e: ApiException) {
            if (e.code in 400..499) { save(null); onSessionLost?.invoke() }
            throw e
        }
    }

    suspend fun rest(method: String, path: String, body: String? = null, prefer: String? = null): String {
        ensureFresh()
        return call(method, "/rest/v1/$path", body, prefer = prefer)
    }

    suspend fun rpc(fn: String, args: JsonObject = JsonObject(emptyMap())): String = rest("POST", "rpc/$fn", args.toString())

    private suspend fun call(method: String, path: String, body: String?, auth: Boolean = true, prefer: String? = null): String =
        withContext(Dispatchers.IO) {
            val token = if (auth) session?.accessToken ?: key else key
            val rb = Request.Builder().url(baseUrl + path)
                .header("apikey", key)
                .header("Authorization", "Bearer $token")
            prefer?.let { rb.header("Prefer", it) }
            val shortPath = path.substringBefore('?').take(60)
            val t0 = System.currentTimeMillis()
            if (DebugTools.extraLatencyMs > 0) delay(DebugTools.extraLatencyMs)
            if (DebugTools.failNext) {
                DebugTools.failNext = false
                DebugTools.record(HttpLog(t0, method, shortPath, 0, 0, "Fallo simulado"))
                throw ApiException("Fallo de red simulado (modo depuración).")
            }
            val payload = (body ?: "").toRequestBody("application/json".toMediaType())
            rb.method(method, if (method == "GET" || method == "DELETE" && body == null) null else payload)
            // Al volver a la app la red puede tardar un instante en estar lista: se reintenta 3 veces con espera creciente
            var attempt = 0
            while (true) {
                try {
                    return@withContext http.newCall(rb.build()).execute().use { r ->
                        val txt = r.body?.string().orEmpty()
                        val err = if (r.isSuccessful) null else friendly(txt, r.code)
                        DebugTools.record(HttpLog(t0, method, shortPath, r.code, System.currentTimeMillis() - t0, err))
                        if (err != null) throw ApiException(err, r.code)
                        txt
                    }
                } catch (e: IOException) {
                    DebugTools.record(HttpLog(t0, method, shortPath, 0, System.currentTimeMillis() - t0, e.javaClass.simpleName))
                    if (++attempt >= 3 || (method != "GET" && attempt >= 2)) throw ApiException("Sin conexión. Comprueba internet e inténtalo de nuevo.")
                    http.connectionPool.evictAll()
                    delay(600L * attempt)
                }
            }
            @Suppress("UNREACHABLE_CODE")
            ""

        }

    private fun friendly(raw: String, code: Int): String {
        val msg = runCatching {
            val o = json.parseToJsonElement(raw).jsonObject
            (o["message"] ?: o["msg"] ?: o["error_description"])?.jsonPrimitive?.content
        }.getOrNull() ?: "Error $code"
        return when {
            msg.contains("Invalid login", true) -> "Correo o contraseña incorrectos."
            msg.contains("Email not confirmed", true) -> "Confirma tu correo antes de entrar (revisa tu bandeja)."
            msg.contains("already registered", true) -> "Ese correo ya tiene cuenta. Prueba a entrar."
            msg.contains("Password should be", true) -> "La contraseña debe tener al menos 6 caracteres."
            msg.contains("JWT", true) -> "Tu sesión ha caducado. Vuelve a entrar."
            msg.contains("expired", true) || msg.contains("invalid", true) && msg.contains("token", true) ->
                "El código no es correcto o ha caducado. Pide uno nuevo."
            msg.contains("security purposes", true) -> "Espera un minuto antes de pedir otro código."
            msg.contains("rate limit", true) -> "Se han enviado demasiados correos. Inténtalo más tarde."
            msg.contains("same as the old", true) || msg.contains("should be different", true) -> "La nueva contraseña debe ser distinta de la anterior."
            else -> msg
        }
    }
}
