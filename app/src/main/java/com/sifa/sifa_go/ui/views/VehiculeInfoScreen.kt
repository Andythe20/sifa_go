package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Report
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.ui.theme.SIFA_GOTheme

@Composable
fun VehicleInfoScreen(
    vehicleData: PlateInfoResponse,
    onIssueFineClick: () -> Unit,
    onNewScanClick: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Nuevo Escaneo") },
            text = { Text("¿Está seguro que desea realizar un nuevo escaneo? Se perderán los datos actuales.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showConfirmDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            onNewScanClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Escanear")
                    }
                }
            },
            dismissButton = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Titulo pequeño encima
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .padding(top = 20.dp, start = 15.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "CONSULTA DEL VEHÍCULO",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelLarge
            )
        }
        // Título Principal
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .padding(bottom = 16.dp, start = 15.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "FICHA DEL VEHÍCULO",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Tarjeta principal con la patente y el año
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp) // Ancho de la franja de color
                        .background(MaterialTheme.colorScheme.primary) // O el color que prefieras
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "PATENTE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Text(
                            text = "AÑO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)

                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 10.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = vehicleData.patente,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Text(
                            text = vehicleData.anio_fabricacion.toString(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)

                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // tarjeta con marca y modelo
        InfoCard("MARCA / MODELO", vehicleData.marca, vehicleData.modelo)

        Spacer(modifier = Modifier.height(6.dp))

        // tajeta con el color
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "COLOR",
                    color = Color.DarkGray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = vehicleData.color,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // tarjeta con nro motor
        InfoCard("NRO MOTOR", vehicleData.nro_motor)

        Spacer(modifier = Modifier.height(6.dp))

        // tarjeta con el nro chasis
        InfoCard("NRO CHASIS", vehicleData.nro_serie)

        Spacer(modifier = Modifier.height(6.dp))

        // tarjeta con el propietario y rut
        InfoCard("PROPIETARIO / RUT", vehicleData.propietario, vehicleData.rut)

        Spacer(modifier = Modifier.height(40.dp))

        // Botones de acción
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp)
        ) {
            Button(
                onClick = onIssueFineClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                // 1. Hacemos el contenedor del botón transparente para ver el gradiente
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                // 2. Cambiamos la forma a esquinas menos redondeadas (ejemplo: 8.dp)
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                // 3. Eliminamos el padding interno para que el gradiente ocupe todo el espacio
                contentPadding = androidx.compose.foundation.layout.PaddingValues()
            ) {
                // 4. Usamos un Box para aplicar el gradiente y centrar el texto
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBrus),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Report,
                            contentDescription = "Emitir infracción",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = "EMITIR INFRACCIÓN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showConfirmDialog = true },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = "Nuevo escaneo",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = "NUEVO ESCANEO",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Componente reutilizable para cada tarjeta con datos
@Composable
fun InfoCard(label: String, value: String, value2: String? = null) {
    var bottomDp = 10
    if (value2 != null) {
        bottomDp = 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                color = Color.DarkGray.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomDp.dp)
        ) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (value2 != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 10.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = value2,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

// Colores del gradiente del botón
val gradientBrus = Brush.horizontalGradient(
    colors = listOf(
        Color(0, 32, 67),
        Color(0, 50, 100)
    )
)


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun VehicleInfoScreenPreview() {
    // 2. Envuelves la vista en tu tema para que respete tus colores y tipografías
    SIFA_GOTheme {
        // 3. Llamas a tu vista pasándole "Datos Falsos" (Mocks) para que tenga qué dibujar
        VehicleInfoScreen(
            vehicleData = PlateInfoResponse(
                patente = "GKSB78",
                marca = "TOYOTA",
                modelo = "YARIS",
                anio_fabricacion = 2020,
                color = "ROJO",
                nro_motor = "1NZFE1234567",
                nro_serie = "JTD1234567890",
                rut = "12.345.678-9",
                propietario = "JUAN PEREZ"
            ),
            onIssueFineClick = {}, // Funciones vacías porque aquí no hay lógica
            onNewScanClick = {}
        )
    }
}