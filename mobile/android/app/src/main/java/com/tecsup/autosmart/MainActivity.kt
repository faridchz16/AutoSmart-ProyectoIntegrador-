package com.tecsup.autosmart

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.tecsup.autosmart.data.model.GoogleLoginRequest
import com.tecsup.autosmart.data.model.LoginRequest
import com.tecsup.autosmart.data.model.RegisterRequest
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.ui.screens.AgregarVehiculoScreen
import com.tecsup.autosmart.ui.screens.EditarVehiculoScreen
import com.tecsup.autosmart.ui.viewmodel.EditarVehiculoViewModel
import com.tecsup.autosmart.ui.screens.LoginScreen
import com.tecsup.autosmart.ui.screens.FotosDeficienciaScreen
import com.tecsup.autosmart.ui.viewmodel.FotosDeficienciaViewModel
import com.tecsup.autosmart.ui.screens.DeficienciasScreen
import com.tecsup.autosmart.ui.viewmodel.DeficienciasViewModel
import com.tecsup.autosmart.ui.screens.ChecklistScreen
import com.tecsup.autosmart.ui.viewmodel.ChecklistViewModel
import com.tecsup.autosmart.ui.screens.OrdenesMecanicoScreen
import com.tecsup.autosmart.ui.viewmodel.OrdenesMecanicoViewModel
import com.tecsup.autosmart.ui.screens.AgendaMecanicoScreen
import com.tecsup.autosmart.ui.screens.CitaDetalleMecanicoScreen
import com.tecsup.autosmart.ui.viewmodel.AgendaMecanicoViewModel
import com.tecsup.autosmart.ui.viewmodel.CitaDetalleMecanicoViewModel
import com.tecsup.autosmart.ui.screens.MecanicoScreen
import com.tecsup.autosmart.ui.screens.RegisterScreen
import com.tecsup.autosmart.ui.screens.ReservaScreen
import com.tecsup.autosmart.ui.screens.PerfilScreen
import com.tecsup.autosmart.ui.screens.MisCitasScreen
import com.tecsup.autosmart.ui.screens.VehiculosScreen
import com.tecsup.autosmart.ui.screens.WelcomeScreen
import com.tecsup.autosmart.ui.theme.AutoSmartAppTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.ui.screens.ServicioDetalleScreen
import com.tecsup.autosmart.ui.screens.ServiciosScreen
import com.tecsup.autosmart.viewmodel.ServiciosViewModel
import com.tecsup.autosmart.ui.screens.OlvideContrasenaScreen
import com.tecsup.autosmart.ui.screens.NuevaContrasenaScreen
import com.tecsup.autosmart.viewmodel.RecuperacionViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoSmartAppTheme {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }
                val scope = rememberCoroutineScope()

                var currentScreen by rememberSaveable { mutableStateOf("loading") }
                var selectedServiceId by rememberSaveable { mutableStateOf(1L) }
                val editarVehiculoVm: EditarVehiculoViewModel = viewModel()

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
                LaunchedEffect(currentScreen) {
                    if (currentScreen !in setOf("ordenes_mecanico", "agenda_mecanico", "detalle_cita_mecanico", "checklist", "deficiencias", "fotos_deficiencia")) {
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
                    }

                    if (currentScreen == "loading") {
                        currentScreen = "welcome"
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
                                    onLoginClick = { correo, password ->
                                        scope.launch {
                                            try {
                                                val response = RetrofitClient.authApi.login(
                                                    LoginRequest(correo, password)
                                                )
                                                if (response.isSuccessful && response.body() != null) {
                                                    val loginResponseBody = response.body()!!
                                                    val userRole = loginResponseBody.rol

                                                    RetrofitClient.token = loginResponseBody.token
                                                    tokenManager.saveSession(
                                                        token = loginResponseBody.token,
                                                        role = userRole
                                                    )

                                                    Toast.makeText(this@MainActivity, "¡Login Exitoso!", Toast.LENGTH_LONG).show()
                                                    currentScreen = if (userRole == "MECANICO") "mecanico" else "vehiculos"
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Credenciales incorrectas (${response.code()})", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Log.e("LOGIN_ERROR", "Error en login", e)
                                                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
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
                                    onEditarVehiculo = { vehiculo ->
                                        editarVehiculoVm.iniciar(vehiculo)
                                        currentScreen = "editar_vehiculo"
                                    },
                                    onLogout = {
                                        scope.launch {
                                            tokenManager.clearSession()
                                            RetrofitClient.token = null
                                            currentScreen = "welcome"
                                        }
                                    },
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

                            "mis_citas" -> {
                                MisCitasScreen(
                                    onNavigateBack = { currentScreen = "vehiculos" }
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
                                    onLogout = {
                                        scope.launch {
                                            tokenManager.clearSession()
                                            RetrofitClient.token = null
                                            currentScreen = "welcome"
                                        }
                                    }
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
