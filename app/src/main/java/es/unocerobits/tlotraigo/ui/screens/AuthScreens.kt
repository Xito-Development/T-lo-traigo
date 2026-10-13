package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Boot
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.ApiException
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun BrandMark(size: Int = 96) {
    val t = rememberInfiniteTransition(label = "logo")
    val bob by t.animateFloat(0f, -6f, infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse), label = "bob")
    Box(Modifier.size(size.dp).graphicsLayer { translationY = bob }.clip(CircleShape), Alignment.Center) {
        // Mismo dibujo que el icono de la app (fondo + primer plano adaptativos)
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(es.unocerobits.tlotraigo.R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(es.unocerobits.tlotraigo.R.drawable.ic_launcher_foreground), "T lo traigo",
            Modifier.fillMaxSize().graphicsLayer { scaleX = 1.3f; scaleY = 1.3f })
    }
}

@Composable
fun SplashScreen(vm: AppViewModel) {
    val boot by vm.boot.collectAsState()
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark(120)
        Spacer(Modifier.height(24.dp))
        Wordmark(width = 260.dp)
        Spacer(Modifier.height(24.dp))
        when (val b = boot) {
            is Boot.Error -> { ErrorText(b.msg); BigButton("Reintentar", { vm.start() }, icon = Icons.Filled.Refresh) }
            else -> CircularProgressIndicator()
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), label = { Text("Contraseña") }, singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Lock, null) },
        trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (show) "Ocultar" else "Mostrar") } },
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
    )
}

@Composable
fun LoginScreen(vm: AppViewModel, onRegister: () -> Unit, onVerify: (email: String, mode: String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var unconfirmed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BrandMark()
        Spacer(Modifier.height(18.dp))
        Wordmark(width = 250.dp)
        Spacer(Modifier.height(18.dp))
        Text("La compra del súper,\nen la puerta de casa.", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Para los pueblos de León. Tú eliges, un vecino te lo trae.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Email, null) }, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        PasswordField(pass) { pass = it }
        ErrorText(err)
        if (unconfirmed) TextButton({ onVerify(Locator.api.normalizeEmail(email), "signup") }) { Text("Tengo un código de verificación") }
        Spacer(Modifier.height(16.dp))
        BigButton("Entrar", {
            scope.launch {
                busy = true; err = null; unconfirmed = false
                try { Locator.api.signIn(email, pass); vm.start() }
                catch (e: ApiException) { err = e.message; unconfirmed = e.message?.startsWith("Confirma") == true }
                busy = false
            }
        }, enabled = email.isNotBlank() && pass.length >= 6, loading = busy, icon = Icons.AutoMirrored.Filled.Login)
        TextButton({
            if (email.isBlank()) err = "Escribe arriba tu correo y vuelve a pulsar aquí."
            else scope.launch {
                busy = true; err = null
                try { Locator.api.sendRecoveryCode(email); onVerify(Locator.api.normalizeEmail(email), "recovery") } catch (e: ApiException) { err = e.message }
                busy = false
            }
        }, Modifier.align(Alignment.CenterHorizontally)) { Text("He olvidado mi contraseña") }
        Spacer(Modifier.height(10.dp))
        BigButton("Crear una cuenta nueva", onRegister, tonal = true, icon = Icons.Filled.PersonAdd)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(vm: AppViewModel, onBack: () -> Unit, onVerify: (email: String, mode: String) -> Unit) {
    var role by rememberSaveable { mutableStateOf("cliente") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {
        TopAppBar({ Text("Crear cuenta") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } })
    }) { pad ->
        Column(Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            SectionTitle("¿Cómo vas a usar la app?")
            ChoiceRow("Quiero recibir la compra", "Para mí o para un familiar mayor", role == "cliente", icon = Icons.Filled.Home) { role = "cliente" }
            ChoiceRow("Quiero repartir", "Hago la compra y la llevo a los vecinos", role == "repartidor", icon = Icons.Filled.LocalShipping) { role = "repartidor" }
            SectionTitle("Tus datos")
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre y apellidos") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Person, null) }, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } }, Modifier.fillMaxWidth(), label = { Text("Teléfono") },
                singleLine = true, leadingIcon = { Icon(Icons.Filled.Phone, null) }, textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Email, null) }, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(Modifier.height(12.dp))
            PasswordField(pass) { pass = it }
            Text("Mínimo 6 caracteres. Te enviaremos un código a tu correo para confirmar que es tuyo.", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ErrorText(err)
            Spacer(Modifier.height(16.dp))
            BigButton("Crear cuenta", {
                scope.launch {
                    busy = true; err = null
                    try {
                        val logged = Locator.api.signUp(email, pass, mapOf("full_name" to name, "phone" to phone, "role" to role))
                        if (logged) vm.start() else onVerify(email.trim().lowercase(), "signup")
                    } catch (e: ApiException) { err = e.message }
                    busy = false
                }
            }, enabled = name.isNotBlank() && email.contains('@') && pass.length >= 6, loading = busy, icon = Icons.Filled.Check)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileForm(vm: AppViewModel, title: String, cta: String, onSaved: () -> Unit) {
    val p by vm.profile.collectAsState()
    val towns by vm.towns.collectAsState()
    var name by rememberSaveable(p?.id) { mutableStateOf(p?.fullName.orEmpty()) }
    var phone by rememberSaveable(p?.id) { mutableStateOf(p?.phone.orEmpty()) }
    var address by rememberSaveable(p?.id) { mutableStateOf(p?.address.orEmpty()) }
    var town by rememberSaveable(p?.id) { mutableStateOf(p?.townId) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre y apellidos") }, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Teléfono") }, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        TownPicker(towns, town) { town = it.id }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Dirección (calle, número, piso…)") },
            minLines = 2, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        ErrorText(err)
        Spacer(Modifier.height(16.dp))
        BigButton(cta, {
            scope.launch {
                busy = true; err = null
                try { Locator.repo.updateProfile(name, phone, address, town); vm.refreshProfile(); onSaved() }
                catch (e: ApiException) { err = e.message }
                busy = false
            }
        }, enabled = name.isNotBlank() && town != null && address.isNotBlank(), loading = busy, icon = Icons.Filled.Save)
    }
}

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Un último paso", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        val driver = vm.profile.collectAsState().value?.isDriver == true
        ProfileForm(vm, if (driver) "¿Dónde vives y repartes?" else "¿Dónde te llevamos la compra?", "Guardar y continuar") { vm.start() }
        Spacer(Modifier.height(12.dp))
        TextButton({ vm.logout() }, Modifier.align(Alignment.CenterHorizontally)) { Text("Salir") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(vm: AppViewModel, email: String, mode: String, onBack: () -> Unit) {
    val recovery = mode == "recovery"
    var code by rememberSaveable { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var verified by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var cooldown by remember { mutableIntStateOf(60) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(cooldown) { if (cooldown > 0) { delay(1000); cooldown-- } }

    Scaffold(topBar = {
        TopAppBar({ Text(if (recovery) "Nueva contraseña" else "Confirmar correo") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } })
    }) { pad ->
        Column(Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (verified) Icons.Filled.LockReset else Icons.Filled.MarkEmailUnread, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(verified, label = "verify") { done ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!done) {
                        Text("Revisa tu correo", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Hemos enviado un código a\n$email", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        OutlinedTextField(
                            code, { code = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(),
                            label = { Text("Código") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                            textStyle = MaterialTheme.typography.headlineLarge.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        )
                        ErrorText(err)
                        info?.let { Text(it, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.height(16.dp))
                        BigButton("Confirmar código", {
                            scope.launch {
                                busy = true; err = null
                                try {
                                    Locator.api.verifyOtp(email, code, if (recovery) "recovery" else "email")
                                    if (recovery) verified = true else vm.start()
                                } catch (e: ApiException) { err = e.message }
                                busy = false
                            }
                        }, enabled = code.length >= 6, loading = busy, icon = Icons.Filled.Check)
                        TextButton({
                            scope.launch {
                                err = null
                                try {
                                    if (recovery) Locator.api.sendRecoveryCode(email) else Locator.api.resendSignupCode(email)
                                    info = "Te hemos enviado un código nuevo."; cooldown = 60
                                } catch (e: ApiException) { err = e.message }
                            }
                        }, enabled = cooldown == 0) { Text(if (cooldown > 0) "Reenviar código en $cooldown s" else "Reenviar código") }
                        Text("¿No te llega? Mira en la carpeta de correo no deseado.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    } else {
                        Text("Elige tu nueva contraseña", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        PasswordField(newPass) { newPass = it }
                        ErrorText(err)
                        Spacer(Modifier.height(16.dp))
                        BigButton("Guardar contraseña", {
                            scope.launch {
                                busy = true; err = null
                                try { Locator.api.updatePassword(newPass); vm.start() } catch (e: ApiException) { err = e.message }
                                busy = false
                            }
                        }, enabled = newPass.length >= 6, loading = busy, icon = Icons.Filled.Save)
                    }
                }
            }
        }
    }
}

/** Aceptación de condiciones y privacidad (se guarda versión y fecha: prueba del consentimiento, art. 7.1 RGPD). */
@Composable
fun TermsScreen(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val driver = vm.profile.collectAsState().value?.isDriver == true
    var ok by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val base = es.unocerobits.tlotraigo.BuildConfig.WEB_URL.trimEnd('/')
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Wordmark(width = 200.dp)
        Spacer(Modifier.height(20.dp))
        Text("Antes de empezar", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Para usar T lo traigo necesitamos que leas y aceptes estas condiciones. Resumen:", style = MaterialTheme.typography.bodyLarge)
        listOf(
            "Pagas el precio del súper más el servicio y el envío que ves antes de pedir.",
            "Puedes cancelar el pedido mientras ningún repartidor lo haya aceptado.",
            "Los productos frescos no admiten devolución, pero si algo llega mal te lo solucionamos.",
            "Usamos tus datos solo para gestionar tus pedidos; nunca los vendemos.",
            if (driver) "Como repartidor, tu ubicación solo se comparte mientras llevas un pedido." else "El repartidor verá tu nombre, teléfono y dirección para entregarte la compra.",
        ).forEach { Text("• $it", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(12.dp))
        TextButton({ ctx.openUrl("$base/condiciones.html") }) { Text("Leer las condiciones de uso") }
        if (driver) TextButton({ ctx.openUrl("$base/repartidores.html") }) { Text("Leer las condiciones para repartidores") }
        TextButton({ ctx.openUrl("$base/privacidad.html") }) { Text("Leer la política de privacidad") }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(ok, { ok = it })
            Text("He leído y acepto las condiciones de uso y la política de privacidad, y soy mayor de edad.", style = MaterialTheme.typography.bodyMedium)
        }
        ErrorText(err)
        Spacer(Modifier.height(12.dp))
        BigButton("Aceptar y continuar", {
            scope.launch {
                busy = true; err = null
                try { Locator.repo.acceptTerms(es.unocerobits.tlotraigo.TERMS_VERSION); vm.start() } catch (e: ApiException) { err = e.message }
                busy = false
            }
        }, enabled = ok, loading = busy, icon = Icons.Filled.Check)
        TextButton({ vm.logout() }, Modifier.align(Alignment.CenterHorizontally)) { Text("Salir") }
    }
}
