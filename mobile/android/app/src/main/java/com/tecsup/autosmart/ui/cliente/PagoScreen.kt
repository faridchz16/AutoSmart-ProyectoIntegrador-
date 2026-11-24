package com.tecsup.autosmart.ui.cliente

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R
import com.tecsup.autosmart.model.CostoOrdenDto
import com.tecsup.autosmart.model.MetodoPagoDto
import com.tecsup.autosmart.model.PagoDto
import com.tecsup.autosmart.ui.auth.fondoAuth
import com.tecsup.autosmart.viewmodel.METODO_TARJETA
import com.tecsup.autosmart.viewmodel.METODO_YAPE
import com.tecsup.autosmart.viewmodel.PagoUiState
import com.tecsup.autosmart.viewmodel.PagoViewModel
import com.tecsup.autosmart.viewmodel.TARJETAS_PRUEBA
import java.math.BigDecimal
import java.math.RoundingMode

private val MarcaFuerte = Color(0xFF1150E8)
private val Verde = Color(0xFF16A34A)
private val Ambar = Color(0xFFB45309)

private fun soles(monto: BigDecimal?): String =
    "S/ " + (monto ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun nombreMetodo(codigo: String?): String = when (codigo) {
    METODO_TARJETA -> "Tarjeta"
    METODO_YAPE -> "Yape / Plin"
    "EFECTIVO" -> "Efectivo en taller"
    null -> "-"
    else -> codigo
}

/**
 * Sprint 4 · HU-61, HU-62 y HU-63:
 * resumen del costo final (T61.2), selector de método (T62.2) y pago en línea con reintento (T63.2).
 */
@Composable
fun PagoScreen(
    idOrden: Long,
    viewModel: PagoViewModel,
    onBack: () -> Unit,
    idCita: Long = 0,
    placa: String = ""
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(idOrden, idCita) {
        if (idOrden > 0) viewModel.iniciar(idOrden) else viewModel.iniciarDesdeCita(idCita, placa)
    }
    BackHandler(enabled = estado.pagando) { }

    // T64.2: cuando llega el PDF, se guarda en Descargas y se abre
    val contexto = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(estado.pdfListo) {
        estado.pdfListo?.let { pdf -> viewModel.comprobanteGuardado(ComprobanteArchivo.guardarYAbrir(contexto, pdf)) }
    }
    // T65.2: no volver a pedir la calificación de un servicio ya calificado en este celular
    LaunchedEffect(estado.costo?.idOrden) {
        estado.costo?.idOrden?.let { if (CalificacionesLocales.contiene(contexto, it)) viewModel.marcarYaCalificado() }
    }
    LaunchedEffect(estado.calificacion, estado.yaCalificado) {
        val id = estado.costo?.idOrden
        if (id != null && (estado.calificacion != null || estado.yaCalificado)) CalificacionesLocales.guardar(contexto, id)
    }

    val c = coloresCliente()
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var confirmar by remember { mutableStateOf(false) }

    if (confirmar) {
        val costo = estado.costo
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Confirmar pago") },
            text = {
                Text("Vas a pagar ${soles(costo?.total)} con ${estado.metodo?.nombre ?: nombreMetodo(estado.metodoElegido)} " +
                    "por el servicio ${costo?.ticket.orEmpty()}.")
            },
            confirmButton = {
                TextButton(onClick = { confirmar = false; viewModel.pagar() }) { Text("Pagar ahora") }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.fondo)
            .verticalScroll(rememberScrollState())
    ) {
        EncabezadoPago(
            ticket = estado.costo?.ticket,
            horizontal = horizontal,
            puedeVolver = !estado.pagando,
            puedeActualizar = !estado.cargando && !estado.pagando,
            onVolver = onBack,
            onActualizar = viewModel::cargar
        )
        val solape = if (horizontal) 40.dp else 52.dp
        val mod = Modifier.padding(horizontal = 16.dp).offset(y = -solape)
        val costo = estado.costo

        when {
            costo == null && estado.cargando -> Tarjeta(mod) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = c.azul)
                    Spacer(Modifier.height(12.dp))
                    Text("Calculando el costo final...", color = c.textoSecundario)
                }
            }

            costo == null -> Tarjeta(mod) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(estado.error ?: "No se pudo cargar el costo.", color = c.rojo)
                    Spacer(Modifier.height(12.dp))
                    BotonPrincipal("Reintentar", onClick = viewModel::cargar)
                }
            }

            horizontal -> Row(mod, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ResumenCosto(costo, Modifier.weight(1f))
                SeccionPago(estado, viewModel, onBack, { confirmar = true }, Modifier.weight(1f))
            }

            else -> Column(mod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ResumenCosto(costo, Modifier)
                SeccionPago(estado, viewModel, onBack, { confirmar = true }, Modifier)
            }
        }
    }
}

@Composable
private fun EncabezadoPago(
    ticket: String?,
    horizontal: Boolean,
    puedeVolver: Boolean,
    puedeActualizar: Boolean,
    onVolver: () -> Unit,
    onActualizar: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .fondoAuth()
    ) {
        Image(
            painter = painterResource(R.drawable.img_welcome_auto),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(if (horizontal) 96.dp else 112.dp)
                .offset(x = 36.dp)
                .alpha(0.28f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 6.dp, end = 8.dp,
                top = if (horizontal) 10.dp else 20.dp,
                bottom = if (horizontal) 56.dp else 72.dp
            )
        ) {
            IconButton(onClick = onVolver, enabled = puedeVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Pagar servicio", color = Color.White, fontSize = if (horizontal) 24.sp else 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    ticket?.let { "Ticket $it" } ?: "Resumen y pago de tu mantenimiento",
                    color = Color(0xFFDCE6F8), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onActualizar, enabled = puedeActualizar) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar",
                    tint = Color.White.copy(alpha = if (puedeActualizar) 1f else 0.4f))
            }
        }
    }
}

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val c = coloresCliente()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.superficie,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, c.borde),
        modifier = modifier.fillMaxWidth()
    ) { Column(content = contenido) }
}

/** T61.2: servicio, adicionales aprobados, repuestos utilizados y total. */
@Composable
private fun ResumenCosto(costo: CostoOrdenDto, modifier: Modifier) {
    val c = coloresCliente()
    Tarjeta(modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Resumen del costo", color = c.texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            FilaMonto(costo.servicio?.nombre ?: "Servicio", "Servicio contratado", costo.servicio?.precio)

            val adicionales = costo.adicionales.orEmpty()
            if (adicionales.isNotEmpty()) {
                HorizontalDivider(color = c.borde)
                Text("Adicionales aprobados", color = c.textoSecundario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                adicionales.forEach { a ->
                    val detalle = listOfNotNull(
                        a.tipo?.lowercase()?.replaceFirstChar { it.uppercase() },
                        a.cantidad?.let { cant -> "$cant × ${soles(a.precioUnitario)}" }
                    ).joinToString(" · ")
                    FilaMonto(a.descripcion ?: "Adicional", detalle, a.subtotal)
                }
            }

            val repuestos = costo.repuestosUtilizados.orEmpty()
            if (repuestos.isNotEmpty()) {
                HorizontalDivider(color = c.borde)
                Text("Repuestos utilizados", color = c.textoSecundario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                repuestos.forEach { r ->
                    Text(
                        "• ${r.material ?: "Material"} — ${r.cantidad ?: 0} ${r.unidad.orEmpty()}".trim(),
                        color = c.texto, fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(color = c.borde)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total a pagar", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(soles(costo.total), color = c.azul, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Montos en ${costo.moneda ?: "PEN"} · incluye solo adicionales aprobados por ti.", color = c.textoSecundario, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FilaMonto(titulo: String, detalle: String, monto: BigDecimal?) {
    val c = coloresCliente()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, color = c.texto, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (detalle.isNotBlank()) Text(detalle, color = c.textoSecundario, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(soles(monto), color = c.texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Parte derecha/inferior: estado del pago, método (T62.2) y pago con reintento (T63.2). */
@Composable
private fun SeccionPago(
    estado: PagoUiState,
    viewModel: PagoViewModel,
    onBack: () -> Unit,
    onPagar: () -> Unit,
    modifier: Modifier
) {
    val costo = estado.costo ?: return
    val c = coloresCliente()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val aprobado = estado.resultado?.takeIf { it.estado == "APROBADO" }
        when {
            // Pago confirmado (recién hecho o ya existente en el servidor)
            aprobado != null || costo.pago != null -> PagoExitoso(
                codigo = aprobado?.codigo ?: costo.pago?.codigo,
                metodo = aprobado?.metodo ?: costo.pago?.metodo,
                fecha = aprobado?.fecha ?: costo.pago?.fecha,
                monto = aprobado?.monto ?: costo.total,
                onBack = onBack,
                descargando = estado.descargando,
                mensajeComprobante = estado.mensajeComprobante,
                onComprobante = viewModel::descargarComprobante
            )

            estado.sinConfirmar != null -> Aviso(
                icono = Icons.Outlined.WifiOff, color = Ambar, titulo = "Pago sin confirmar",
                texto = estado.sinConfirmar
            ) {
                BotonPrincipal(if (estado.cargando) "Comprobando..." else "Comprobar estado",
                    enabled = !estado.cargando, onClick = viewModel::comprobarEstado)
            }

            !costo.puedePagar -> Aviso(
                icono = Icons.Outlined.Info, color = c.azul, titulo = "Aún no puedes pagar",
                texto = "Podrás pagar cuando el mecánico finalice el servicio. Estado actual: " +
                    (costo.estadoCita ?: "-").replace('_', ' ').lowercase() + "."
            )

            else -> {
                estado.resultado?.takeIf { it.estado == "RECHAZADO" }?.let { rechazo ->
                    PagoRechazado(rechazo, onReintentar = viewModel::reintentar)
                }
                if (estado.resultado == null) {
                    SelectorMetodo(estado, viewModel)
                    DatosDelMetodo(estado, viewModel)
                    estado.errorDatos?.let { Text(it, color = c.rojo, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp)) }
                    if (estado.metodo?.enLinea == true) {
                        Button(
                            onClick = onPagar,
                            enabled = !estado.pagando,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            if (estado.pagando) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Procesando pago...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("Pagar ${soles(costo.total)}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        // T65.2: calificar al mecánico cuando el servicio ya terminó
        if (estado.puedeCalificar) TarjetaCalificacion(estado, viewModel)
    }
}

/** T65.2: estrellas de 1 a 5 y comentario opcional; solo se califica una vez por servicio. */
@Composable
private fun TarjetaCalificacion(estado: PagoUiState, viewModel: PagoViewModel) {
    val c = coloresCliente()
    val textos = listOf("", "Muy malo", "Malo", "Regular", "Bueno", "¡Excelente!")
    Tarjeta {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val hecha = estado.calificacion
            when {
                hecha != null -> {
                    Text("¡Gracias por tu calificación!", color = c.texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Estrellas(hecha.estrellas, tamano = 30, onElegir = null)
                    hecha.comentario?.takeIf { it.isNotBlank() }?.let { Text("“$it”", color = c.textoSecundario, fontSize = 14.sp) }
                    hecha.mecanico?.let { Text("Se la enviamos a $it.", color = c.textoSecundario, fontSize = 13.sp) }
                }
                estado.yaCalificado -> {
                    Text("Ya calificaste este servicio", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Solo se puede calificar una vez por servicio. ¡Gracias por tu opinión!", color = c.textoSecundario, fontSize = 14.sp)
                }
                else -> {
                    Text("¿Cómo te atendió el mecánico?", color = c.texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Tu calificación ayuda a otros clientes a elegir.", color = c.textoSecundario, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Estrellas(estado.estrellas, tamano = 36, onElegir = if (estado.calificando) null else viewModel::elegirEstrellas)
                        Spacer(Modifier.width(10.dp))
                        Text(textos[estado.estrellas.coerceIn(0, 5)], color = c.azul, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        value = estado.comentario,
                        onValueChange = viewModel::editarComentario,
                        label = { Text("Comentario (opcional)") },
                        enabled = !estado.calificando,
                        minLines = 2, maxLines = 4,
                        supportingText = { Text("${estado.comentario.length}/500") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    estado.errorCalificacion?.let { Text(it, color = c.rojo, fontSize = 14.sp) }
                    BotonPrincipal(if (estado.calificando) "Enviando..." else "Enviar calificación",
                        enabled = !estado.calificando && estado.estrellas > 0, onClick = viewModel::calificar)
                }
            }
        }
    }
}

@Composable
private fun Estrellas(valor: Int, tamano: Int, onElegir: ((Int) -> Unit)?) {
    Row {
        (1..5).forEach { n ->
            val llena = n <= valor
            Icon(
                if (llena) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "$n estrellas",
                tint = if (llena) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                modifier = Modifier
                    .size(tamano.dp)
                    .then(if (onElegir != null) Modifier.clickable { onElegir(n) } else Modifier)
            )
        }
    }
}

/** T62.2: lista de métodos habilitados que devuelve el backend. */
@Composable
private fun SelectorMetodo(estado: PagoUiState, viewModel: PagoViewModel) {
    val c = coloresCliente()
    Tarjeta {
        Column(Modifier.padding(vertical = 14.dp)) {
            Text("Método de pago", color = c.texto, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(6.dp))
            estado.errorMetodos?.takeIf { estado.metodos.isEmpty() }?.let {
                Text(it, color = c.rojo, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp))
                TextButton(onClick = viewModel::cargar, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Reintentar") }
            }
            estado.metodos.forEach { metodo ->
                OpcionMetodo(
                    metodo = metodo,
                    seleccionado = metodo.codigo == estado.metodoElegido,
                    habilitado = !estado.pagando,
                    onClick = { viewModel.elegirMetodo(metodo.codigo) }
                )
            }
        }
    }
}

@Composable
private fun OpcionMetodo(metodo: MetodoPagoDto, seleccionado: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    val c = coloresCliente()
    val icono: ImageVector = when (metodo.codigo) {
        METODO_TARJETA -> Icons.Outlined.CreditCard
        METODO_YAPE -> Icons.Outlined.PhoneAndroid
        else -> Icons.Outlined.Payments
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (seleccionado) c.azulSuave else Color.Transparent)
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (seleccionado) MarcaFuerte else c.azulSuave),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = if (seleccionado) Color.White else c.azul, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(metodo.nombre ?: nombreMetodo(metodo.codigo), color = c.texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(if (metodo.enLinea) "Pago en línea desde la app" else "Pagas en caja del taller",
                color = c.textoSecundario, fontSize = 13.sp)
        }
        RadioButton(selected = seleccionado, onClick = onClick, enabled = habilitado,
            colors = RadioButtonDefaults.colors(selectedColor = MarcaFuerte))
    }
}

/** Datos según el método: tarjeta de prueba, código de Yape/Plin o instrucciones de efectivo. */
@Composable
private fun DatosDelMetodo(estado: PagoUiState, viewModel: PagoViewModel) {
    val c = coloresCliente()
    val metodo = estado.metodo ?: return
    Tarjeta {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                metodo.codigo == METODO_TARJETA -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tarjeta", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        EtiquetaPrueba()
                    }
                    Text("Elige una tarjeta de prueba. No se usan tarjetas reales ni se guardan sus datos.",
                        color = c.textoSecundario, fontSize = 13.sp)
                    TARJETAS_PRUEBA.forEach { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !estado.pagando) { viewModel.elegirTarjeta(t.token) }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = estado.tarjeta == t.token, onClick = { viewModel.elegirTarjeta(t.token) },
                                enabled = !estado.pagando, colors = RadioButtonDefaults.colors(selectedColor = MarcaFuerte))
                            Column(Modifier.weight(1f)) {
                                Text(t.nombre, color = c.texto, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(t.resultado, color = c.textoSecundario, fontSize = 12.sp)
                            }
                        }
                    }
                }

                metodo.codigo == METODO_YAPE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Yape / Plin", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        EtiquetaPrueba()
                    }
                    Text("Abre tu app Yape o Plin, genera el código de aprobación y escríbelo aquí. En modo de prueba usa 123456.",
                        color = c.textoSecundario, fontSize = 13.sp)
                    OutlinedTextField(
                        value = estado.codigoYape,
                        onValueChange = viewModel::editarCodigoYape,
                        label = { Text("Código de aprobación") },
                        singleLine = true,
                        enabled = !estado.pagando,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        supportingText = { Text("${estado.codigoYape.length}/6") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                !metodo.enLinea -> {
                    Text("Pago en efectivo", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Acércate a la caja del taller con tu ticket ${estado.costo?.ticket.orEmpty()} y paga ${soles(estado.costo?.total)}. " +
                            "El administrador registrará tu pago y aquí verás el servicio como pagado.",
                        color = c.texto, fontSize = 14.sp
                    )
                }

                else -> Text("Este método todavía no está disponible en la app.", color = c.textoSecundario)
            }
        }
    }
}

@Composable
private fun EtiquetaPrueba() {
    Surface(shape = RoundedCornerShape(10.dp), color = Ambar.copy(alpha = 0.15f)) {
        Text("Modo de prueba", color = Ambar, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun PagoRechazado(pago: PagoDto, onReintentar: () -> Unit) {
    val c = coloresCliente()
    Aviso(
        icono = Icons.Outlined.ErrorOutline, color = c.rojo, titulo = "Pago rechazado",
        texto = (pago.motivoRechazo ?: "La pasarela rechazó el pago.") +
            ". No se realizó ningún cobro. Puedes intentarlo de nuevo o elegir otro método."
    ) {
        BotonPrincipal("Intentar de nuevo", onClick = onReintentar)
    }
}

@Composable
private fun PagoExitoso(
    codigo: String?, metodo: String?, fecha: String?, monto: BigDecimal?, onBack: () -> Unit,
    descargando: Boolean = false, mensajeComprobante: String? = null, onComprobante: () -> Unit = {}
) {
    val c = coloresCliente()
    Tarjeta {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Verde, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(8.dp))
            Text("¡Pago realizado!", color = c.texto, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(soles(monto), color = Verde, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            DatoPago("Código", codigo ?: "-")
            DatoPago("Método", nombreMetodo(metodo))
            DatoPago("Fecha", fecha?.replace('T', ' ') ?: "-")
            Spacer(Modifier.height(16.dp))
            // T64.2: comprobante en PDF
            OutlinedButton(
                onClick = onComprobante, enabled = !descargando, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (descargando) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (descargando) "Descargando..." else "Descargar comprobante (PDF)")
            }
            mensajeComprobante?.let {
                Text(it, color = c.textoSecundario, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            BotonPrincipal("Listo", onClick = onBack)
        }
    }
}

@Composable
private fun DatoPago(etiqueta: String, valor: String) {
    val c = coloresCliente()
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(etiqueta, color = c.textoSecundario, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(valor, color = c.texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Aviso(
    icono: ImageVector,
    color: Color,
    titulo: String,
    texto: String,
    acciones: @Composable ColumnScope.() -> Unit = {}
) {
    val c = coloresCliente()
    Tarjeta {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text(titulo, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Text(texto, color = c.texto, fontSize = 14.sp)
            acciones()
        }
    }
}

@Composable
private fun BotonPrincipal(texto: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) { Text(texto, fontWeight = FontWeight.SemiBold) }
}
