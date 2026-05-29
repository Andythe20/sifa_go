package com.sifa.sifa_go.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra de paginacion con iconos de anterior/siguiente e indicador de pagina actual.
 * Solo se renderiza cuando hay mas de una pagina.
 *
 * @param currentPage pagina actual (1-indexado para mostrar al usuario)
 * @param totalPages total de paginas
 * @param isFirstPage true si esta en la primera pagina
 * @param isLastPage true si esta en la ultima pagina
 * @param onPreviousPage callback al presionar anterior
 * @param onNextPage callback al presionar siguiente
 */
@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    isFirstPage: Boolean,
    isLastPage: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    // Solo se muestra si hay más de una página
    if (totalPages > 1) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousPage,
                enabled = !isFirstPage
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Anterior",
                    tint = if (!isFirstPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "$currentPage/$totalPages",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onNextPage,
                enabled = !isLastPage
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Siguiente",
                    tint = if (!isLastPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
