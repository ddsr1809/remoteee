package app.remoto.control.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.remoto.control.core.driver.HelpTopic
import app.remoto.control.ui.devices.DevicesScreen
import app.remoto.control.ui.help.HelpScreen
import app.remoto.control.ui.home.ComingSoonScreen
import app.remoto.control.ui.home.HomeScreen
import app.remoto.control.ui.home.WelcomeScreen
import app.remoto.control.ui.remote.RemoteScreen
import app.remoto.control.ui.settings.SettingsScreen
import app.remoto.control.ui.theme.RemotoTheme

@Composable
fun RemotoRoot(viewModel: MainViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val start by viewModel.start.collectAsStateWithLifecycle()

    RemotoTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val s = start) {
                StartDestination.Loading -> Box(Modifier.fillMaxSize())
                StartDestination.Welcome -> AppNavHost(startAtWelcome = true, openDeviceId = null)
                is StartDestination.Home -> AppNavHost(startAtWelcome = false, openDeviceId = s.openDeviceId)
            }
        }
    }
}

@Composable
private fun AppNavHost(
    startAtWelcome: Boolean,
    openDeviceId: String?,
    navController: NavHostController = rememberNavController(),
) {
    // Al arrancar con un dispositivo usado antes, se abre su control encima del inicio
    // (así "atrás" lleva al inicio). Solo una vez, aunque gire la pantalla.
    var openedLastDevice by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openDeviceId) {
        if (!openedLastDevice && openDeviceId != null) {
            openedLastDevice = true
            navController.navigate(RemoteRoute(openDeviceId))
        }
    }

    val openRemote: (String) -> Unit = { id -> navController.navigate(RemoteRoute(id)) }
    val openHelp: (HelpTopic?) -> Unit = { topic -> navController.navigate(HelpRoute(topic?.name)) }
    val openComingSoon: (ComingSoonFeature) -> Unit = { f -> navController.navigate(ComingSoonRoute(f.name)) }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = if (startAtWelcome) WelcomeRoute else HomeRoute,
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onComingSoon = openComingSoon,
                onDemoReady = { id ->
                    navController.navigate(HomeRoute) { popUpTo(WelcomeRoute) { inclusive = true } }
                    openRemote(id)
                },
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                onOpenRemote = openRemote,
                onComingSoon = openComingSoon,
                onDevices = { navController.navigate(DevicesRoute) },
                onSettings = { navController.navigate(SettingsRoute) },
                onHelp = { openHelp(null) },
            )
        }
        composable<RemoteRoute> {
            RemoteScreen(onBack = back, onHelp = openHelp)
        }
        composable<DevicesRoute> {
            DevicesScreen(onBack = back, onOpenRemote = openRemote)
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = back, onHelp = { openHelp(null) })
        }
        composable<HelpRoute> { entry ->
            val route = entry.toRoute<HelpRoute>()
            HelpScreen(
                topic = route.topic?.let { name -> HelpTopic.entries.firstOrNull { it.name == name } },
                onBack = back,
                onOpenTopic = { openHelp(it) },
            )
        }
        composable<ComingSoonRoute> { entry ->
            val route = entry.toRoute<ComingSoonRoute>()
            ComingSoonScreen(
                feature = ComingSoonFeature.entries.firstOrNull { it.name == route.feature } ?: ComingSoonFeature.WIFI,
                onBack = back,
                onDemoReady = { id ->
                    navController.navigate(HomeRoute) { popUpTo(navController.graph.id) { inclusive = true } }
                    openRemote(id)
                },
            )
        }
    }
}
