package com.tecsup.autosmart.ui.auth

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.res.painterResource
import com.tecsup.autosmart.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.data.local.TokenManager

private val VectorEyeOpen: ImageVector by lazy {
    ImageVector.Builder(
        name = "EyeOpen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathData {
            moveTo(12f, 4.5f)
            curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            close()
            moveTo(12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
            close()
            moveTo(12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            close()
        },
        fill = SolidColor(Color.White.copy(alpha = 0.85f))
    ).build()
}

private val VectorEyeOff: ImageVector by lazy {
    ImageVector.Builder(
        name = "EyeOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathData {
            moveTo(12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 12.65f, 16.87f, 13.26f, 16.64f, 13.82f)
            lineTo(19.56f, 16.74f)
            curveTo(21.27f, 15.11f, 22.47f, 13.64f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            curveTo(10.6f, 4.5f, 9.26f, 4.75f, 8.02f, 5.2f)
            lineTo(10.18f, 7.36f)
            curveTo(10.74f, 7.13f, 11.35f, 7f, 12f, 7f)
            close()
            moveTo(2f, 4.27f)
            lineTo(4.28f, 6.55f)
            lineTo(4.74f, 7.01f)
            curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(13.55f, 19.5f, 15.03f, 19.2f, 16.38f, 18.66f)
            lineTo(16.8f, 19.08f)
            lineTo(19.73f, 22.01f)
            lineTo(21.14f, 20.6f)
            lineTo(3.41f, 2.86f)
            lineTo(2f, 4.27f)
            close()
            moveTo(7.53f, 9.8f)
            lineTo(9.08f, 11.35f)
            curveTo(9.03f, 11.56f, 9f, 11.77f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(12.23f, 15f, 12.44f, 14.97f, 12.65f, 14.92f)
            lineTo(14.2f, 16.47f)
            curveTo(13.42f, 16.85f, 12.73f, 17f, 12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            curveTo(7f, 11.27f, 7.15f, 10.58f, 7.53f, 9.8f)
            close()
        },
        fill = SolidColor(Color.White.copy(alpha = 0.85f))
    ).build()
}

// Campos translúcidos de la variante 4B (ver AuthEstilo.kt)
@Composable
private fun loginFieldColors() = authFieldColors()

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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
            text = "Inicia sesión para gestionar tus vehículos",
            fontSize = 14.sp,
            color = AuthEstilo.TextoSuave,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }

    val botonGoogle: @Composable () -> Unit = {
        Button(
            onClick = onGoogleClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Continuar con Google", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }

    val formulario: @Composable () -> Unit = {
        // Campo Correo
        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                errorMessage = null
            },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = Color(0xFFE2E8F0)) },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = loginFieldColors()
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 14.dp))

        // Campo Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFE2E8F0)) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) VectorEyeOff else VectorEyeOpen,
                        contentDescription = "Mostrar/Ocultar contraseña",
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
            },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (correo.isNotBlank() && password.isNotBlank()) {
                        onLoginClick(correo, password)
                    }
                }
            ),
            colors = loginFieldColors()
        )

        AnimatedVisibility(
            visible = !errorMessage.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                color = AuthEstilo.Error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 18.dp))

        // Botón Ingresar
        Button(
            onClick = {
                focusManager.clearFocus()
                onLoginClick(correo, password)
            },
            enabled = !isLoading && correo.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.22f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AuthEstilo.AzulWeb,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                val ingresarActivo = correo.isNotBlank() && password.isNotBlank()
                Text(
                    text = "INGRESAR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ingresarActivo) AuthEstilo.AzulWeb else Color.White.copy(alpha = 0.85f)
                )
            }
        }

    }


    val colorAzul = Color.White // enlaces en blanco sobre el fondo azul
    val avisoOlvido = onForgotPasswordClick

    val olvidoPassword: @Composable () -> Unit = {
        TextButton(onClick = avisoOlvido, enabled = !isLoading) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                color = colorAzul,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    val enlaceRegistro: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿No tienes una cuenta?", color = Color.White, fontSize = 14.sp)
            TextButton(onClick = onNavigateToRegister, enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("Regístrate", color = colorAzul, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ---------- Pantalla ----------
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .fondoAuth() // Fondo azul de la variante 4B
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val altura = maxHeight
        Column(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = altura)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = if (altura < 640.dp) 12.dp else 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                encabezado()
                Spacer(Modifier.height(28.dp))
                formulario()
                Spacer(Modifier.height(12.dp))
                olvidoPassword()
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.35f))
                    Text("ó", color = AuthEstilo.TextoSuave, fontSize = 16.sp)
                    HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.35f))
                }
                Spacer(Modifier.height(24.dp))
                botonGoogle()
            }
            Spacer(Modifier.height(40.dp))
            enlaceRegistro()
        }
    }
}
