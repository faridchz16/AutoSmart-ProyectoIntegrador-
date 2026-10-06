package com.tecsup.autosmart

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.tecsup.autosmart.data.model.LoginRequest
import com.tecsup.autosmart.data.model.RegisterRequest
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.ui.screens.LoginScreen
import com.tecsup.autosmart.ui.screens.RegisterScreen
import com.tecsup.autosmart.ui.screens.VehiculosScreen
import com.tecsup.autosmart.ui.theme.AutoSmartAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoSmartAppTheme {
                var currentScreen by remember { mutableStateOf("login") }
                val scope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
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

                                                    // 🔑 AQUÍ SE GUARDA EL TOKEN JWT EN RETROFITCLIENT
                                                    RetrofitClient.token = loginResponseBody.token

                                                    Toast.makeText(this@MainActivity, "¡Login Exitoso!", Toast.LENGTH_LONG).show()
                                                    currentScreen = "vehiculos"
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
                                VehiculosScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}