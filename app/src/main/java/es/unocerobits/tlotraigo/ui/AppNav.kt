package es.unocerobits.tlotraigo.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Boot
import es.unocerobits.tlotraigo.ui.components.bump
import es.unocerobits.tlotraigo.ui.components.eur
import es.unocerobits.tlotraigo.ui.screens.*

private enum class Tab(val label: String, val icon: ImageVector) {
    COMPRAR("Comprar", Icons.Filled.Storefront), PEDIDOS("Pedidos", Icons.Filled.ReceiptLong),
    ENTIDAD("Mi entidad", Icons.Filled.Apartment), ADMIN("Admin", Icons.Filled.AdminPanelSettings), AJUSTES("Ajustes", Icons.Filled.Settings)
}

/** Etiqueta de pestaña que no se parte ni desborda con letra grande. */
@Composable
fun TabLabel(text: String) = Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val boot by vm.boot.collectAsState()
    LaunchedEffect(boot) {
        val dest = when (boot) {
            Boot.LoggedOut -> "login"; Boot.NeedsTerms -> "terms"; Boot.NeedsProfile -> "onboard"; Boot.Client -> "home"; Boot.Driver -> "driver"
            else -> return@LaunchedEffect
        }
        if (nav.currentDestination?.route != dest) nav.navigate(dest) { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
    }
    DisposableEffect(nav) {   // estadísticas de pantallas (solo con consentimiento)
        val l = androidx.navigation.NavController.OnDestinationChangedListener { _, d, _ -> d.route?.let { es.unocerobits.tlotraigo.data.Analytics.screen(it.substringBefore('/')) } }
        nav.addOnDestinationChangedListener(l); onDispose { nav.removeOnDestinationChangedListener(l); es.unocerobits.tlotraigo.data.Analytics.flush() }
    }
    val update by vm.update.collectAsState()
    LaunchedEffect(Unit) { vm.checkUpdate() }
    update?.let { UpdateDialog(it) { vm.dismissUpdate() } }
    val chatReq by vm.openChat.collectAsState()
    LaunchedEffect(chatReq, boot) { val id = chatReq; if (id != null && (boot == Boot.Client || boot == Boot.Driver)) { nav.navigate("chat/$id"); vm.openChat.value = null } }
    val toVerify: (String, String) -> Unit = { email, mode -> nav.navigate("verify/$mode/${Uri.encode(email)}") }
    val spec = tween<IntOffset>(380, easing = EaseOutCubic)
    val invite by vm.inviteCode.collectAsState()
    if (invite != null && (boot == Boot.Client || boot == Boot.Driver)) RedeemInviteDialog(vm, invite!!) { vm.inviteCode.value = null }
    CompositionLocalProvider(LocalOpenChat provides { id: Long -> nav.navigate("chat/$id") }, LocalOpenOrg provides { id: Long -> nav.navigate("org/$id") },
        LocalIsSuper provides (vm.profile.collectAsState().value?.role == "superadmin")) {
    NavHost(
        nav, "splash",
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, spec) + fadeIn(tween(300)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, spec) { it / 4 } + fadeOut(tween(250)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, spec) { it / 4 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, spec) + fadeOut(tween(250)) },
    ) {
        composable("splash") { SplashScreen(vm) }
        composable("login") { LoginScreen(vm, onRegister = { nav.navigate("register") }, onVerify = toVerify) }
        composable("register") { RegisterScreen(vm, onBack = { nav.popBackStack() }, onVerify = toVerify) }
        composable("verify/{mode}/{email}") { e ->
            VerifyScreen(vm, e.arguments?.getString("email").orEmpty(), e.arguments?.getString("mode") ?: "signup") { nav.popBackStack() }
        }
        composable("onboard") { OnboardingWizard(vm) }
        composable("org/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
            OrgPanel(vm, e.arguments?.getLong("id") ?: 0, onBack = { nav.popBackStack() }, onOrder = { nav.navigate("order/$it") }) }
        composable("chat/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e -> ChatScreen(vm, e.arguments?.getLong("id") ?: 0) { nav.popBackStack() } }
        composable("needs") { NeedsScreen(vm, onBack = { nav.popBackStack() }, onCart = { nav.navigate("cart") }) }
        composable("terms") { TermsScreen(vm) }
        composable("home") { ClientHome(vm, onCart = { nav.navigate("cart") }, onOrder = { nav.navigate("order/$it") }, onDebug = { nav.navigate("debug") },
            onEditProduct = { id -> nav.navigate("admin/product/${id ?: -1}") }, onNeeds = { nav.navigate("needs") }) }
        composable("admin/product/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
            ProductEditorScreen(e.arguments?.getLong("id")?.takeIf { it > 0 }) { nav.popBackStack() }
        }
        composable("driver") { DriverHome(vm) { nav.navigate("debug") } }
        composable("debug") { DebugScreen(vm) { nav.popBackStack() } }
        composable("cart") {
            CartScreen(vm, onBack = { nav.popBackStack() }, onPlaced = { id -> nav.navigate("order/$id") { popUpTo("home") } })
        }
        composable("order/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
            OrderDetailScreen(vm, e.arguments?.getLong("id") ?: 0, onBack = { nav.popBackStack() }, onRepeat = { nav.navigate("cart") { popUpTo("home") } })
        }
    }
    }
}

@Composable
private fun ClientHome(vm: AppViewModel, onCart: () -> Unit, onOrder: (Long) -> Unit, onDebug: () -> Unit, onEditProduct: (Long?) -> Unit, onNeeds: () -> Unit) {
    val prof = vm.profile.collectAsState().value
    val isAdmin = prof?.isAdmin == true
    val isGestor = prof?.isGestor == true
    var tab by rememberSaveable { mutableStateOf(if (isGestor) Tab.ENTIDAD else Tab.COMPRAR) }
    val home = if (isGestor) Tab.ENTIDAD else Tab.COMPRAR
    BackHandler(tab != home) { tab = home }
    val lines by vm.cart.lines.collectAsState()
    val count = lines.values.sumOf { it.qty }
    val total = lines.values.sumOf { it.total }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.filter { (it != Tab.ADMIN || isAdmin) && (it != Tab.ENTIDAD || isGestor) }.forEach { t -> NavigationBarItem(tab == t, { tab = t }, { Icon(t.icon, null) }, label = { TabLabel(t.label) }, alwaysShowLabel = true) }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(count > 0 && tab == Tab.COMPRAR, enter = scaleIn() + slideInVertically { it }, exit = scaleOut() + slideOutVertically { it }) {
                ExtendedFloatingActionButton(
                    onClick = onCart,
                    icon = { BadgedBox({ Badge(Modifier.bump(count)) { Text("$count") } }) { Icon(Icons.Filled.ShoppingCart, null) } },
                    text = { Text("Ver cesta · ${eur(total)}", style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { pad ->
        // El Scaffold ya aplica el inset de la barra de estado: no añadir statusBarsPadding (doble margen).
        AnimatedContent(tab, Modifier.padding(pad), transitionSpec = {
            (fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 20 }) togetherWith fadeOut(tween(150))
        }, label = "tab") { t ->
            when (t) {
                Tab.COMPRAR -> CatalogScreen(vm, onCart, onNeeds)
                Tab.PEDIDOS -> OrdersScreen(onOrder)
                Tab.ADMIN -> AdminHome(vm, onEditProduct)
                Tab.ENTIDAD -> prof?.orgId?.let { OrgPanel(vm, it, onOrder = onOrder) } ?: Text("Sin entidad asignada", Modifier.padding(androidx.compose.ui.unit.Dp(24f)))
                Tab.AJUSTES -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { SettingsScreen(vm, onDebug = onDebug) }
            }
        }
    }
}
