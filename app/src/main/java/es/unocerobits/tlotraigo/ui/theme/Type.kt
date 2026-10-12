package es.unocerobits.tlotraigo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import es.unocerobits.tlotraigo.R
import es.unocerobits.tlotraigo.data.AppFont

// Fuentes con licencia OFL incluidas en la app (funcionan sin Google Play Services).
private val Atkinson = FontFamily(Font(R.font.atkinson_regular, FontWeight.Normal), Font(R.font.atkinson_bold, FontWeight.Bold))
private val Andika = FontFamily(Font(R.font.andika_regular, FontWeight.Normal), Font(R.font.andika_bold, FontWeight.Bold))
private val Lato = FontFamily(Font(R.font.lato_regular, FontWeight.Normal), Font(R.font.lato_bold, FontWeight.Bold))

fun fontFamilyFor(f: AppFont): FontFamily = when (f) {
    AppFont.ATKINSON -> Atkinson; AppFont.ANDIKA -> Andika; AppFont.LATO -> Lato
    AppFont.SISTEMA -> FontFamily.Default; AppFont.CLASICA -> FontFamily.Serif
}

private val b = Typography()
private val cache = HashMap<AppFont, Typography>()

fun typographyFor(f: AppFont): Typography = cache.getOrPut(f) {
    val fam = fontFamilyFor(f)
    fun TextStyle.a(size: Int, line: Int, w: FontWeight = FontWeight.Normal, track: Double = 0.0) =
        copy(fontFamily = fam, fontSize = size.sp, lineHeight = line.sp, fontWeight = w, letterSpacing = track.sp)
    Typography(
        displaySmall = b.displaySmall.a(38, 44, FontWeight.Bold, -0.5),
        headlineLarge = b.headlineLarge.a(32, 38, FontWeight.Bold, -0.3),
        headlineMedium = b.headlineMedium.a(28, 34, FontWeight.Bold, -0.2),
        headlineSmall = b.headlineSmall.a(24, 30, FontWeight.Bold),
        titleLarge = b.titleLarge.a(22, 28, FontWeight.Bold),
        titleMedium = b.titleMedium.a(18, 24, FontWeight.Bold),
        titleSmall = b.titleSmall.a(16, 22, FontWeight.Bold),
        bodyLarge = b.bodyLarge.a(18, 27),
        bodyMedium = b.bodyMedium.a(16, 24),
        bodySmall = b.bodySmall.a(14, 20),
        labelLarge = b.labelLarge.a(17, 22, FontWeight.Bold, 0.1),
        labelMedium = b.labelMedium.a(15, 20, FontWeight.Bold),
        labelSmall = b.labelSmall.a(13, 18, FontWeight.Bold),
    )
}
