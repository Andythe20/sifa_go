package com.sifa.sifa_go.ui.navigation

import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.sifa.sifa_go.core.network.NetworkStatus
import com.sifa.sifa_go.core.network.rememberNetworkStatus
import com.sifa.sifa_go.core.utils.BiometricHelper
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.ui.components.MainLayout
import com.sifa.sifa_go.ui.components.NetworkBanner
import com.sifa.sifa_go.ui.views.CameraScreen
import com.sifa.sifa_go.ui.views.LoginScreen
import com.sifa.sifa_go.viewmodel.SifaViewModel
import com.sifa.sifa_go.R
import com.sifa.sifa_go.ui.views.CreditsScreen
import com.sifa.sifa_go.ui.views.HelpScreen


@Composable
fun AppNavigation() {
    // ENRUTADOR RAÍZ (Nivel 1): Solo decide entre Login o la App Principal
    val rootNavController = rememberNavController()

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Observador de red global - disponible en todas las pantallas
    val networkStatus = rememberNetworkStatus()

    Column(modifier = Modifier.fillMaxSize()) {
        // Banner de red global - aparece en todas las vistas
        NetworkBanner(
            networkStatus = networkStatus.value,
            modifier = Modifier.fillMaxWidth()
        )

        NavHost(navController = rootNavController, startDestination = "check_auth") {

            // RUTA DE DECISIÓN (Invisible para el usuario)
            composable("check_auth") {
                LaunchedEffect(Unit) {
                    val token = sessionManager.getToken()
                    if (token == null) {
                        // No hay sesión -> Al Login
                        rootNavController.navigate("login") {
                            popUpTo("check_auth") { inclusive = true }
                        }
                    } else {
                        // HAY SESIÓN -> Pedir huella de inmediato
                        BiometricHelper.authenticate(
                            context = context,
                            onSuccess = {
                                rootNavController.navigate("main_app") {
                                    popUpTo("check_auth") { inclusive = true }
                                }
                            },
                            onError = { error ->
                                // Si falla la huella o cancela, lo mandamos al login por seguridad
                                // o puedes dejarlo en una pantalla de 'Reintentar Huella'
                                rootNavController.navigate("login")
                            }
                        )
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
                // Llamamos a la función que contiene el MainLayout y el segundo enrutador
                MainAppNavigation(
                    onLogout = {
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
    sifaViewModel: SifaViewModel = viewModel(),
    onLogout: () -> Unit
) {
    // ENRUTADOR DE PESTAÑAS: Maneja las vistas DENTRO del MainLayout
    val tabsNavController = rememberNavController()

    // Obtenemos la ruta actual para pintar de azul el ícono correcto en el footer
    val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "scan"

    // La cámara se crea aquí.
    // Como estamos dentro de MainAppNavigation, la cámara sobrevivirá aunque pases al Historial y vuelvas.
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }

    MainLayout(
        title = "SIFA GO",
        currentRoute = currentRoute,
        onNavigate = { route ->
            tabsNavController.navigate(route) {
                // Evita crear un historial infinito al tocar los botones del menú
                popUpTo(tabsNavController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onBackClick = { tabsNavController.popBackStack() },
        onProfileClick = { tabsNavController.navigate("profile") }
    ) { paddingValues ->

        // El NavHost interno que dibuja las vistas respetando los márgenes del MainLayout
        NavHost(
            navController = tabsNavController,
            startDestination = "scan",
            modifier = Modifier.padding(paddingValues)
        ) {

            composable("scan") {
                CameraScreen(
                    cameraController = cameraController, // Pasamos el controlador seguro
                    sifaViewModel = sifaViewModel,
                    onPhotoConfirmed = { pathToUpload ->
                        sifaViewModel.currentPhotoPath = pathToUpload
                        println("Enviando foto al backend: $pathToUpload")
                    }
                )
            }

            composable("history") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Historial en construcción")
                }
            }

            composable("reports") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Reportes en construcción")
                }
            }

            composable("profile") {
                /* TODO: esta vista debe tener su propio archivo, por ahora solo es de prueba */
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Perfil del Fiscalizador", modifier = Modifier.padding(bottom = 24.dp))

                    // Botón de cerrar sesión con un color de error (rojo) por defecto en Material3
                    Button(
                        onClick = { onLogout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cerrar Sesión")
                    }
                }
            }
        }
    }
}