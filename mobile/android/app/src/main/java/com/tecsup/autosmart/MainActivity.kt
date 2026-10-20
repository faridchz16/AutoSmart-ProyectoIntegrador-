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
import com.tecsup.autosmart.ui.screens.LoginScreen
import com.tecsup.autosmart.ui.screens.MecanicoScreen
import com.tecsup.autosmart.ui.screens.RegisterScreen
import com.tecsup.autosmart.ui.screens.ReservaScreen
import com.tecsup.autosmart.ui.screens.VehiculosScreen
import com.tecsup.autosmart.ui.screens.WelcomeScreen
import com.tecsup.autosmart.ui.theme.AutoSmartAppTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

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

                // Lógica de inicio (Welcome por defecto)
                LaunchedEffect(Unit) {
                    val savedToken = tokenManager.getToken.firstOrNull()

                    if (!savedToken.isNullOrEmpty()) {
                        RetrofitClient.token = savedToken
                    }

                    if (currentScreen == "loading") {
                        currentScreen = "welcome"
                    }
                }

                // Manejo del botón 'Atrás'
                BackHandler(enabled = currentScreen == "login" || currentScreen == "register") {
                    currentScreen = "welcome"
                }

                BackHandler(enabled = currentScreen == "reserva") {
                    currentScreen = "vehiculos"
                }

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
                                    onLogout = {
                                        scope.launch {
                                            tokenManager.clearSession()
                                            RetrofitClient.token = null
                                            currentScreen = "welcome"
                                        }
                                    },
                                    onNavigateToReserva = {
                                        currentScreen = "reserva"
                                    }
                                )
                            }
                            "reserva" -> {
                                ReservaScreen(
                                    idServicio = selectedServiceId,
                                    onNavigateBack = { currentScreen = "vehiculos" }
                                )
                            }
                            "mecanico" -> {
                                MecanicoScreen(
                                    onLogout = {
                                        scope.launch {
                                            tokenManager.clearSession()
                                            RetrofitClient.token = null
                                            currentScreen = "welcome"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}