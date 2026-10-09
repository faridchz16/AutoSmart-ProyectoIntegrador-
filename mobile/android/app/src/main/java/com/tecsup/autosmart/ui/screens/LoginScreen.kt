package com.tecsup.autosmart.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R
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

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Black.copy(alpha = 0.35f),
    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF3B82F6),
    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
    focusedLabelColor = Color(0xFF60A5FA),
    unfocusedLabelColor = Color.White.copy(alpha = 0.75f)
)

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit
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
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Inicia sesión para gestionar tus vehículos",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 4.dp)
        )
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
                color = Color(0xFFF87171),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 22.dp))

        // Botón Ingresar
        Button(
            onClick = {
                focusManager.clearFocus()
                onLoginClick(correo, password)
            },
            enabled = !isLoading && correo.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 42.dp else 50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color(0xFF2563EB).copy(alpha = 0.45f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Ingresar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    val enlaceRegistro: @Composable () -> Unit = {
        TextButton(
            onClick = onNavigateToRegister,
            enabled = !isLoading
        ) {
            Text(
                text = "¿No tienes cuenta? Regístrate aquí",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }

    // ---------- Pantalla ----------

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.fondo_login),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay cinemático
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = if (isLandscape) 760.dp else 450.dp)
                    .fillMaxWidth(0.88f)
                    .padding(vertical = if (isLandscape) 4.dp else 16.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (isLandscape) {
                    // HORIZONTAL: izquierda título; derecha campos + botón + registro
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            encabezado()
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            formulario()
                            enlaceRegistro()
                        }
                    }
                } else {
                    // VERTICAL: todo en una columna
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        encabezado()
                        Spacer(modifier = Modifier.height(24.dp))
                        formulario()
                        Spacer(modifier = Modifier.height(16.dp))
                        enlaceRegistro()
                    }
                }
            }
        }
    }
}