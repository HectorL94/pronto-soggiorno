package app.prontosoggiorno.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.prontosoggiorno.appContainer
import app.prontosoggiorno.ui.add.AddPraticaRoute
import app.prontosoggiorno.ui.detail.PraticaDetailRoute
import app.prontosoggiorno.ui.list.PraticaListRoute
import app.prontosoggiorno.ui.onboarding.OnboardingRoute
import app.prontosoggiorno.ui.settings.SettingsRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

// Pantallas de la app (rutas con tipos de Navigation Compose).
@Serializable data object OnboardingDest
@Serializable data object ListDest
@Serializable data object AddDest
@Serializable data class DetailDest(val praticaId: Long)
@Serializable data object SettingsDest

/**
 * Raíz de la interfaz.
 * @param openPratica pratica a abrir al tocar una notificación (se consume con [onPraticaOpened]).
 */
@Composable
fun ProntoSoggiornoAppUi(openPratica: StateFlow<Long?>, onPraticaOpened: () -> Unit) {
    val settingsRepository = LocalContext.current.appContainer.settingsRepository
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val loaded = settings ?: return@Surface // un instante, mientras se leen los ajustes
        // La pantalla inicial se decide una sola vez.
        val start: Any = remember { if (loaded.onboardingCompleted) ListDest else OnboardingDest }
        val navController = rememberNavController()
        AppNavHost(navController, start)

        val pending by openPratica.collectAsStateWithLifecycle()
        LaunchedEffect(pending, loaded.onboardingCompleted) {
            val id = pending ?: return@LaunchedEffect
            if (loaded.onboardingCompleted) {
                navController.navigate(DetailDest(id)) {
                    popUpTo(ListDest)
                    launchSingleTop = true
                }
            }
            onPraticaOpened()
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, start: Any) {
    NavHost(navController = navController, startDestination = start) {
        composable<OnboardingDest> {
            OnboardingRoute(
                onFinished = { addFirst ->
                    navController.navigate(ListDest) { popUpTo(OnboardingDest) { inclusive = true } }
                    if (addFirst) navController.navigate(AddDest)
                },
            )
        }
        composable<ListDest> {
            PraticaListRoute(
                onAdd = { navController.navigate(AddDest) },
                onOpen = { navController.navigate(DetailDest(it)) },
                onSettings = { navController.navigate(SettingsDest) },
            )
        }
        composable<AddDest> {
            AddPraticaRoute(
                onBack = { navController.popBackStack() },
                onAdded = { id ->
                    navController.navigate(DetailDest(id)) { popUpTo(ListDest) }
                },
            )
        }
        composable<DetailDest> { entry ->
            val id = entry.toRoute<DetailDest>().praticaId
            PraticaDetailRoute(praticaId = id, onBack = { navController.popBackStack(ListDest, inclusive = false) })
        }
        composable<SettingsDest> {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onAllDataDeleted = {
                    navController.navigate(OnboardingDest) { popUpTo(0) { inclusive = true } }
                },
            )
        }
    }
}
