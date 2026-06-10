package com.sifa.sifa_go.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifa.sifa_go.core.utils.PasswordValidator

@Composable
fun PasswordRequirementsIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val requirements = PasswordValidator.validate(password)
    val isValid = requirements.all { it.isValid }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isValid && password.isNotEmpty())
            Color(0xFFE8F5E9)
        else if (password.isNotEmpty())
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isValid && password.isNotEmpty())
                    "Contraseña válida"
                else if (password.isEmpty())
                    "La contraseña debe cumplir:"
                else
                    "Requisitos pendientes:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isValid && password.isNotEmpty())
                    Color(0xFF2E7D32)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                requirements.forEach { requirement ->
                    PasswordRequirementRow(requirement = requirement)
                }
            }
        }
    }
}

@Composable
private fun PasswordRequirementRow(
    requirement: PasswordValidator.Requirement
) {
    val iconColor by animateColorAsState(
        targetValue = if (requirement.isValid)
            Color(0xFF4CAF50)
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        label = "iconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (requirement.isValid)
            Color(0xFF2E7D32)
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "textColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (requirement.isValid) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = requirement.label,
            fontSize = 13.sp,
            color = textColor,
            fontWeight = if (requirement.isValid) FontWeight.Medium else FontWeight.Normal
        )
    }
}
