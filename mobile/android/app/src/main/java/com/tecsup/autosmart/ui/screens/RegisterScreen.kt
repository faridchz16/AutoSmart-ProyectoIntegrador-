package com.tecsup.autosmart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Expresión regular idéntica a la del backend Java
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*\\d).{8,}$")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "AutoSmart", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Crear Cuenta", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                errorMessage = null
            },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                errorMessage = null
            },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            supportingText = {
                Text("Mínimo 8 caracteres, 1 mayúscula y 1 número")
            },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Mostrar mensaje de error si no cumple las reglas
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                when {
                    nombre.isBlank() -> {
                        errorMessage = "El nombre es obligatorio"
                    }
                    correo.isBlank() -> {
                        errorMessage = "El correo es obligatorio"
                    }
                    !password.matches(passwordRegex) -> {
                        errorMessage = "La contraseña requiere mínimo 8 caracteres, una mayúscula y un número"
                    }
                    else -> {
                        errorMessage = null
                        onRegisterClick(nombre.trim(), correo.trim(), password)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
