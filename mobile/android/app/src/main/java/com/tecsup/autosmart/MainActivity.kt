package com.tecsup.autosmart

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.tecsup.autosmart.data.local.TokenManager
import com.tecsup.autosmart.data.model.LoginRequest
import com.tecsup.autosmart.data.model.RegisterRequest
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.ui.screens.LoginScreen
import com.tecsup.autosmart.ui.screens.MecanicoScreen
import com.tecsup.autosmart.ui.screens.RegisterScreen
import com.tecsup.autosmart.ui.screens.VehiculosScreen
import com.tecsup.autosmart.ui.screens.WelcomeScreen
import com.tecsup.autosmart.ui.theme.AutoSmartAppTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoSmartAppTheme {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }
                val scope = rememberCoroutineScope()

                // rememberSaveable: la pantalla actual sobrevive al girar el dispositivo
                var currentScreen by rememberSaveable { mutableStateOf("loading") }

                // Auto-login: lee el token y el rol guardados al abrir la app
                LaunchedEffect(Unit) {
                    val savedToken = tokenManager.getToken.firstOrNull()
                    val savedRole = tokenManager.getRole.firstOrNull()

                    // El token se restaura siempre (también tras girar o si el sistema cierra el proceso)
                    if (!savedToken.isNullOrEmpty()) {
                        RetrofitClient.token = savedToken
                    }

                    // La pantalla solo se decide la primera vez; al girar no se toca
                    if (currentScreen == "loading") {
                        currentScreen = if (!savedToken.isNullOrEmpty()) {
                            if (savedRole == "MECANICO") "mecanico" else "vehiculos"
                        } else {
                            "welcome"
                        }
                    }
                }

                // Botón "atrás": desde login o registro vuelve a la bienvenida
                BackHandler(enabled = currentScreen == "login" || currentScreen == "register") {
                    currentScreen = "welcome"
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
                                                    val userRole = loginResponseBody.rol ?: "CLIENTE"

                                                    // Guardar en memoria de Retrofit
                                                    RetrofitClient.token = loginResponseBody.token

                                                    // Guardar en DataStore (Token y Rol)
                                                    tokenManager.saveSession(
                                                        token = loginResponseBody.token,
                                                        role = userRole
                                                    )

                                                    Toast.makeText(this@MainActivity, "¡Login Exitoso!", Toast.LENGTH_LONG).show()

                                                    // Redirección dinámica por rol
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
                                    onNavigateToRegister = { currentScreen = "register" }
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
                                    }
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