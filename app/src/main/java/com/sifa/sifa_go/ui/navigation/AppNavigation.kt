package com.sifa.sifa_go.ui.navigation

import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.sifa.sifa_go.core.network.rememberGpsStatus
import com.sifa.sifa_go.core.network.rememberNetworkStatus
import com.sifa.sifa_go.core.network.AuthRetrofitClient
import com.sifa.sifa_go.core.utils.BiometricHelper
import com.sifa.sifa_go.core.network.ServerConfig
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.core.utils.vibrateShort
import com.sifa.sifa_go.data.model.RefreshTokenRequest
import com.sifa.sifa_go.ui.components.MainLayout
import com.sifa.sifa_go.ui.components.NetworkBanner
import androidx.navigation.navigation
import com.sifa.sifa_go.ui.views.LiveScannerScreen
import com.sifa.sifa_go.ui.views.PreviewScreen
import com.sifa.sifa_go.ui.views.TicketSuccessScreen
import com.sifa.sifa_go.ui.views.TicketScreen
import com.sifa.sifa_go.ui.views.VehicleInfoScreen
import com.sifa.sifa_go.ui.views.PlateResultScreen
import com.sifa.sifa_go.ui.views.CameraView
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import java.io.File
import android.util.Log
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import com.sifa.sifa_go.ui.views.LoginScreen
import com.sifa.sifa_go.ui.views.RecoveryScreen
import com.sifa.sifa_go.viewmodel.SifaViewModel
import com.sifa.sifa_go.R
import com.sifa.sifa_go.ui.views.CreditsScreen
import com.sifa.sifa_go.ui.views.HelpScreen
import com.sifa.sifa_go.ui.views.ChangePasswordScreen
import com.sifa.sifa_go.ui.views.HistoryScreen
import com.sifa.sifa_go.ui.views.HomeScreen
import com.sifa.sifa_go.ui.views.ProfileScreen
import com.sifa.sifa_go.ui.views.OverviewScreen
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

    var showSessionExpiredOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SessionManager.sessionExpiredEvent.collect {
            context.vibrateShort()
            showSessionExpiredOverlay = true
            kotlinx.coroutines.delay(ServerConfig.SESSION_EXPIRED_DELAY_MS)
            rootNavController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val currentRoute = rootNavController.currentBackStackEntryAsState()?.value?.destination?.route
    LaunchedEffect(currentRoute) {
        if (currentRoute == "login") {
            showSessionExpiredOverlay = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NetworkBanner(
                networkStatus = networkStatus.value,
                gpsStatus = gpsStatus.value,
                modifier = Modifier.fillMaxWidth()
            )

            // Debe empezar en check_auth ya que ahí se revisa si hay una sesión activa
            NavHost(navController = rootNavController, startDestination = "check_auth") {//TODO
            //NavHost(navController = rootNavController, startDestination = "recovery") {
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
                    onNavigateToRecovery = {
                        rootNavController.navigate("recovery")
                    },
                    onNavigateToCredits = {
                        rootNavController.navigate("credits")
                    },
                    onNavigateToHelp = {
                        rootNavController.navigate("help")
                    }
                )
            }

            // RUTA RAÍZ 2: Recuperación de contraseña
            composable("recovery") {
                RecoveryScreen(
                    onNavigateToLogin = {
                        rootNavController.popBackStack("login", false)
                    }
                )
            }

            // RUTA RAÍZ 3: El contenedor de toda tu aplicación principal
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
                    },
                    onHelp = { rootNavController.navigate("help") },
                    onCredits = { rootNavController.navigate("credits") }
                )
            }

            // RUTA RAÍZ 4: Pantalla de créditos (accesible desde el menú lateral)
            composable("credits") {
                CreditsScreen(
                    onBack = { rootNavController.popBackStack() }
                )
            }

            // RUTA RAÍZ 5: Pantalla de ayuda (accesible desde el menú lateral)
            composable("help") {
                HelpScreen(
                    onBack = { rootNavController.popBackStack() }
                )
            }
        }
    }

    if (showSessionExpiredOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Sesión expirada",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Redirigiendo al inicio de sesión...",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
    onLogout: () -> Unit,
    onHelp: () -> Unit = {},
    onCredits: () -> Unit = {}
) {
    // ENRUTADOR DE PESTAÑAS: Maneja las vistas DENTRO del MainLayout
    val tabsNavController = rememberNavController()

    // Obtenemos la ruta actual para pintar de azul el ícono correcto en el footer
    val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    // La cámara se crea aquí.
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Controlamos el ciclo de vida de la cámara de forma manual y eficiente
    // Se bindea una sola vez al entrar a la app principal y se desvincula al salir.
    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.unbind()
        }
    }

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
        onProfileClick = { tabsNavController.navigate("profile") },
        onHelp = onHelp,
        onCredits = onCredits,
        onLogout = onLogout
    ) { paddingValues ->
        val onChangePassword = { tabsNavController.navigate("change_password") }

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
                // Ya no necesitamos la limpieza proactiva aquí porque se hace al entrar al grafo de fiscalización
                // o al finalizar con éxito. Esto evita parpadeos y asegura que si el usuario vuelve atrás 
                // desde la cámara al Home, los datos sigan ahí si decide re-entrar (opcional).

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
                        sifaViewModel.startGpsCalibration() // Iniciamos GPS para el ingreso manual
                        tabsNavController.navigate("fiscalizacion/resultado") { launchSingleTop = true }
                    }
                )
            }

            // 1. FLUJO DE FISCALIZACIÓN (GRAFO ANIDADO)
            navigation(
                startDestination = "fiscalizacion/camara",
                route = "scan"
            ) {
                composable(
                    "fiscalizacion/camara",
                    enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    // Limpieza proactiva al entrar a la cámara (inicio del flujo)
                    LaunchedEffect(Unit) {
                        sifaViewModel.clearProcess()
                        coreViewModel.clearData()
                    }

                    var waitingForAiResponse by remember { mutableStateOf(false) }

                    if (sifaViewModel.currentPhotoPath != null) {
                        PreviewScreen(
                            photoPath = sifaViewModel.currentPhotoPath!!,
                            isProcessing = sifaViewModel.isLoading,
                            onRetakePhoto = {
                                sifaViewModel.removeEvidencePhoto(sifaViewModel.currentPhotoPath!!)
                                sifaViewModel.currentPhotoPath = null
                                waitingForAiResponse = false
                            },
                            onSendPhoto = { finalPath ->
                                if (!waitingForAiResponse) {
                                    sifaViewModel.uploadImageToBackend(finalPath)
                                    waitingForAiResponse = true
                                }
                            }
                        )
                    } else {
                        LiveScannerScreen(
                            cameraController = cameraController,
                            gpsStatus = gpsStatus,
                            isGPSCalibrating = sifaViewModel.isGPSCalibrating,
                            gpsAccuracy = sifaViewModel.gpsAccuracy,
                            onStartGpsCalibration = { sifaViewModel.startGpsCalibration() },
                            onPhotoTaken = { path ->
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                                sifaViewModel.captureTime = java.time.LocalDateTime.now().format(formatter)
                                sifaViewModel.currentPhotoPath = path
                                sifaViewModel.evidencePhotoPaths.add(path)
                            }
                        )
                    }

                    // Esperar a que la IA termine de procesar antes de navegar al resultado
                    LaunchedEffect(waitingForAiResponse, sifaViewModel.isLoading) {
                        if (waitingForAiResponse && !sifaViewModel.isLoading) {
                            tabsNavController.navigate("fiscalizacion/resultado")
                        }
                    }
                }

                composable(
                    "fiscalizacion/resultado",
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    var isConsulting by remember { mutableStateOf(false) }

                    PlateResultScreen(
                        initialPlate = sifaViewModel.detectedPlate,
                        errorMessage = coreViewModel.errorMessage ?: sifaViewModel.detectionError,
                        isLoading = coreViewModel.isLoading || isConsulting,
                        isManualEntry = sifaViewModel.isManualEntry,
                        onConsultClick = { finalPlate ->
                            sifaViewModel.detectedPlate = finalPlate
                            sifaViewModel.detectionError = null
                            coreViewModel.fetchVehicleInfo(finalPlate)
                            isConsulting = true
                        },
                        onRetakePhoto = {
                            sifaViewModel.currentPhotoPath = null
                            isConsulting = false
                            if (!tabsNavController.popBackStack("fiscalizacion/camara", false)) {
                                tabsNavController.navigate("fiscalizacion/camara")
                            }
                        }
                    )

                    LaunchedEffect(isConsulting, coreViewModel.isLoading) {
                        if (isConsulting && !coreViewModel.isLoading) {
                            isConsulting = false
                            if (coreViewModel.vehicleData != null) {
                                tabsNavController.navigate("fiscalizacion/info_vehiculo")
                            }
                        }
                    }
                }

                composable(
                    "fiscalizacion/info_vehiculo",
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    if (coreViewModel.vehicleData != null) {
                        VehicleInfoScreen(
                            vehicleData = coreViewModel.vehicleData!!,
                            onIssueFineClick = { tabsNavController.navigate("fiscalizacion/formulario") },
                            onNewScanClick = {
                                sifaViewModel.clearProcess()
                                coreViewModel.clearData()
                                tabsNavController.popBackStack("fiscalizacion/camara", false)
                            }
                        )
                    } else {
                        // Si por alguna razón llegamos aquí y no hay datos, mostramos un cargando
                        // o volvemos atrás. Dado que fetchVehicleInfo es asíncrono, esto puede pasar
                        // si la navegación ocurre antes de que la API responda.
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        
                        // Si hay un mensaje de error, significa que la carga falló
                        LaunchedEffect(coreViewModel.errorMessage) {
                            if (coreViewModel.errorMessage != null) {
                                tabsNavController.popBackStack()
                            }
                        }
                    }
                }

                composable(
                    "fiscalizacion/formulario",
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    val mainExecutor = remember { androidx.core.content.ContextCompat.getMainExecutor(context) }
                    var isTakingEvidencePhoto by remember { androidx.compose.runtime.mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        TicketScreen(
                            vehicleData = coreViewModel.vehicleData!!,
                            tiposInfraccion = coreViewModel.tiposInfraccion,
                            evidencePhotos = sifaViewModel.evidencePhotoPaths,
                            latitude = sifaViewModel.latitude,
                            longitude = sifaViewModel.longitude,
                            isSubmitting = coreViewModel.isSubmittingInfraccion,
                            isManualEntry = sifaViewModel.isManualEntry,
                            onCancelClick = { tabsNavController.popBackStack() },
                            onSubmitClick = { idInfraccion, observaciones, lat, lon ->
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                                val fechaFiscalizacion = sifaViewModel.captureTime ?: java.time.LocalDateTime.now().format(formatter)
                                val lugarFinal = sifaViewModel.currentAddress ?: "Ubicación GPS: $lat, $lon"

                                val request = com.sifa.sifa_go.data.model.InfraccionCreateRequest(
                                    lugar = lugarFinal,
                                    fecha = fechaFiscalizacion,
                                    latitud = lat?.toFloat() ?: 0f,
                                    longitud = lon?.toFloat() ?: 0f,
                                    patenteVehiculo = coreViewModel.vehicleData!!.patente,
                                    idTipoInfraccion = idInfraccion,
                                    observaciones = observaciones,
                                    fechaCitacion = null
                                )
                                coreViewModel.submitInfraccion(request, sifaViewModel.evidencePhotoPaths.toList())
                            },
                            onAddPhotoClick = { isTakingEvidencePhoto = true },
                            onRemovePhoto = { path ->
                                sifaViewModel.removeEvidencePhoto(path)
                            }
                        )

                        if (isTakingEvidencePhoto) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black)
                            ) {
                                CameraView(cameraController = cameraController, lifecycle = lifecycleOwner, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { isTakingEvidencePhoto = false },
                                    modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 16.dp)
                                ) {
                                    Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                                }
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        context.vibrateShort()
                                        val photoFile = File(context.cacheDir, "sifa_photo_${System.currentTimeMillis()}.jpg")
                                        val outputOptions = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                        cameraController.takePicture(
                                            outputOptions,
                                            mainExecutor,
                                            object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {
                                                    val permanentPath = com.sifa.sifa_go.core.utils.ImageUtils.compressImage(context, photoFile)
                                                    if (photoFile.exists()) { photoFile.delete() }
                                                    sifaViewModel.evidencePhotoPaths.add(permanentPath)
                                                    isTakingEvidencePhoto = false
                                                }
                                                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                                    Log.e("AppNavigation", "Error al tomar la foto de evidencia", exception)
                                                }
                                            }
                                        )
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                                    icon = { Icon(androidx.compose.material.icons.Icons.Filled.CameraAlt, contentDescription = null) },
                                    text = { Text("CAPTURAR EVIDENCIA") }
                                )
                            }
                        }

                        LaunchedEffect(coreViewModel.submitSuccess) {
                            if (coreViewModel.submitSuccess) {
                                tabsNavController.navigate("fiscalizacion/exito")
                            }
                        }
                    }
                }

                composable(
                    "fiscalizacion/exito",
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    TicketSuccessScreen(
                        onAnimationFinished = {
                            sifaViewModel.clearProcess()
                            coreViewModel.clearData()
                            tabsNavController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(
                "history",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                HistoryScreen(sifaViewModel = sifaViewModel)
            }

            composable(
                "overview",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                OverviewScreen(sifaViewModel = sifaViewModel)
            }

            composable(
                "profile",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                ProfileScreen(
                    onLogout = onLogout,
                    onChangePassword = onChangePassword
                )
            }

            composable(
                "change_password",
                enterTransition = { slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                ChangePasswordScreen(
                    onSuccess = onLogout
                )
            }
        }
    }
}