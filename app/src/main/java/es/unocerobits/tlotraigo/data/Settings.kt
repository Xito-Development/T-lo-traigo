package es.unocerobits.tlotraigo.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("ajustes")

enum class ThemeMode(val label: String) {
    SISTEMA("Automático"), CLARO("Claro"), OSCURO("Oscuro");
    fun isDark(system: Boolean) = when (this) { SISTEMA -> system; CLARO -> false; OSCURO -> true }
}

/** Paletas amables: color principal, secundario y de acento (tono ~40, legible sobre blanco). */
enum class Palette(val label: String, val primary: Long, val secondary: Long, val tertiary: Long) {
    MONTANA("Montaña", 0xFF2E6B57, 0xFF8A6200, 0xFF9A4338),
    MAR("Mar", 0xFF1F6391, 0xFF3F6F7A, 0xFF8A5A2B),
    LAVANDA("Lavanda", 0xFF6A55A3, 0xFF8A5274, 0xFF3F7A6A),
    MELOCOTON("Melocotón", 0xFFB0532F, 0xFF7A5A48, 0xFF5F6B2E),
    GIRASOL("Girasol", 0xFF7C6000, 0xFF6E5E3E, 0xFF3E6D52),
    ROSA("Rosa", 0xFFA23E66, 0xFF7A5660, 0xFF7E5A2C),
    PIZARRA("Pizarra", 0xFF3F5A73, 0xFF5B6070, 0xFF7A5470),
}

enum class AppFont(val label: String, val hint: String) {
    ATKINSON("Atkinson Hyperlegible", "Diseñada para personas con baja visión"),
    ANDIKA("Andika", "Letras muy claras, pensada para leer con facilidad"),
    LATO("Lato", "Moderna y suave"),
    SISTEMA("Letra del móvil", "La misma que usa tu teléfono"),
    CLASICA("Clásica", "Con remates, como la de un periódico"),
}

data class UiSettings(
    val mode: ThemeMode = ThemeMode.SISTEMA,
    val palette: Palette = Palette.MONTANA,
    val highContrast: Boolean = false,
    val font: AppFont = AppFont.ATKINSON,
    val senior: Boolean = true,
    val textScale: Float = 1.15f,
    val speak: Boolean = true,
)

class SettingsStore(private val ctx: Context) {
    private val kTheme = stringPreferencesKey("theme")
    private val kPalette = stringPreferencesKey("palette")
    private val kContrast = booleanPreferencesKey("contrast")
    private val kFont = stringPreferencesKey("font")
    private val kSenior = booleanPreferencesKey("senior")
    private val kScale = floatPreferencesKey("scale")
    private val kSpeak = booleanPreferencesKey("speak")

    private inline fun <reified E : Enum<E>> parse(v: String?, def: E): E =
        v?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: def

    val flow: Flow<UiSettings> = ctx.store.data.map { p ->
        val legacyContrast = p[kTheme] == "CONTRASTE" // versión 0.1/0.2
        UiSettings(
            mode = parse(p[kTheme], ThemeMode.SISTEMA),
            palette = parse(p[kPalette], Palette.MONTANA),
            highContrast = p[kContrast] ?: legacyContrast,
            font = parse(p[kFont], AppFont.ATKINSON),
            senior = p[kSenior] ?: true,
            textScale = p[kScale] ?: 1.15f,
            speak = p[kSpeak] ?: true,
        )
    }

    suspend fun save(s: UiSettings) {
        ctx.store.edit {
            it[kTheme] = s.mode.name; it[kPalette] = s.palette.name; it[kContrast] = s.highContrast; it[kFont] = s.font.name
            it[kSenior] = s.senior; it[kScale] = s.textScale; it[kSpeak] = s.speak
        }
    }
}
