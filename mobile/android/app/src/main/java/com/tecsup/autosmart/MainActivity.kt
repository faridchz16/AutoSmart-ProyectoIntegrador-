package com.tecsup.autosmart

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tecsup.autosmart.data.auth.GoogleAuthManager
import com.tecsup.autosmart.data.auth.GoogleResultado
import com.tecsup.autosmart.data.local.TokenManager
import com.tecsup.autosmart.data.notifications.NotificacionesFcm
import com.tecsup.autosmart.data.notifications.citaDeAviso
import com.tecsup.autosmart.model.GoogleLoginRequest
import com.tecsup.autosmart.model.LoginRequest
import com.tecsup.autosmart.model.RegisterRequest
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.ui.cliente.AgregarVehiculoScreen
import com.tecsup.autosmart.ui.cliente.EditarVehiculoScreen
import com.tecsup.autosmart.viewmodel.EditarVehiculoViewModel
import com.tecsup.autosmart.ui.cliente.HistorialVehiculoScreen
import com.tecsup.autosmart.viewmodel.HistorialVehiculoViewModel
import com.tecsup.autosmart.ui.auth.LoginScreen
import com.tecsup.autosmart.ui.mecanico.FotosDeficienciaScreen
import com.tecsup.autosmart.viewmodel.FotosDeficienciaViewModel
import com.tecsup.autosmart.ui.mecanico.DeficienciasScreen
import com.tecsup.autosmart.viewmodel.DeficienciasViewModel
import com.tecsup.autosmart.ui.mecanico.ChecklistScreen
import com.tecsup.autosmart.viewmodel.ChecklistViewModel
import com.tecsup.autosmart.ui.mecanico.OrdenesMecanicoScreen
import com.tecsup.autosmart.ui.cliente.DetalleMantenimientoScreen
import com.tecsup.autosmart.viewmodel.DetalleMantenimientoViewModel
import com.tecsup.autosmart.viewmodel.OrdenesMecanicoViewModel
import com.tecsup.autosmart.ui.mecanico.AgendaMecanicoScreen
import com.tecsup.autosmart.ui.mecanico.CitaDetalleMecanicoScreen
import com.tecsup.autosmart.viewmodel.AgendaMecanicoViewModel
import com.tecsup.autosmart.viewmodel.CitaDetalleMecanicoViewModel
import com.tecsup.autosmart.ui.mecanico.MecanicoScreen
import com.tecsup.autosmart.ui.auth.RegisterScreen
import com.tecsup.autosmart.ui.servicios.ReservaScreen
import com.tecsup.autosmart.ui.cliente.PerfilScreen
import com.tecsup.autosmart.ui.cliente.PresupuestoScreen
import com.tecsup.autosmart.ui.cliente.SeguimientoScreen
import com.tecsup.autosmart.viewmodel.SeguimientoViewModel
import com.tecsup.autosmart.viewmodel.PresupuestoViewModel
import com.tecsup.autosmart.ui.cliente.MisCitasScreen
import com.tecsup.autosmart.ui.cliente.VehiculosScreen
import com.tecsup.autosmart.ui.auth.WelcomeScreen
import com.tecsup.autosmart.ui.theme.AutoSmartAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.ui.servicios.ServicioDetalleScreen
import com.tecsup.autosmart.ui.servicios.ServiciosScreen
import com.tecsup.autosmart.viewmodel.ServiciosViewModel
import com.tecsup.autosmart.ui.auth.OlvideContrasenaScreen
import com.tecsup.autosmart.ui.auth.NuevaContrasenaScreen
import com.tecsup.autosmart.viewmodel.RecuperacionViewModel

class MainActivity : ComponentActivity() {
    private val citaNotificada = MutableStateFlow<Long?>(null)
    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibirAviso(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        citaNotificada.value?.let { outState.putLong("cita_notificada", it) }
        super.onSaveInstanceState(outState)
    }

    private fun recibirAviso(intent: Intent?) {
        val cita = citaDeAviso(intent?.getStringExtra("tipo"), intent?.getStringExtra("idCita"))
        if (cita != null) citaNotificada.value = cita
    }

    private fun prepararNotificacionesCliente() {
        if (!NotificacionesFcm.disponible(this)) return
        NotificacionesFcm.programarRegistro(applicationContext)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) return
        val preferencias = getSharedPreferences("notificaciones", MODE_PRIVATE)
        if (preferencias.getBoolean("permiso_solicitado", false)) return
        preferencias.edit().putBoolean("permiso_solicitado", true).apply()
        permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) recibirAviso(intent)
        else citaNotificada.value = savedInstanceState.getLong("cita_notificada").takeIf { it > 0 }
        NotificacionesFcm.crearCanal(this)
        enableEdgeToEdge()
        setContent {
            AutoSmartAppTheme {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }
                val scope = rememberCoroutineScope()

                var currentScreen by rememberSaveable { mutableStateOf("loading") }
                var sesionCliente by remember { mutableStateOf(false) }
                var cerrandoSesion by remember { mutableStateOf(false) }
                val avisoCita by citaNotificada.collectAsState()
                val cerrarSesion: () -> Unit = {
                    if (!cerrandoSesion) scope.launch {
                        cerrandoSesion = true
                        try {
                            NotificacionesFcm.cerrarSesion(applicationContext, tokenManager)
                            sesionCliente = false
                            citaNotificada.value = null
                            RetrofitClient.token = null
                            currentScreen = "welcome"
                        } finally {
                            cerrandoSesion = false
                        }
                    }
                }
                var selectedServiceId by rememberSaveable { mutableStateOf(1L) }
                val editarVehiculoVm: EditarVehiculoViewModel = viewModel()
                val historialVehiculoVm: HistorialVehiculoViewModel = viewModel()
                var historialVehiculoId by rememberSaveable { mutableStateOf(0L) }

                var presupuestoCitaId by rememberSaveable { mutableStateOf(0L) }
                val presupuestoVm: PresupuestoViewModel = viewModel()
                var seguimientoCitaId by rememberSaveable { mutableStateOf(0L) }
                val seguimientoVm: SeguimientoViewModel = viewModel()
                LaunchedEffect(currentScreen) {
                    if (currentScreen != "seguimiento") seguimientoVm.limpiar()
                }
                LaunchedEffect(currentScreen) {
                    if (currentScreen != "presupuesto") presupuestoVm.limpiar()
                }
                var selectedCitaMecanicoId by rememberSaveable { mutableStateOf(0L) }
                var fotoDeficienciaId by rememberSaveable { mutableStateOf(0L) }
                val fotosVm: FotosDeficienciaViewModel = viewModel()
                LaunchedEffect(currentScreen) {
                    if (currentScreen != "fotos_deficiencia") fotosVm.limpiar()
                }
                var deficienciasOrdenId by rememberSaveable { mutableStateOf(0L) }
                var deficienciasEditable by rememberSaveable { mutableStateOf(false) }
                val deficienciasVm: DeficienciasViewModel = viewModel()
                LaunchedEffect(currentScreen) {
                    if (currentScreen != "deficiencias" && currentScreen != "fotos_deficiencia") deficienciasVm.limpiar()
                }
                var checklistOrdenId by rememberSaveable { mutableStateOf(0L) }
                var checklistEditable by rememberSaveable { mutableStateOf(false) }
                val checklistVm: ChecklistViewModel = viewModel()
                LaunchedEffect(currentScreen) {
                    if (currentScreen != "checklist") checklistVm.limpiar()
                }
                val ordenesMecanicoVm: OrdenesMecanicoViewModel = viewModel()
                var mantenimientoOrdenId by rememberSaveable { mutableStateOf(0L) }
                var mantenimientoDesdeHistorial by rememberSaveable { mutableStateOf(false) }
                val mantenimientoVm: DetalleMantenimientoViewModel = viewModel()
                LaunchedEffect(currentScreen) {
                    if (currentScreen != "detalle_mantenimiento") mantenimientoVm.limpiar()
                }
                LaunchedEffect(currentScreen) {
                    if (currentScreen !in setOf("ordenes_mecanico", "agenda_mecanico", "detalle_cita_mecanico", "checklist", "deficiencias", "fotos_deficiencia", "detalle_mantenimiento")) {
                        ordenesMecanicoVm.limpiar()
                    }
                }
                val agendaMecanicoVm: AgendaMecanicoViewModel = viewModel()
                val detalleMecanicoVm: CitaDetalleMecanicoViewModel = viewModel()
                LaunchedEffect(currentScreen) {
                    if (currentScreen != "agenda_mecanico" && currentScreen != "detalle_cita_mecanico") {
                        agendaMecanicoVm.limpiar()
                        detalleMecanicoVm.limpiar()
                    }
                }

                val serviciosVm: ServiciosViewModel = viewModel()
                val recuperacionVm: RecuperacionViewModel = viewModel()
                var correoRecuperacion by rememberSaveable { mutableStateOf("") }


                LaunchedEffect(Unit) {
                    val savedToken = tokenManager.getToken.firstOrNull()

                    if (!savedToken.isNullOrEmpty()) {
                        RetrofitClient.token = savedToken
                        sesionCliente = tokenManager.tokenCliente() != null
                        if (sesionCliente) prepararNotificacionesCliente()
                    }

                    if (currentScreen == "loading") {
                        currentScreen = "welcome"
                    }
                }


                LaunchedEffect(avisoCita, sesionCliente, cerrandoSesion) {
                    val id = avisoCita ?: return@LaunchedEffect
                    if (sesionCliente && !cerrandoSesion) {
                        seguimientoCitaId = id
                        currentScreen = "seguimiento"
                        citaNotificada.value = null
                    }
                }

                BackHandler(enabled = currentScreen == "login" || currentScreen == "register") {
                    currentScreen = "welcome"
                }

                BackHandler(enabled = currentScreen == "agregar_vehiculo") {
                    currentScreen = "vehiculos"
                }

                BackHandler(enabled = currentScreen == "perfil") {
                    currentScreen = "vehiculos"
                }

                BackHandler(enabled = currentScreen == "mis_citas") {
                    currentScreen = "vehiculos"
                }

                BackHandler(enabled = currentScreen == "servicios") { currentScreen = "vehiculos" }
                BackHandler(enabled = currentScreen == "servicio_detalle") { currentScreen = "servicios" }
                BackHandler(enabled = currentScreen == "reserva") { currentScreen = "servicio_detalle" }
                BackHandler(enabled = currentScreen == "olvide") { currentScreen = "login" }
                BackHandler(enabled = currentScreen == "nueva_password") { currentScreen = "olvide" }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "loading" -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            "welcome" -> {
                                WelcomeScreen(
                                    onNavigateToLogin = { currentScreen = "login" },
                                    onNavigateToRegister = { currentScreen = "register" }
                                )
                            }
                            "login" -> {
                                LoginScreen(
                                    // Devuelve null si entra, o el mensaje que se muestra en rojo debajo de los campos
                                    onLoginClick = { correo, password ->
                                        try {
                                            val response = RetrofitClient.authApi.login(
                                                LoginRequest(correo, password)
                                            )
                                            val loginResponseBody = response.body()
                                            if (response.isSuccessful && loginResponseBody != null) {
                                                val userRole = loginResponseBody.rol

                                                RetrofitClient.token = loginResponseBody.token
                                                tokenManager.saveSession(
                                                    token = loginResponseBody.token,
                                                    role = userRole
                                                )

                                                Toast.makeText(this@MainActivity, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                                                currentScreen = if (userRole == "MECANICO") "mecanico" else "vehiculos"
                                                sesionCliente = userRole.equals("CLIENTE", ignoreCase = true)
                                                if (sesionCliente) prepararNotificacionesCliente() else citaNotificada.value = null
                                                null
                                            } else {
                                                when (response.code()) {
                                                    401 -> "Correo o contraseña incorrectos"
                                                    403 -> com.tecsup.autosmart.data.remote.mensajeDeError(response.errorBody()?.string(), 403)
                                                    400 -> "Revisa el correo y la contraseña"
                                                    in 500..599 -> "El servidor tuvo un problema. Intenta de nuevo en unos minutos"
                                                    else -> com.tecsup.autosmart.data.remote.mensajeDeError(response.errorBody()?.string(), response.code())
                                                }
                                            }
                                        } catch (e: Exception) {
                                            if (e is kotlinx.coroutines.CancellationException) throw e
                                            Log.e("LOGIN_ERROR", "Error en login", e)
                                            "No se pudo conectar con el servidor. Revisa tu conexión a internet"
                                        }
                                    },
                                    onNavigateToRegister = { currentScreen = "register" },
                                    onGoogleClick = {
                                        scope.launch {
                                            when (val resultado = GoogleAuthManager.obtenerIdToken(this@MainActivity)) {
                                                is GoogleResultado.Token -> {
                                                    try {
                                                        val response = RetrofitClient.authApi.loginConGoogle(
                                                            GoogleLoginRequest(resultado.idToken)
                                                        )
                                                        if (response.isSuccessful && response.body() != null) {
                                                            val authResponse = response.body()!!
                                                            val userRole = authResponse.rol

                                                            RetrofitClient.token = authResponse.token
                                                            tokenManager.saveSession(
                                                                token = authResponse.token,
                                                                role = userRole
                                                            )

                                                            Toast.makeText(this@MainActivity, "¡Inicio de sesión con Google exitoso!", Toast.LENGTH_LONG).show()
                                                            currentScreen = if (userRole == "MECANICO") "mecanico" else "vehiculos"
                                                            sesionCliente = userRole.equals("CLIENTE", ignoreCase = true)
                                                            if (sesionCliente) prepararNotificacionesCliente() else citaNotificada.value = null
                                                        } else {
                                                            Toast.makeText(this@MainActivity, "Error al autenticar con Google (${response.code()})", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("GOOGLE_AUTH_ERROR", "Error al conectar con el servidor", e)
                                                        Toast.makeText(this@MainActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                is GoogleResultado.Error -> {
                                                    Toast.makeText(this@MainActivity, resultado.mensaje, Toast.LENGTH_SHORT).show()
                                                }
                                                GoogleResultado.Cancelado -> {}
                                            }
                                        }
                                    },
                                            onForgotPasswordClick = {
                                        recuperacionVm.reiniciar()
                                        currentScreen = "olvide"
                                    }
                                )
                            }
                            "register" -> {
                                RegisterScreen(
                                    onRegisterClick = { nombre, correo, password ->
                                        scope.launch {
                                            try {
                                                val response = RetrofitClient.authApi.register(
                                                    RegisterRequest(nombre, correo, password)
                                                )
                                                if (response.isSuccessful) {
                                                    Toast.makeText(this@MainActivity, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                                                    currentScreen = "login"
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Error al registrar (${response.code()})", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Log.e("REGISTER_ERROR", "Excepción al registrar", e)
                                                Toast.makeText(this@MainActivity, "Excepción: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onNavigateToLogin = { currentScreen = "login" }
                                )
                            }
                            "vehiculos" -> {
                                VehiculosScreen(
                                    onHistorialVehiculo = { id ->
                                        historialVehiculoVm.limpiar()
                                        historialVehiculoId = id
                                        currentScreen = "historial_vehiculo"
                                    },
                                    onEditarVehiculo = { vehiculo ->
                                        editarVehiculoVm.iniciar(vehiculo)
                                        currentScreen = "editar_vehiculo"
                                    },
                                    onLogout = cerrarSesion,
                                    onNavigateToPerfil = {
                                        currentScreen = "perfil"
                                    },
                                    onNavigateToMisCitas = {
                                        currentScreen = "mis_citas"
                                    },
                                    onNavigateToReserva = {
                                        currentScreen = "servicios"
                                    },
                                    onNavigateToAgregarVehiculo = {
                                        currentScreen = "agregar_vehiculo"
                                    }
                                )
                            }

                            "historial_vehiculo" -> {
                                HistorialVehiculoScreen(
                                    idVehiculoInicial = historialVehiculoId,
                                    viewModel = historialVehiculoVm,
                                    onBack = {
                                        historialVehiculoVm.limpiar()
                                        currentScreen = "vehiculos"
                                    },
                                    onVerDetalle = { idOrden ->
                                        mantenimientoOrdenId = idOrden
                                        mantenimientoDesdeHistorial = true
                                        currentScreen = "detalle_mantenimiento"
                                    }
                                )
                            }

                            "agregar_vehiculo" -> {
                                AgregarVehiculoScreen(
                                    onBack = {
                                        currentScreen = "vehiculos"
                                    },

                                    onVehiculoRegistrado = {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "¡Vehículo registrado correctamente!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        currentScreen = "vehiculos"
                                    }
                                )
                            }


                            "editar_vehiculo" -> {
                                EditarVehiculoScreen(
                                    viewModel = editarVehiculoVm,
                                    onBack = { currentScreen = "vehiculos" },
                                    onActualizado = {
                                        Toast.makeText(this@MainActivity,
                                            "Vehículo actualizado correctamente", Toast.LENGTH_SHORT).show()
                                        currentScreen = "vehiculos"
                                    }
                                )
                            }

                            "perfil" -> {
                                PerfilScreen(
                                    onNavigateBack = { currentScreen = "vehiculos" }
                                )
                            }

                            "presupuesto" -> {
                                PresupuestoScreen(presupuestoCitaId, presupuestoVm,
                                    onBack = { currentScreen = "mis_citas" })
                            }
                            "seguimiento" -> {
                                SeguimientoScreen(seguimientoCitaId, seguimientoVm,
                                    onBack = { currentScreen = "mis_citas" })
                            }
                            "mis_citas" -> {
                                MisCitasScreen(
                                    onNavigateBack = { currentScreen = "vehiculos" },
                                    onPresupuesto = { id -> presupuestoCitaId = id; currentScreen = "presupuesto" },
                                    onSeguimiento = { id -> seguimientoCitaId = id; currentScreen = "seguimiento" }
                                )
                            }

                            "servicios" -> {
                                ServiciosScreen(
                                    vm = serviciosVm,
                                    onVerDetalle = { id ->
                                        selectedServiceId = id
                                        currentScreen = "servicio_detalle"
                                    },
                                    onBack = { currentScreen = "vehiculos" }
                                )
                            }
                            "servicio_detalle" -> {
                                ServicioDetalleScreen(
                                    idServicio = selectedServiceId,
                                    vm = serviciosVm,
                                    onReservar = { currentScreen = "reserva" },
                                    onBack = { currentScreen = "servicios" }
                                )
                            }
                            "reserva" -> {
                                ReservaScreen(
                                    idServicio = selectedServiceId,
                                    onNavigateBack = { currentScreen = "servicio_detalle" }
                                )
                            }
                            "fotos_deficiencia" -> {
                                FotosDeficienciaScreen(deficienciasOrdenId, fotoDeficienciaId, deficienciasEditable, fotosVm,
                                    onBack = { currentScreen = "deficiencias"; deficienciasVm.cargar() })
                            }
                            "deficiencias" -> {
                                DeficienciasScreen(deficienciasOrdenId, deficienciasEditable, deficienciasVm,
                                    onBack = { currentScreen = "ordenes_mecanico" },
                                    onFotos = { id -> fotoDeficienciaId = id; currentScreen = "fotos_deficiencia" })
                            }
                            "checklist" -> {
                                ChecklistScreen(checklistOrdenId, checklistEditable, checklistVm,
                                    onBack = { currentScreen = "ordenes_mecanico" })
                            }
                            "detalle_mantenimiento" -> {
                                DetalleMantenimientoScreen(
                                    idOrden = mantenimientoOrdenId,
                                    viewModel = mantenimientoVm,
                                    descripcionVolver = if (mantenimientoDesdeHistorial) "Volver al historial" else "Volver a la orden",
                                    onBack = {
                                        currentScreen = if (mantenimientoDesdeHistorial) "historial_vehiculo" else "ordenes_mecanico"
                                    }
                                )
                            }
                            "ordenes_mecanico" -> {
                                OrdenesMecanicoScreen(
                                    viewModel = ordenesMecanicoVm,
                                    onBack = { currentScreen = "mecanico" },
                                    onAgenda = { currentScreen = "agenda_mecanico" },
                                    onChecklist = { id, editable ->
                                        checklistOrdenId = id
                                        checklistEditable = editable
                                        currentScreen = "checklist"
                                    },
                                    onDeficiencias = { id, editable ->
                                        deficienciasOrdenId = id
                                        deficienciasEditable = editable
                                        currentScreen = "deficiencias"
                                    },
                                    onDetalleMantenimiento = { idOrden ->
                                        mantenimientoOrdenId = idOrden
                                        mantenimientoDesdeHistorial = false
                                        currentScreen = "detalle_mantenimiento"
                                    }
                                )
                            }
                            "agenda_mecanico" -> {
                                AgendaMecanicoScreen(
                                    viewModel = agendaMecanicoVm,
                                    onBack = { currentScreen = "ordenes_mecanico" },
                                    onDetalle = { id ->
                                        selectedCitaMecanicoId = id
                                        detalleMecanicoVm.cargar(id, forzar = true)
                                        currentScreen = "detalle_cita_mecanico"
                                    }
                                )
                            }
                            "detalle_cita_mecanico" -> {
                                CitaDetalleMecanicoScreen(
                                    idCita = selectedCitaMecanicoId,
                                    viewModel = detalleMecanicoVm,
                                    onBack = { currentScreen = "agenda_mecanico" }
                                )
                            }
                            "mecanico" -> {
                                MecanicoScreen(
                                    onOrdenesClick = { currentScreen = "ordenes_mecanico" },
                                    onLogout = cerrarSesion
                                )
                            }

                            "olvide" -> {
                                OlvideContrasenaScreen(
                                    vm = recuperacionVm,
                                    onCodigoEnviado = { correo ->
                                        correoRecuperacion = correo
                                        currentScreen = "nueva_password"
                                    },
                                    onBack = { currentScreen = "login" }
                                )
                            }
                            "nueva_password" -> {
                                NuevaContrasenaScreen(
                                    vm = recuperacionVm,
                                    correo = correoRecuperacion,
                                    onActualizada = {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Contraseña actualizada. Inicia sesión con tu nueva contraseña",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        currentScreen = "login"
                                    },
                                    onBack = { currentScreen = "olvide" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
