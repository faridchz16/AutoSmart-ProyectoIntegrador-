package com.tecsup.autosmart.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.data.model.CitaCreadaResponse
import java.math.RoundingMode
import java.util.Locale

@Composable
fun TicketConfirmacionDialog(
    cita: CitaCreadaResponse,
    onFinalizar: () -> Unit
) {
    val estado = cita.estado.uppercase(Locale.ROOT)
    val aceptada = estado == "CONFIRMADA"

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = if (aceptada) "Cita aceptada" else "Comprobante de reserva",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ticket: ${cita.ticket}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (estado) {
                        "PENDIENTE" -> "Reserva registrada. Pendiente de aceptación por el mecánico."
                        "CONFIRMADA" -> "Tu cita fue aceptada por el mecánico."
                        else -> "Consulta el estado de tu reserva a continuación."
                    }
                )
                HorizontalDivider()
                Text("Estado: ${cita.estado}")
                Text("Servicio: ${cita.servicio}")
                Text("Fecha: ${cita.fecha}")
                Text("Hora de inicio: ${cita.horaInicio.take(5)}")
                Text("Hora de fin: ${cita.horaFin.take(5)}")
                Text("Mecánico: ${cita.mecanico}")
                Text("Placa: ${cita.placa}")
                Text(
                    text = "Precio: S/ ${cita.precio.setScale(2, RoundingMode.HALF_UP).toPlainString()}",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(onClick = onFinalizar) {
                Text("Finalizar")
            }
        }
    )
}
