package com.tecsup.autosmart

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.tecsup.autosmart.model.ChecklistItemDto
import com.tecsup.autosmart.model.ChecklistRequest
import com.tecsup.autosmart.model.ChecklistResponse
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.model.DecisionPresupuesto
import com.tecsup.autosmart.model.DeficienciaResponse
import com.tecsup.autosmart.model.DetalleMantenimientoDto
import com.tecsup.autosmart.model.FotoReferenciaDto
import com.tecsup.autosmart.model.HitoSeguimiento
import com.tecsup.autosmart.model.SeguimientoDto
import com.tecsup.autosmart.model.OrdenTrabajoDto
import com.tecsup.autosmart.model.PresupuestoItemDto
import com.tecsup.autosmart.model.PresupuestoResponse
import com.tecsup.autosmart.ui.cliente.DetalleMantenimientoScreen
import com.tecsup.autosmart.ui.cliente.MisCitasScreen
import com.tecsup.autosmart.ui.cliente.PresupuestoScreen
import com.tecsup.autosmart.ui.cliente.SeguimientoScreen
import com.tecsup.autosmart.ui.mecanico.ChecklistScreen
import com.tecsup.autosmart.ui.mecanico.DeficienciasScreen
import com.tecsup.autosmart.ui.mecanico.FotosDeficienciaScreen
import com.tecsup.autosmart.ui.mecanico.OrdenesMecanicoScreen
import com.tecsup.autosmart.ui.theme.AutoSmartAppTheme
import com.tecsup.autosmart.viewmodel.ChecklistViewModel
import com.tecsup.autosmart.viewmodel.DeficienciasViewModel
import com.tecsup.autosmart.viewmodel.DetalleMantenimientoViewModel
import com.tecsup.autosmart.viewmodel.FotosDeficienciaViewModel
import com.tecsup.autosmart.viewmodel.MisCitasViewModel
import com.tecsup.autosmart.viewmodel.OrdenesMecanicoViewModel
import com.tecsup.autosmart.viewmodel.PresupuestoViewModel
import com.tecsup.autosmart.viewmodel.SeguimientoViewModel
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class FlujosSprint03Test {
    @get:Rule
    val compose = createComposeRule()

    private val orden = OrdenTrabajoDto(81, 12, "AS-12", "EN_PROCESO",
        "2026-11-06T09:00:00", "Mantenimiento", "ABC-123")
    private val foto = FotoReferenciaDto(7, "/photos/7")
    private val deficiencia = DeficienciaResponse(24, 81, "FRENOS", "CRITICA",
        "Desgaste de pastillas", "2026-11-06T09:10:00", listOf(foto))
    private val cita = CitaCreadaResponse(12, "AS-12", "ESPERA_APROBACION", "2026-11-06",
        "09:00:00", "10:00:00", BigDecimal("100.00"), "Mantenimiento", "Mecánico", "ABC-123")
    private val presupuesto = PresupuestoResponse(9, 81, 12, "PENDIENTE",
        "Reparación adicional", "2026-11-06T10:00:00", null,
        listOf(PresupuestoItemDto("REPUESTO", "Pastillas", 2, BigDecimal("12.30"), BigDecimal("24.60"))),
        BigDecimal("24.60"), listOf(deficiencia))

    @Test
    fun ordenAbreChecklistGuardaYPermiteConsultarEvidencia() {
        val items = listOf("LUCES", "FRENOS", "NEUMATICOS", "FLUIDOS", "CARROCERIA")
            .map { ChecklistItemDto(it, "OK") }
        var guardado: ChecklistRequest? = null
        val ordenes = OrdenesMecanicoViewModel(consultar = { Result.success(listOf(orden)) })
        val checklist = ChecklistViewModel(
            consultar = { id ->
                assertEquals(81L, id)
                Result.success(ChecklistResponse(5, id, "2026-11-06T09:00:00", null, items))
            },
            enviar = { id, request ->
                assertEquals(81L, id)
                guardado = request
                Result.success(ChecklistResponse(5, id, "2026-11-06T09:30:00",
                    request.observacionGeneral, request.items))
            }
        )
        val deficiencias = DeficienciasViewModel(consultar = { id ->
            assertEquals(81L, id)
            Result.success(listOf(deficiencia))
        }, enviar = { _, _ -> error("La prueba solo consulta deficiencias") })
        val fotos = FotosDeficienciaViewModel(
            SavedStateHandle(),
            consultar = { id, def ->
                assertEquals(81L, id)
                assertEquals(24L, def)
                Result.success(listOf(foto))
            },
            enviar = { _, _, _ -> error("La prueba solo consulta fotos") },
            ver = ::leerFoto
        )
        compose.setContent {
            AutoSmartAppTheme {
                var pantalla by remember { mutableStateOf("orden") }
                var idOrden by remember { mutableStateOf(0L) }
                var idDeficiencia by remember { mutableStateOf(0L) }
                var editable by remember { mutableStateOf(false) }
                when (pantalla) {
                    "checklist" -> ChecklistScreen(idOrden, editable, checklist) { pantalla = "orden" }
                    "deficiencias" -> DeficienciasScreen(idOrden, editable, deficiencias,
                        onBack = { pantalla = "orden" },
                        onFotos = { idDeficiencia = it; pantalla = "fotos" })
                    "fotos" -> FotosDeficienciaScreen(idOrden, idDeficiencia, editable, fotos) {
                        pantalla = "deficiencias"
                    }
                    else -> OrdenesMecanicoScreen(ordenes, onBack = {}, onAgenda = {},
                        onChecklist = { id, puedeEditar ->
                            idOrden = id
                            editable = puedeEditar
                            pantalla = "checklist"
                        },
                        onDeficiencias = { id, puedeEditar ->
                            idOrden = id
                            editable = puedeEditar
                            pantalla = "deficiencias"
                        })
                }
            }
        }

        compose.onNodeWithText("Ver detalle").performClick()
        compose.onNodeWithText("Checklist de recepción").performClick()
        scrollHasta("Observación general opcional")
        compose.onNodeWithText("Observación general opcional").performTextReplacement("Recepción verificada")
        scrollHasta("Guardar checklist")
        compose.onNodeWithText("Guardar checklist").performClick()
        scrollHasta("Último registro: 2026-11-06 09:30:00")
        compose.runOnIdle {
            assertEquals("Recepción verificada", guardado?.observacionGeneral)
            assertEquals(items, guardado?.items)
        }
        compose.onNodeWithContentDescription("Volver a la orden").performClick()
        compose.onNodeWithText("Deficiencias").performClick()
        scrollHasta("Fotos de evidencia")
        compose.onNodeWithText("Fotos de evidencia").performClick()
        compose.onNodeWithText("Orden: 81 · Deficiencia: 24").assertIsDisplayed()
        scrollHasta("Foto 7")
        esperarFoto("Foto 7 de la deficiencia")
        compose.onNodeWithContentDescription("Volver a deficiencias").performClick()
        compose.onNodeWithContentDescription("Volver a la orden").performClick()
        compose.onNodeWithText("Detalle de la orden").assertIsDisplayed()
    }

    @Test
    fun misCitasAbrePresupuestoConImporteEvidenciaYRegreso() {
        mostrarMisCitas { Result.success(presupuesto) }
        abrirPresupuesto()

        compose.onNodeWithText("Estado: Pendiente de aprobación").assertIsDisplayed()
        scrollHasta("Total: S/ 24.60")
        compose.onNodeWithText("Total: S/ 24.60").assertIsDisplayed()
        scrollHasta("Deficiencia 24")
        esperarFoto("Foto 7 del presupuesto")
        compose.onNodeWithContentDescription("Volver a Mis citas").performClick()
        compose.onNodeWithText("Mis Citas").assertIsDisplayed()
    }

    @Test
    fun presupuestoDistingueAusenciaDeErrorYRecuperaLaConsulta() {
        var respuesta: Result<PresupuestoResponse?> = Result.success(null)
        mostrarMisCitas { respuesta }
        abrirPresupuesto()
        compose.onNodeWithText("Esta cita aún no tiene presupuesto.").assertIsDisplayed()

        compose.runOnIdle { respuesta = Result.failure(IllegalStateException("Sesión expirada")) }
        compose.onNodeWithText("Actualizar").performClick()
        compose.onNodeWithText("Sesión expirada").assertIsDisplayed()
        compose.onNodeWithText("Esta cita aún no tiene presupuesto.").assertDoesNotExist()

        compose.runOnIdle { respuesta = Result.success(presupuesto) }
        compose.onNodeWithText("Reintentar").performClick()
        compose.onNodeWithText("Estado: Pendiente de aprobación").assertIsDisplayed()
        compose.onNodeWithText("Sesión expirada").assertDoesNotExist()
    }

    @Test
    fun aprobarPideConfirmacionYEsperaRespuestaDelServidor() {
        val respuesta = CompletableDeferred<Result<PresupuestoResponse>>()
        var solicitudes = 0
        mostrarMisCitas(enviarDecision = { p, decision ->
            assertEquals(9L, p.idPresupuesto)
            assertEquals(DecisionPresupuesto.APROBADO, decision)
            solicitudes++
            respuesta.await()
        }) { Result.success(presupuesto) }
        abrirPresupuesto()
        scrollHasta("Aprobar")
        compose.onNodeWithText("Aprobar").performClick()
        compose.runOnIdle { assertEquals(0, solicitudes) }
        compose.onNodeWithText("Confirmar").performClick()
        compose.onNodeWithText("Registrando decisión...").assertExists()
        compose.onNodeWithText("Aprobar").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, solicitudes) }

        respuesta.complete(Result.success(presupuesto.copy(estado = "APROBADO",
            fechaDecision = "2026-11-06T10:30:00")))
        scrollHasta("Estado: Aprobado")
        compose.onNodeWithText("Estado: Aprobado").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, solicitudes) }
    }

    @Test
    fun misCitasMuestraLaLineaDeTiempoYActualizaElAvance() {
        var avance = 33
        val citas = MisCitasViewModel(consultar = { Result.success(listOf(cita)) })
        val seguimiento = SeguimientoViewModel(consultar = { id ->
            assertEquals(12L, id)
            Result.success(SeguimientoDto(id, "AS-12", "EN_PROCESO", "Mantenimiento",
                "ABC-123", "Mecánico", "2026-11-06", "09:00", avance,
                listOf(
                    HitoSeguimiento("RECIBIDO", "Vehículo recibido en el taller", true,
                        "2026-11-06T09:10", null),
                    HitoSeguimiento("FINALIZADA", "Vehículo listo para recoger", false, null, null)
                )))
        })
        compose.setContent {
            AutoSmartAppTheme {
                var idCita by remember { mutableStateOf<Long?>(null) }
                val id = idCita
                if (id == null) {
                    MisCitasScreen(viewModel = citas, onSeguimiento = { idCita = it })
                } else {
                    SeguimientoScreen(id, seguimiento) { idCita = null }
                }
            }
        }
        compose.onNodeWithText("Ver seguimiento").performScrollTo().performClick()
        compose.onNodeWithText("Avance: 33 %").assertIsDisplayed()
        scrollHasta("Vehículo recibido en el taller")
        compose.onNodeWithText("Completado").assertIsDisplayed()
        scrollHasta("Vehículo listo para recoger")
        compose.onNodeWithText("Pendiente").assertIsDisplayed()
        compose.runOnIdle { avance = 67 }
        compose.onNodeWithText("Actualizar").performClick()
        scrollHasta("Avance: 67 %")
        compose.onNodeWithText("Avance: 67 %").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver a Mis citas").performClick()
        compose.onNodeWithText("Mis Citas").assertIsDisplayed()
    }

    @Test
    fun desdeLaOrdenConsultaElMantenimientoYVuelveAlDetalle() {
        val ordenes = OrdenesMecanicoViewModel(consultar = { Result.success(listOf(orden)) })
        val detalle = DetalleMantenimientoViewModel(consultar = { id ->
            assertEquals(81L, id)
            Result.success(DetalleMantenimientoDto(
                idOrden = id, idCita = 12, ticket = "AS-12", estado = "EN_PROCESO",
                servicio = "Mantenimiento", precioServicio = BigDecimal("100.00"),
                placa = "ABC-123", vehiculo = "Toyota Yaris", mecanico = "Mecánico",
                fechaInicio = "2026-11-06T09:00:00", fechaFin = null,
                diagnostico = "Desgaste de frenos", checklist = null, deficiencias = emptyList(),
                tareas = null, presupuesto = null, repuestosConsumidos = emptyList(),
                costoRepuestos = BigDecimal.ZERO, totalEstimado = BigDecimal("100.00")
            ))
        })
        compose.setContent {
            AutoSmartAppTheme {
                var idOrden by remember { mutableStateOf<Long?>(null) }
                val id = idOrden
                if (id == null) {
                    OrdenesMecanicoScreen(ordenes, onBack = {}, onAgenda = {},
                        onChecklist = { _, _ -> }, onDeficiencias = { _, _ -> },
                        onDetalleMantenimiento = { idOrden = it })
                } else {
                    DetalleMantenimientoScreen(id, detalle) { idOrden = null }
                }
            }
        }
        compose.onNodeWithText("Ver detalle").performClick()
        scrollHasta("Ver mantenimiento")
        compose.onNodeWithText("Ver mantenimiento").performClick()
        scrollHasta("Desgaste de frenos")
        compose.onNodeWithText("Desgaste de frenos").assertIsDisplayed()
        scrollHasta("Total estimado: S/ 100.00")
        compose.onNodeWithText("Total estimado: S/ 100.00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver a la orden").performClick()
        compose.onNodeWithText("Detalle de la orden").assertIsDisplayed()
    }

    private fun mostrarMisCitas(
        enviarDecision: suspend (PresupuestoResponse, DecisionPresupuesto) -> Result<PresupuestoResponse> =
            { _, _ -> error("Esta prueba solo consulta presupuestos") },
        consultar: suspend () -> Result<PresupuestoResponse?>
    ) {
        val citas = MisCitasViewModel(
            cancelar = { error("Consultar presupuesto no debe cancelar una cita") },
            consultar = { Result.success(listOf(cita)) }
        )
        val vm = PresupuestoViewModel(consultar = { id ->
            assertEquals(12L, id)
            consultar()
        }, verFoto = ::leerFoto, enviarDecision = enviarDecision)
        compose.setContent {
            AutoSmartAppTheme {
                var idCita by remember { mutableStateOf<Long?>(null) }
                val id = idCita
                if (id == null) {
                    MisCitasScreen(viewModel = citas, onPresupuesto = { idCita = it })
                } else {
                    PresupuestoScreen(id, vm) {
                        vm.limpiar()
                        idCita = null
                    }
                }
            }
        }
    }

    private fun abrirPresupuesto() {
        compose.onNodeWithText("Consultar presupuesto").performScrollTo().performClick()
    }

    private fun scrollHasta(texto: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(texto))
    }

    private fun esperarFoto(descripcion: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription(descripcion).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(descripcion).performScrollTo().assertIsDisplayed()
    }

    private fun leerFoto(id: Long): Result<ByteArray> {
        assertEquals(7L, id)
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        return try {
            ByteArrayOutputStream().use { bytes ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes))
                Result.success(bytes.toByteArray())
            }
        } finally {
            bitmap.recycle()
        }
    }
}
