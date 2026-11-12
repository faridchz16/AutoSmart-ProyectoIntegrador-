package com.tecsup.autosmart.ui.auth

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Campos translúcidos de la variante 4B (ver AuthEstilo.kt)
@Composable
private fun registerFieldColors() = authFieldColors()

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Misma regla que el backend: mínimo 8 caracteres, 1 mayúscula y 1 número
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*\\d).{8,}$")

    // Valida los campos y, si todo está bien, llama al registro
    val intentarRegistro: () -> Unit = {
        focusManager.clearFocus()
        val correoLimpio = correo.trim()
        when {
            nombre.isBlank() ->
                errorMessage = "Ingresa tu nombre completo"
            correoLimpio.isEmpty() ->
                errorMessage = "Ingresa tu correo electrónico"
            !Patterns.EMAIL_ADDRESS.matcher(correoLimpio).matches() ->
                errorMessage = "Ingresa un correo electrónico válido"
            !password.matches(passwordRegex) ->
                errorMessage = "La contraseña necesita mínimo 8 caracteres, una mayúscula y un número"
            else -> {
                errorMessage = null
                onRegisterClick(nombre.trim(), correoLimpio, password)
            }
        }
    }

    // ---------- Piezas reutilizables ----------

    val encabezado: @Composable () -> Unit = {
        Text(
            text = "AutoSmart",
            fontSize = if (isLandscape) 30.sp else 34.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = Color.White
        )
        Text(
            text = "Crear cuenta",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AuthEstilo.TextoSuave,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    val campos: @Composable () -> Unit = {
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it; errorMessage = null },
            label = { Text("Nombre completo") },
            singleLine = true,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it; errorMessage = null },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            // La ayuda solo se muestra en vertical para ahorrar altura en horizontal
            supportingText = if (isLandscape) null else {
                { Text("Mínimo 8 caracteres, 1 mayúscula y 1 número") }
            },
            singleLine = true,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { intentarRegistro() }),
            colors = registerFieldColors()
        )

        if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage ?: "",
                color = AuthEstilo.Error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 22.dp))

        Button(
            onClick = { intentarRegistro() },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 44.dp else 50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = AuthEstilo.AzulWeb
            )
        ) {
            Text(
                text = "Registrarse",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    val yaTienesCuenta: @Composable () -> Unit = {
        Text(
            text = "¿Ya tienes cuenta?",
            color = AuthEstilo.TextoSuave,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 44.dp else 50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.06f),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Iniciar sesión",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // ---------- Pantalla ----------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .fondoAuth() // Fondo azul de la variante 4B
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 32.dp else 28.dp,
                    vertical = if (isLandscape) 12.dp else 24.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLandscape) {
                // HORIZONTAL: izquierda título + "Iniciar sesión"; derecha campos + Registrarse
                Row(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        encabezado()
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            yaTienesCuenta()
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        campos()
                    }
                }
            } else {
                // VERTICAL: todo en una columna
                Column(
                    modifier = Modifier
                        .widthIn(max = 450.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    encabezado()
                    Spacer(modifier = Modifier.height(28.dp))
                    campos()
                    Spacer(modifier = Modifier.height(24.dp))
                    yaTienesCuenta()
                }
            }
        }
    }
}