package es.unocerobits.tlotraigo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Sugerencia de dirección o pueblo (OpenStreetMap). */
data class AddrSuggestion(val street: String?, val number: String?, val town: String?, val postalCode: String?, val province: String?,
                          val lat: Double, val lon: Double, val kind: String) {
    val title get() = listOfNotNull(street?.let { s -> s + (number?.let { " $it" } ?: "") }, if (street == null) town else null).joinToString().ifBlank { town ?: "" }
    val subtitle get() = listOfNotNull(if (street != null) town else null, postalCode, province).joinToString(" · ")
}

/**
 * Autocompletado de direcciones y pueblos sin claves ni coste: Photon (OpenStreetMap) para calles y pueblos,
 * y Zippopotam para obtener el pueblo a partir del código postal.
 */
object Geo {
    private val http = OkHttpClient.Builder().connectTimeout(6, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
    // Zona de León y alrededores: las sugerencias de aquí salen primero
    private const val BBOX = "-7.2,41.8,-4.5,43.3"

    private fun get(url: String): String = http.newCall(Request.Builder().url(url).header("User-Agent", "TLoTraigo/0.6 (tlotraigo@unocerobits.com)").build())
        .execute().use { it.body?.string().orEmpty() }

    /** Pueblos de un código postal (p. ej. 24796 → Villaquejida). */
    suspend fun placesByCp(cp: String): List<AddrSuggestion> = withContext(Dispatchers.IO) {
        if (!Regex("^[0-9]{5}$").matches(cp)) return@withContext emptyList()
        runCatching {
            val o = JSONObject(get("https://api.zippopotam.us/es/$cp")); val arr = o.getJSONArray("places")
            (0 until arr.length()).map { arr.getJSONObject(it) }.map { p ->
                AddrSuggestion(null, null, p.getString("place name"), cp, p.optString("state").ifBlank { null },
                    p.optString("latitude").toDoubleOrNull() ?: 0.0, p.optString("longitude").toDoubleOrNull() ?: 0.0, "pueblo")
            }.distinctBy { it.town }
        }.getOrDefault(emptyList())
    }

    /** Calles o pueblos que empiezan por lo escrito ("Villaq" → Villaquejida, "Calle Mayor Valderas"…). */
    suspend fun search(query: String, onlyPlaces: Boolean = false): List<AddrSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim(); if (q.length < 3) return@withContext emptyList()
        if (Regex("^[0-9]{5}$").matches(q)) return@withContext placesByCp(q)
        runCatching {
            val tags = if (onlyPlaces) "&osm_tag=place:village&osm_tag=place:town&osm_tag=place:city&osm_tag=place:hamlet" else ""
            val o = JSONObject(get("https://photon.komoot.io/api/?q=${URLEncoder.encode(q, "UTF-8")}&limit=8&bbox=$BBOX$tags"))
            val f = o.getJSONArray("features")
            (0 until f.length()).mapNotNull { i ->
                val ft = f.getJSONObject(i); val p = ft.getJSONObject("properties"); val c = ft.getJSONObject("geometry").getJSONArray("coordinates")
                if (p.optString("countrycode") != "ES") return@mapNotNull null
                val type = p.optString("osm_value")
                val isPlace = p.optString("osm_key") == "place"
                AddrSuggestion(
                    street = if (isPlace) null else (p.optString("street").ifBlank { null } ?: p.optString("name").ifBlank { null }),
                    number = p.optString("housenumber").ifBlank { null },
                    town = if (isPlace) p.optString("name") else (p.optString("city").ifBlank { null } ?: p.optString("district").ifBlank { null } ?: p.optString("county").ifBlank { null }),
                    postalCode = p.optString("postcode").ifBlank { null }, province = p.optString("state").ifBlank { null },
                    lat = c.getDouble(1), lon = c.getDouble(0), kind = if (isPlace) "pueblo" else type)
            }.distinctBy { it.title + it.subtitle }
        }.getOrDefault(emptyList())
    }
}
