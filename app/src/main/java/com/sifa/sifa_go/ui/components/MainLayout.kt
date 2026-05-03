package com.sifa.sifa_go.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

// Data class para manejar los items del menú de forma limpia
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    title: String = "",
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "scan", // Ruta actual para marcar el ícono activo
    onNavigate: (String) -> Unit = {}, // Función para cambiar de vista
    content: @Composable (PaddingValues) -> Unit
) {
    // Definimos las 4 opciones de tu menú inferior
    val bottomNavItems = listOf(
        BottomNavItem("Escanear", Icons.Filled.CameraAlt, "scan"),
        BottomNavItem("Historial", Icons.Filled.History, "history"),
        BottomNavItem("Reportes", Icons.Filled.Assessment, "reports"),
        BottomNavItem("Perfil", Icons.Filled.Person, "profile")
    )

    // Identificamos si la ruta actual es una pestaña principal, ya que las vistas raíz
    // no deben tener la opción de navegar hacia atrás
    val isRootTab = bottomNavItems.any { it.route == currentRoute }

    Scaffold (
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    // Solo mostramos la flecha si NO estamos en una pestaña raíz
                    if (!isRootTab) {
                        IconButton(onClick = onBackClick) {
                            // Usamos AutoMirrored para soporte RTL automático si es necesario
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver atrás"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Perfil del fiscalizador"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    ) { paddingValues ->
        // Aquí adentro se inyectará la vista que corresponda (como tu CameraScreen)
        content(paddingValues)
    }
}