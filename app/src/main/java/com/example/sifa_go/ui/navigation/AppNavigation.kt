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
import com.example.sifa_go.ui.views.PreviewScreen
import com.example.sifa_go.viewmodel.SifaViewModel

@Composable
fun AppNavigation(
    // Inyectamos el ViewModel aquí. Se mantendrá vivo mientras AppNavigation exista.
    sifaViewModel: SifaViewModel = viewModel()
) {
    // El controlador que maneja el estado de las pantallas
    val navController = rememberNavController()

    // Obtenemos la ruta actual para que el MainLayout sepa qué ícono pintar de azul
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "scan"

    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }

    // Envolvemos toda la navegación con tu Layout Principal
    MainLayout(
        title = "SIFA GO",
        currentRoute = currentRoute,
        onNavigate = { route ->
            navController.navigate(route) {
                // Evita crear múltiples copias de la misma pantalla al navegar
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        onBackClick = {
            navController.popBackStack()
        },
        onProfileClick = {
            navController.navigate("profile")
        }
    ) { paddingValues ->
        // Aquí adentro va el NavHost, fíjate que le pasamos el paddingValues
        // Esto evita que tus vistas queden ocultas detrás del menú o el header
        NavHost(
            navController = navController,
            startDestination = "scan",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Ruta 1: Escanear
            composable("scan") {
                CameraScreen(
                    onPhotoConfirmed = { pathToUpload ->
                        // 1. Guardamos la foto confirmada en el ViewModel
                        sifaViewModel.currentPhotoPath = pathToUpload

                        // 2. Aquí llamaremos al backend con IA
                        println("Enviando foto al backend: $pathToUpload")

                        // 3. Más adelante, aquí harás un navController.navigate("formulario_multa")
                    }
                )
            }

            // Ruta 2: Previsualización
            composable("preview") {
                // Recuperamos la ruta desde el ViewModel
                val photoPath = sifaViewModel.currentPhotoPath

                // Validamos que exista
                if (photoPath != null) {
                    PreviewScreen(
                        photoPath = photoPath,
                        onRetakePhoto = {
                            sifaViewModel.clearProcess() // Limpiamos el rastro anterior
                            navController.popBackStack() // Volvemos a la cámara
                        },
                        onSendPhoto = { pathToUpload ->
                            // Aquí llamaremos a tu API de Laravel para procesar la imagen
                            println("Enviando foto al backend: $pathToUpload")
                        }
                    )
                } else {
                    // Por si ocurre un error extraño, volvemos a la cámara
                    navController.popBackStack()
                }
            }

            // Ruta 3: Historial (Vista de prueba)
            composable("history") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Historial en construcción")
                }
            }

            // Ruta 4: Reportes (Vista de prueba)
            composable("reports") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Reportes en construcción")
                }
            }

            // Ruta 5: Perfil (Vista de prueba)
            composable("profile") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Perfil del Fiscalizador")
                }
            }
        }
    }
}