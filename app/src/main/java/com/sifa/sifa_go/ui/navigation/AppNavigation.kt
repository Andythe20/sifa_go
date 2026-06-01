package com.sifa.sifa_go.ui.navigation

import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sifa.sifa_go.core.network.GpsStatus
import com.sifa.sifa_go.core.network.NetworkStatus
import com.sifa.sifa_go.core.network.rememberGpsStatus
import com.sifa.sifa_go.core.network.rememberNetworkStatus
import com.sifa.sifa_go.core.network.AuthRetrofitClient
import com.sifa.sifa_go.core.utils.BiometricHelper
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.RefreshTokenRequest
import com.sifa.sifa_go.ui.components.MainLayout
import com.sifa.sifa_go.ui.components.NetworkBanner
import com.sifa.sifa_go.ui.views.CameraScreen
import com.sifa.sifa_go.ui.views.LoginScreen
import com.sifa.sifa_go.viewmodel.SifaViewModel
import com.sifa.sifa_go.R
import com.sifa.sifa_go.ui.views.CreditsScreen
import com.sifa.sifa_go.ui.views.HelpScreen
import com.sifa.sifa_go.ui.views.HistoryScreen
import com.sifa.sifa_go.ui.views.HomeScreen
import com.sifa.sifa_go.ui.views.ProfileScreen
import com.sifa.sifa_go.ui.views.ReportsScreen
import com.sifa.sifa_go.viewmodel.CoreViewModel
import com.sifa.sifa_go.viewmodel.PresenceViewModel


@Composable
fun AppNavigation() {
    // ENRUTADOR RAÍZ (Nivel 1): Solo decide entre Login o la App Principal
    val rootNavController = rememberNavController()

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Observador de red global - disponible en todas las pantallas
    val networkStatus = rememberNetworkStatus()
    val gpsStatus = rememberGpsStatus()

    // Observa eventos de sesión expirada y redirige al login
    LaunchedEffect(Unit) {
        SessionManager.sessionExpiredEvent.collect {
            rootNavController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NetworkBanner(
            networkStatus = networkStatus.value,
            gpsStatus = gpsStatus.value,
            modifier = Modifier.fillMaxWidth()
        )

        // Debe empezar en check_auth ya que ahí se revisa si hay una sesión activa
        NavHost(navController = rootNavController, startDestination = "check_auth") {

            // RUTA DE DECISIÓN (Invisible para el usuario)
            composable("check_auth") {
                LaunchedEffect(Unit) {
                    if (sessionManager.hasValidSession()) {
                        // Intentar refresh proactivo antes de mostrar biometría
                        val refreshToken = sessionManager.getRefreshToken()
                        if (refreshToken != null) {
                            try {
                                val refreshResponse = AuthRetrofitClient.apiService.refresh(
                                    RefreshTokenRequest(refreshToken)
                                )
                                if (refreshResponse.isSuccessful) {
                                    val body = refreshResponse.body()
                                    if (body != null) {
                                        sessionManager.saveSession(
                                            token = body.accessToken,
                                            refreshToken = body.refreshToken,
                                            username = body.sub,
                                            roles = body.roles,
                                            expiry = body.exp
                                        )
                                    }
                                }
                            } catch (_: Exception) { }
                        }

                        BiometricHelper.authenticate(
                            context = context,
                            onSuccess = {
                                rootNavController.navigate("main_app") {
                                    popUpTo("check_auth") { inclusive = true }
                                }
                            },
                            onError = { error ->
                                rootNavController.navigate("login")
                            }
                        )
                    } else {
                        sessionManager.logout()
                        rootNavController.navigate("login") {
                            popUpTo("check_auth") { inclusive = true }
                        }
                    }
                }

                // Mientras decide, mostramos una pantalla de carga con tu logo
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.sifago_logo),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp)
                    )
                }
            }

            // RUTA RAÍZ 1: Pantalla de Login (Pantalla completa, sin menús)
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        // Navegamos a la app principal y borramos el login del historial
                        rootNavController.navigate("main_app") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToCredits = {
                        rootNavController.navigate("credits")
                    },
                    onNavigateToHelp = {
                        rootNavController.navigate("help")
                    }
                )
            }

            // RUTA RAÍZ 2: El contenedor de toda tu aplicación principal
            composable("main_app") {
                val context = LocalContext.current

                val presenceViewModel: PresenceViewModel = viewModel()

                LaunchedEffect(Unit) {
                    presenceViewModel.startHeartbeatEngine(context, sessionManager)
                }

                // Llamamos a la función que contiene el MainLayout y el segundo enrutador
                MainAppNavigation(
                    presenceViewModel = presenceViewModel,
                    gpsStatus = gpsStatus.value,
                    onLogout = {
                        presenceViewModel.stopHeartbeatEngine()
                        sessionManager.logout() // Borramos el token y el username del celular
                        rootNavController.navigate("login") {
                            // Limpiamos absolutamente todo el historial de pantallas para que no pueda volver con el botón "Atrás"
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // RUTA RAÍZ 3: Pantalla de créditos (accesible desde el menú lateral)
            composable("credits") {
                CreditsScreen(
                    onBack = { rootNavController.popBackStack() }
                )
            }

            // RUTA RAÍZ 4: Pantalla de ayuda (accesible desde el menú lateral)
            composable("help") {
                HelpScreen(
                    onBack = { rootNavController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MainAppNavigation(
    presenceViewModel: PresenceViewModel,
    sifaViewModel: SifaViewModel = viewModel(),
    coreViewModel: CoreViewModel = viewModel(),
    gpsStatus: GpsStatus = GpsStatus.Available,
    onLogout: () -> Unit
) {
    // ENRUTADOR DE PESTAÑAS: Maneja las vistas DENTRO del MainLayout
    val tabsNavController = rememberNavController()

    // Obtenemos la ruta actual para pintar de azul el ícono correcto en el footer
    val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    // La cámara se crea aquí.
    // Como estamos dentro de MainAppNavigation, la cámara sobrevivirá aunque pases al Historial y vuelvas.
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }

    MainLayout(
        presenceViewModel = presenceViewModel,
        title = "SIFA GO",
        username = sifaViewModel.currentUsername,
        currentRoute = currentRoute,
        onNavigate = { route ->

            if (currentRoute == route) {
                if (route == "scan") {
                    sifaViewModel.clearProcess()
                    coreViewModel.clearData()
                }
                return@MainLayout
            }

            // Primero navegamos para que el NavHost empiece el cambio de vista
            tabsNavController.navigate(route) {
                // Navegación limpia para evitar "atascamiento" de estados previos
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        },
        onBackClick = { tabsNavController.popBackStack() },
        onProfileClick = { tabsNavController.navigate("profile") }
    ) { paddingValues ->

        // El NavHost interno que dibuja las vistas respetando los márgenes del MainLayout
        NavHost(
            navController = tabsNavController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(
                "home",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                // Limpiamos los datos del proceso al entrar al inicio para evitar parpadeos visuales
                // Añadimos un pequeño retraso para asegurar que la animación de salida de la cámara haya terminado
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1000)
                    // Solo limpiamos si el usuario NO ha iniciado un nuevo proceso en este segundo
                    if (!sifaViewModel.isManualEntry && sifaViewModel.detectedPlate == null) {
                        sifaViewModel.clearProcess()
                        coreViewModel.clearData()
                    }
                }

                HomeScreen(
                    username = sifaViewModel.currentUsername,
                    sifaViewModel = sifaViewModel,
                    onStartCamera = {
                        sifaViewModel.clearProcess()
                        coreViewModel.clearData()
                        tabsNavController.navigate("scan") { launchSingleTop = true }
                    },
                    onStartManual = {
                        sifaViewModel.clearProcess()
                        coreViewModel.clearData()
                        sifaViewModel.isManualEntry = true
                        sifaViewModel.detectedPlate = "" // Limpiamos residuos
                        tabsNavController.navigate("scan") { launchSingleTop = true }
                    }
                )
            }

            composable(
                "scan",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                CameraScreen(
                    cameraController = cameraController, // Pasamos el controlador seguro
                    sifaViewModel = sifaViewModel,
                    coreViewModel = coreViewModel,
                    gpsStatus = gpsStatus,
                    currentRoute = currentRoute,
                    onPhotoConfirmed = { pathToUpload ->
                        sifaViewModel.currentPhotoPath = pathToUpload
                        println("Enviando foto al backend: $pathToUpload")
                    }
                )
            }

            composable(
                "history",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                HistoryScreen(sifaViewModel = sifaViewModel)
            }

            composable(
                "reports",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                ReportsScreen(sifaViewModel = sifaViewModel)
            }

            composable(
                "profile",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                ProfileScreen(
                    onLogout = onLogout
                )
            }
        }
    }
}