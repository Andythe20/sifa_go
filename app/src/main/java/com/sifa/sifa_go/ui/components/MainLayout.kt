package com.sifa.sifa_go.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifa.sifa_go.ui.theme.SIFA_GOTheme
import com.sifa.sifa_go.viewmodel.PresenceViewModel

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    presenceViewModel: PresenceViewModel? = null,
    title: String = "",
    username: String = "",
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onHelp: () -> Unit = {},
    onCredits: () -> Unit = {},
    onLogout: () -> Unit = {},
    currentRoute: String = "scan",
    onNavigate: (String) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val bottomNavItems = listOf(
        BottomNavItem("Escanear", Icons.Filled.CameraAlt, "scan"),
        BottomNavItem("Historial", Icons.Filled.History, "history"),
        BottomNavItem("Inicio", Icons.Filled.Home, "home"),
        BottomNavItem("Resumen", Icons.Filled.Assessment, "overview"),
        BottomNavItem("Perfil", Icons.Filled.Person, "profile")
    )

    val isRootTab = bottomNavItems.any { it.route == currentRoute }

    val isScanFlow = currentRoute.startsWith("fiscalizacion/") || currentRoute == "scan"

    val heartbeatTrigger by presenceViewModel?.heartbeatTrigger?.collectAsState() ?: remember { mutableStateOf(0L) }
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(heartbeatTrigger) {
        if (heartbeatTrigger > 0) {
            pulseAnim.snapTo(1f)
            pulseAnim.animateTo(1.4f, tween(150, easing = EaseOutCubic))
            pulseAnim.animateTo(1f, tween(300, easing = EaseInCubic))
        }
    }
    val dotScale = if (pulseAnim.value > 1.01f) pulseAnim.value else breathingScale

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (username.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 2.dp)
                            ) {
                                val user = username.substringBefore("@")
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .graphicsLayer(scaleX = dotScale, scaleY = dotScale)
                                                .background(Color(0xFF4CAF50), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = user,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                GlobalMenu(
                                    showLogout = true,
                                    onHelp = onHelp,
                                    onCredits = onCredits,
                                    onLogout = onLogout
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    if (!isRootTab) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver atrás",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                for (item in bottomNavItems) {
                    val isSelected = if (item.route == "scan") isScanFlow else currentRoute == item.route

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onNavigate(item.route) },
                        icon = {
                            if (isSelected) {
                                val liftOffset by animateDpAsState(
                                    targetValue = (-6).dp,
                                    animationSpec = spring(
                                        dampingRatio = 0.5f,
                                        stiffness = 300f
                                    ),
                                    label = "liftOffset"
                                )
                                Surface(
                                    modifier = Modifier.offset(y = liftOffset),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    tonalElevation = 2.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.sp,
                                                fontSize = 9.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content(PaddingValues(0.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainLayoutPreview() {
    SIFA_GOTheme {
        MainLayout(
            title = "SIFA GO",
            username = "andresortegasuazooo.fis@gmail.com",
            currentRoute = "home",
            onNavigate = {}
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("Contenido de la Vista")
            }
        }
    }
}
