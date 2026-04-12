package com.example.sifa_go.ui.navigation

import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sifa_go.ui.components.MainLayout
import com.example.sifa_go.ui.views.CameraScreen
import com.example.sifa_go.ui.views.LoginScreen
import com.example.sifa_go.ui.views.PreviewScreen
import com.example.sifa_go.viewmodel.SifaViewModel

@Composable
fun AppNavigation() {
    // ENRUTADOR RAÍZ (Nivel 1): Solo decide entre Login o la App Principal
    val rootNavController = rememberNavController()

    NavHost(navController = rootNavController, startDestination = "login") {

        // RUTA RAÍZ 1: Pantalla de Login (Pantalla completa, sin menús)
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Navegamos a la app principal y borramos el login del historial
                    rootNavController.navigate("main_app") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // RUTA RAÍZ 2: El contenedor de toda tu aplicación principal
        composable("main_app") {
            // Llamamos a la función que contiene el MainLayout y el segundo enrutador
            MainAppNavigation()
        }
    }
}
@Composable
fun MainAppNavigation(
    sifaViewModel: SifaViewModel = viewModel()
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
            // --- TUS 4 RUTAS DE LA APP VAN AQUÍ ---

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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Perfil del Fiscalizador")
                }
            }
        }
    }
}