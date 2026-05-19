package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow

// ─── Data class que representa a cada colaborador ───────────────────────────
// Todos los campos opcionales son nullable: la card solo renderiza lo que existe
data class Contributor(
    val name: String,
    val role: String? = null,
    val avatarUrl: String? = null,
    val github: String? = null,
    val linkedin: String? = null,
    val website: String? = null,
    val email: String? = null
)

// ─── Screen principal de Créditos ───────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit
) {
    // Lista de colaboradores — en el futuro puede cargarse desde assets/API
    val contributors = remember { getContributors() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créditos", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            // Encabezado de la sección
            item {
                AppInfoHeader()
            }

            item {
                Text(
                    text = "Equipo de desarrollo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                HorizontalDivider()
            }

            // Una card por cada colaborador
            items(contributors) { contributor ->
                ContributorCard(contributor = contributor)
            }
        }
    }
}

// ─── Header con info general de la app ──────────────────────────────────────
@Composable
private fun AppInfoHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "SIFA GO",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Versión 1.0.0",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Aplicación de fiscalización desarrollada\npor el equipo SIFA.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─── Card individual de colaborador ─────────────────────────────────────────
@Composable
fun ContributorCard(contributor: Contributor) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Fila superior: avatar + nombre + rol
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar: imagen desde URL o ícono fallback
                ContributorAvatar(avatarUrl = contributor.avatarUrl)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contributor.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contributor.role != null) {
                        Text(
                            text = contributor.role,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sección de links — solo renderiza si al menos uno existe
            val hasLinks = listOf(
                contributor.github,
                contributor.linkedin,
                contributor.website,
                contributor.email
            ).any { it != null }

            if (hasLinks) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Botones de links en grid de 2 columnas
                val links = buildList {
                    contributor.github?.let { add(Triple("GitHub", Icons.Filled.Code, it)) }
                    contributor.linkedin?.let { add(Triple("LinkedIn", Icons.Filled.Link, it)) }
                    contributor.website?.let { add(Triple("Website", Icons.Filled.Language, it)) }
                    contributor.email?.let {
                        add(
                            Triple(
                                "Email",
                                Icons.Filled.Person,
                                "mailto:$it"
                            )
                        )
                    }
                }

                // Dividir en filas de 2
                links.chunked(2).forEach { rowLinks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLinks.forEach { (label, icon, url) ->
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 0.dp
                                )
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        // Si la fila tiene 1 solo elemento, agregar Spacer para balance
                        if (rowLinks.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

// ─── Componente Avatar ───────────────────────────────────────────────────────
@Composable
private fun ContributorAvatar(avatarUrl: String?) {
    if (avatarUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            // Fallback visual si falla la carga
            error = androidx.compose.ui.res.painterResource(
                id = android.R.drawable.ic_menu_myplaces // ícono genérico del sistema
            )
        )
    } else {
        // Sin avatar_url: círculo con ícono
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// ─── Datos de ejemplo (reemplazar por carga desde assets/API) ────
// Agrega más colaboradores aquí siguiendo la misma estructura
private fun getContributors(): List<Contributor> = listOf(
    Contributor(
        name = "Nicolas López",
        role = "Full Stack Developer",
        avatarUrl = "https://github.com/Nicolas-15.png",
        github = "https://github.com/Nicolas-15/",
        linkedin = null,
        website = null,
        email = null
    ),
    Contributor(
        name = "Leonel Briones Palacios",
        role = "Backend Developer",
        avatarUrl = "https://github.com/jarodsmdev.png",
        github = "https://github.com/jarodsmdev",
        linkedin = "https://www.linkedin.com/in/leonel-briones-palacios/",
        website = "https://leonel-briones.netlify.app/",
        email = "lbriones.dev@gmail.com"
    ),
    Contributor(
        name = "Andrés Ortega",
        role = "Full Stack Developer",
        avatarUrl = "https://github.com/Andythe20.png",
        github = "https://github.com/Andythe20/Proyecto-Fullstack2-React",
        linkedin = "https://www.linkedin.com/in/andres-ortega-suazo/",
        website = null,
        email = "an.ortegas@duocuc.cl"
    )
)