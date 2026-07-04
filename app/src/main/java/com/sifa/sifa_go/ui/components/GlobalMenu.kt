package com.sifa.sifa_go.ui.components

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GlobalMenu(
    showLogout: Boolean = true,
    onHelp: () -> Unit,
    onCredits: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Más opciones",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        DropdownMenuItem(
            text = { Text("Ayuda") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null
                )
            },
            onClick = {
                expanded = false
                onHelp()
            }
        )

        DropdownMenuItem(
            text = { Text("Créditos") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null
                )
            },
            onClick = {
                expanded = false
                onCredits()
            }
        )

        if (showLogout) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Cerrar Sesión") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    showLogoutDialog = true
                }
            )
        }
    }

    LogoutConfirmationDialog(
        show = showLogoutDialog,
        onConfirm = {
            showLogoutDialog = false
            onLogout()
        },
        onDismiss = { showLogoutDialog = false }
    )
}
