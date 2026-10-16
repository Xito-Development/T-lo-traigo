package es.unocerobits.tlotraigo.data

import android.app.Activity
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import es.unocerobits.tlotraigo.Locator
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*

@Serializable
data class Passkey(val id: String, val name: String = "Passkey", @SerialName("created_at") val createdAt: String = "", @SerialName("last_used") val lastUsed: String? = null)

/**
 * Passkeys con el Gestor de credenciales de Android: la llave se guarda en el móvil (o en el gestor de contraseñas de Google)
 * y se entra con la huella, la cara o el PIN, sin contraseña. Requiere Android 9 o superior.
 */
object Passkeys {
    val supported get() = Build.VERSION.SDK_INT >= 28

    /** Crea una passkey para la cuenta con la sesión abierta. */
    suspend fun register(act: Activity) {
        val o = Locator.api.function("passkey", buildJsonObject { put("action", "register-options") })
        val res = try {
            CredentialManager.create(act).createCredential(act, CreatePublicKeyCredentialRequest(o["options"].toString())) as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialCancellationException) { throw ApiException("Cancelado") }
          catch (e: Exception) { throw ApiException("No se pudo crear la passkey: ${e.message ?: "error del sistema"}") }
        Locator.api.function("passkey", buildJsonObject {
            put("action", "register-verify"); put("challengeId", o["challengeId"]!!); put("name", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            put("response", Locator.api.json.parseToJsonElement(res.registrationResponseJson))
        })
    }

    /** Entra con una passkey guardada (sin escribir usuario ni contraseña). */
    suspend fun login(act: Activity) {
        val o = Locator.api.function("passkey", buildJsonObject { put("action", "login-options") })
        val cred = try {
            CredentialManager.create(act).getCredential(act, GetCredentialRequest(listOf(GetPublicKeyCredentialOption(o["options"].toString())))).credential
        } catch (e: GetCredentialCancellationException) { throw ApiException("Cancelado") }
          catch (e: NoCredentialException) { throw ApiException("No tienes ninguna passkey de T lo traigo en este móvil. Entra con tu contraseña y créala en Ajustes → Cuenta.") }
          catch (e: Exception) { throw ApiException("No se pudo usar la passkey: ${e.message ?: "error del sistema"}") }
        val pk = cred as? PublicKeyCredential ?: throw ApiException("Credencial no válida")
        val r = Locator.api.function("passkey", buildJsonObject {
            put("action", "login-verify"); put("challengeId", o["challengeId"]!!); put("response", Locator.api.json.parseToJsonElement(pk.authenticationResponseJson))
        })
        Locator.api.adoptSession(r["session"]!!.jsonObject)
    }

    suspend fun list(): List<Passkey> = Locator.api.json.decodeFromString(Locator.api.rest("GET", "passkeys?select=id,name,created_at,last_used&order=created_at"))
    suspend fun delete(id: String) { Locator.api.rest("DELETE", "passkeys?id=eq.$id") }
}
