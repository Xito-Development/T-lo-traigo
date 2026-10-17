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
import es.unocerobits.tlotraigo.data.composeAddress
import es.unocerobits.tlotraigo.data.Profile
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
fun SplashScreen(vm: AppViewModel, onAnimationDone: () -> Unit = {}) {
    val boot by vm.boot.collectAsState()
    var done by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        AnimatedSplash { done = true; onAnimationDone() }
        // Si tarda la conexión o falla, se muestra debajo sin estropear la animación
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (val b = boot) {
                is Boot.Error -> { Text(b.msg, color = androidx.compose.ui.graphics.Color.White); Spacer(Modifier.height(8.dp)); BigButton("Reintentar", { vm.start() }, icon = Icons.Filled.Refresh) }
                Boot.Loading -> if (done) CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFFF2B544))
                else -> {}
            }
        }
    }
}

@Composable
private fun PasswordField(value: String, label: String = "Contraseña", onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
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
        Text("Prototipo de demostración · Para los pueblos de León. Tú eliges, un vecino te lo trae.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (es.unocerobits.tlotraigo.data.Passkeys.supported) {
            val act = androidx.compose.ui.platform.LocalContext.current.findActivity() ?: return@Column
            Spacer(Modifier.height(4.dp))
            OutlinedButton({ scope.launch {
                busy = true; err = null
                try { es.unocerobits.tlotraigo.data.Passkeys.login(act); vm.start() } catch (e: ApiException) { if (e.message != "Cancelado") err = e.message }
                busy = false
            } }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !busy, shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Filled.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Entrar con passkey (huella o cara)") }
        }
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
    var pass2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val emailOk = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$").matches(email.trim())
    val phoneDigits = phone.filter(Char::isDigit)
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
                shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneDigits.isNotEmpty() && phoneDigits.length < 9,
                supportingText = { Text(if (phoneDigits.isNotEmpty() && phoneDigits.length < 9) "Faltan cifras: son 9" else "Para que el repartidor pueda llamarte") })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Email, null) }, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = email.isNotBlank() && !emailOk,
                supportingText = { Text(if (email.isNotBlank() && !emailOk) "Revisa el correo (ej.: nombre@gmail.com)" else "Te enviaremos un código para confirmar que es tuyo") })
            Spacer(Modifier.height(8.dp))
            PasswordField(pass) { pass = it }
            Spacer(Modifier.height(8.dp))
            PasswordField(pass2, label = "Repite la contraseña") { pass2 = it }
            PasswordChecklist(pass, pass2)
            Text("Consejo: una frase fácil de recordar con un número, como «Mipueblo2026».", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }, enabled = name.isNotBlank() && emailOk && passwordOk(pass) && pass == pass2, loading = busy, icon = Icons.Filled.Check)
            if (!(name.isNotBlank() && emailOk && passwordOk(pass) && pass == pass2)) Text(listOfNotNull(if (name.isBlank()) "tu nombre" else null, if (!emailOk) "un correo válido" else null,
                if (!passwordOk(pass)) "una contraseña segura" else null, if (passwordOk(pass) && pass != pass2) "repetir la contraseña igual" else null).let { "Falta: " + it.joinToString(", ") },
                Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Datos y dirección de entrega paso a paso: nombre → pueblo (CP y provincia) → calle y número → piso e indicaciones → revisar. */
@Composable
fun ProfileForm(vm: AppViewModel, title: String, cta: String, onBack: (() -> Unit)? = null, onSaved: () -> Unit) {
    val p by vm.profile.collectAsState()
    val towns by vm.towns.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(p?.fullName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(p?.phone.orEmpty()) }
    var recipient by rememberSaveable { mutableStateOf(p?.recipientName ?: p?.fullName.orEmpty()) }
    var town by rememberSaveable { mutableStateOf(p?.townId) }
    var cp by rememberSaveable { mutableStateOf(p?.postalCode.orEmpty()) }
    var province by rememberSaveable { mutableStateOf(p?.province ?: "León") }
    var street by rememberSaveable { mutableStateOf(p?.street.orEmpty()) }
    var number by rememberSaveable { mutableStateOf(p?.streetNumber.orEmpty()) }
    var floor by rememberSaveable { mutableStateOf(p?.floorDoor.orEmpty()) }
    var directions by rememberSaveable { mutableStateOf(p?.directions.orEmpty()) }
    // Cuando el perfil llega después de abrir la pantalla, se rellenan solo los campos que siguen vacíos
    LaunchedEffect(p?.id) { p?.let { pp ->
        if (name.isBlank()) name = pp.fullName; if (phone.isBlank()) phone = pp.phone.orEmpty(); if (recipient.isBlank()) recipient = pp.recipientName ?: pp.fullName
        if (town == null) town = pp.townId; if (cp.isBlank()) cp = pp.postalCode.orEmpty(); if (street.isBlank()) street = pp.street.orEmpty()
        if (number.isBlank()) number = pp.streetNumber.orEmpty(); if (floor.isBlank()) floor = pp.floorDoor.orEmpty(); if (directions.isBlank()) directions = pp.directions.orEmpty()
    } }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val townName = towns.firstOrNull { it.id == town }?.name
    val draft = (p ?: Profile(id = "")).copy(recipientName = recipient, street = street, streetNumber = number, floorDoor = floor,
        postalCode = cp, province = province, directions = directions, townId = town)
    val steps = listOf("Tus datos", "Pueblo", "Calle", "Piso e indicaciones", "Revisar")
    val valid = when (step) {
        0 -> name.isNotBlank() && phone.filter(Char::isDigit).length >= 9
        1 -> town != null && (cp.isBlank() || Regex("^[0-9]{5}$").matches(cp)) &&
            towns.firstOrNull { it.id == town }?.postalCode.let { tc -> tc == null || cp.isBlank() || cp == tc }
        2 -> street.isNotBlank() && number.isNotBlank()
        else -> true
    }
    @Composable fun field(v: String, set: (String) -> Unit, label: String, kb: KeyboardType = KeyboardType.Text, cap: KeyboardCapitalization = KeyboardCapitalization.Sentences, ph: String? = null) =
        OutlinedTextField(v, set, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text(label) }, singleLine = true,
            placeholder = ph?.let { { Text(it) } }, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = kb, capitalization = cap))
    Column(Modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) Text(title, style = MaterialTheme.typography.headlineMedium)
        Text("Paso ${step + 1} de ${steps.size}: ${steps[step]}", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        LinearProgressIndicator({ (step + 1f) / steps.size }, Modifier.fillMaxWidth().padding(vertical = 8.dp))
        androidx.activity.compose.BackHandler(step > 0 || onBack != null) { if (step > 0) step-- else onBack?.invoke() }   // atrás del sistema = paso anterior
        AnimatedContent(step, transitionSpec = { (slideInHorizontally { (if (targetState > initialState) 1 else -1) * it / 4 } + fadeIn(tween(220))) togetherWith ExitTransition.None },
            label = "addr") { st ->
            Column {
                when (st) {
                    0 -> {
                        field(name, { name = it }, "Tu nombre y apellidos", cap = KeyboardCapitalization.Words)
                        field(phone, { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } }, "Teléfono de contacto", KeyboardType.Phone)
                        field(recipient, { recipient = it }, "¿A nombre de quién se entrega?", cap = KeyboardCapitalization.Words, ph = "Si es para otra persona, su nombre")
                        var redeem by remember { mutableStateOf(false) }
                        if (redeem) RedeemInviteDialog(vm) { redeem = false; vm.refreshProfile() }
                        TextButton({ redeem = true }, Modifier.padding(top = 4.dp)) { Icon(Icons.Filled.VpnKey, null); Spacer(Modifier.width(6.dp)); Text("Tengo un código de invitación") }
                    }
                    1 -> {
                        Spacer(Modifier.height(10.dp))
                        TownPicker(towns, town) { t -> town = t.id; t.postalCode?.let { cp = it } }
                        field(cp, { v -> cp = v.filter(Char::isDigit).take(5)
                            if (cp.length == 5) towns.firstOrNull { it.postalCode == cp }?.let { town = it.id } }, "Código postal", KeyboardType.Number,
                            ph = "Al escribirlo elegimos tu pueblo")
                        val townCp = towns.firstOrNull { it.id == town }?.postalCode
                        if (cp.length == 5 && townCp != null && cp != townCp) Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("El código postal de ${townName.orEmpty()} es $townCp.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            TextButton({ cp = townCp }) { Text("Usar $townCp") } }
                        else if (cp.length == 5 && !cp.startsWith("24")) Text("Ese código no es de la provincia de León.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        field(province, { province = it }, "Provincia", cap = KeyboardCapitalization.Words)
                    }
                    2 -> {
                        AddressSuggestField(street, { street = it }, "Calle, plaza o camino", onPick = { sg ->
                            street = sg.street ?: street; sg.number?.let { number = it }; sg.postalCode?.let { if (cp.isBlank()) cp = it }
                        }, context = townName.orEmpty(), placeholder = "Escribe y elige: «Calle Mayor»")
                        field(number, { number = it.take(10) }, "Número", ph = "Ej.: 12 o s/n")
                    }
                    3 -> {
                        field(floor, { floor = it.take(30) }, "Piso, puerta o bloque (opcional)", ph = "Ej.: 1º B")
                        OutlinedTextField(directions, { directions = it.take(200) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Indicaciones para el repartidor (opcional)") },
                            placeholder = { Text("Ej.: casa con portón verde, llamar fuerte") }, minLines = 2, shape = MaterialTheme.shapes.medium)
                    }
                    else -> Surface(Modifier.fillMaxWidth().padding(top = 10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(16.dp)) {
                            Text(recipient.ifBlank { name }, style = MaterialTheme.typography.titleMedium)
                            Text(composeAddress(draft, townName), style = MaterialTheme.typography.bodyLarge)
                            Text("Tel.: $phone", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        ErrorText(err)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0 || onBack != null) OutlinedButton({ if (step > 0) step-- else onBack?.invoke() }, Modifier.heightIn(min = 56.dp), shape = MaterialTheme.shapes.medium) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(4.dp)); Text("Atrás") }
            if (step < steps.lastIndex) BigButton("Siguiente", { step++ }, Modifier.weight(1f), enabled = valid, icon = Icons.Filled.ArrowForward)
            else BigButton(cta, {
                scope.launch {
                    busy = true; err = null
                    try { Locator.repo.saveAddress(draft, name, phone, townName); vm.refreshProfile(); step = 0; onSaved() } catch (e: ApiException) { err = e.message }
                    busy = false
                }
            }, Modifier.weight(1f), loading = busy, icon = Icons.Filled.Save)
        }
        if (!valid && step < steps.lastIndex) Text(when (step) { 0 -> "Pon tu nombre y un teléfono de 9 cifras."; 1 -> "Elige tu pueblo y revisa el código postal."
            else -> "Escribe la calle y el número (o «s/n»)." }, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Un último paso", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            TextButton({ vm.logout() }) { Text("Salir") }
        }
        val driver = vm.profile.collectAsState().value?.isDriver == true
        ProfileForm(vm, if (driver) "¿Dónde vives y repartes?" else "¿Dónde te llevamos la compra?", "Guardar y continuar") { vm.start() }
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
                        Text("Llega desde tlotraigo@unocerobits.com. ¿No te llega? Mira en correo no deseado o escribe a tlotraigo@unocerobits.com.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    } else {
                        Text("Elige tu nueva contraseña", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        PasswordField(newPass) { newPass = it }
                        PasswordChecklist(newPass)
                        ErrorText(err)
                        Spacer(Modifier.height(16.dp))
                        BigButton("Guardar contraseña", {
                            scope.launch {
                                busy = true; err = null
                                try { Locator.api.updatePassword(newPass); vm.start() } catch (e: ApiException) { err = e.message }
                                busy = false
                            }
                        }, enabled = passwordOk(newPass), loading = busy, icon = Icons.Filled.Save)
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
