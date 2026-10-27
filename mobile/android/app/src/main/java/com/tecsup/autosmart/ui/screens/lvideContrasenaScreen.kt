package com.tecsup.autosmart.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.viewmodel.RecuperacionViewModel

@Composable
internal fun recuperacionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF0F172A),
    unfocusedContainerColor = Color(0xFF0B1220),
    errorContainerColor = Color(0xFF0B1220),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    errorTextColor = Color.White,
    focusedBorderColor = Color(0xFF3B82F6),
    unfocusedBorderColor = Color(0xFF334155),
    errorBorderColor = Color(0xFFF87171),
    focusedLabelColor = Color(0xFF60A5FA),
    unfocusedLabelColor = Color(0xFF94A3B8),
    errorLabelColor = Color(0xFFF87171)
)

@Composable
internal fun PantallaRecuperacion(
    titulo: String,
    subtitulo: String,
    onBack: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070C14), Color(0xFF0F172A), Color(0xFF030712)))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) { Text("Atrás", fontSize = 13.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(24.dp))
            Text(titulo, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(subtitulo, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(24.dp))
            contenido()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun OlvideContrasenaScreen(
    vm: RecuperacionViewModel,
    onCodigoEnviado: (String) -> Unit,
    onBack: () -> Unit
) {
    val estado by vm.estado.collectAsState()
    val focusManager = LocalFocusManager.current

    var correo by rememberSaveable { mutableStateOf("") }
    var errorLocal by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(estado.codigoEnviado) {
        if (estado.codigoEnviado) {
            vm.consumirCodigoEnviado()
            onCodigoEnviado(correo.trim())
        }
    }

    val enviar = {
        focusManager.clearFocus()
        val c = correo.trim()
        errorLocal = when {
            c.isEmpty() -> "Ingresa tu correo electrónico"
            !Patterns.EMAIL_ADDRESS.matcher(c).matches() -> "Ingresa un correo electrónico válido"
            else -> null
        }
        if (errorLocal == null) vm.solicitarCodigo(c)
    }

    val mensajeError = errorLocal ?: estado.error

    PantallaRecuperacion(
        titulo = "Olvidé mi contraseña",
        subtitulo = "Escribe el correo de tu cuenta y te enviaremos un código para crear una contraseña nueva. El código vence en 15 minutos.",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it; errorLocal = null; vm.limpiarMensajes() },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = mensajeError != null,
            enabled = !estado.enviando,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { enviar() }),
            colors = recuperacionFieldColors()
        )

        mensajeError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFF87171), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { enviar() },
            enabled = !estado.enviando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1D4ED8),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF1D4ED8).copy(alpha = 0.45f)
            )
        ) {
            if (estado.enviando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Enviar código", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
