package com.tecsup.autosmart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.viewmodel.RecuperacionViewModel

@Composable
fun NuevaContrasenaScreen(
    vm: RecuperacionViewModel,
    correo: String,
    onActualizada: () -> Unit,
    onBack: () -> Unit
) {
    val estado by vm.estado.collectAsState()
    val focusManager = LocalFocusManager.current

    var codigo by rememberSaveable { mutableStateOf("") }
    var nueva by rememberSaveable { mutableStateOf("") }
    var confirmar by rememberSaveable { mutableStateOf("") }
    var errorLocal by rememberSaveable { mutableStateOf<String?>(null) }

    // Misma regla que el registro y el backend: 8 caracteres, una mayúscula y un número
    val regexPassword = Regex("^(?=.*[A-Z])(?=.*\\d).{8,}$")

    LaunchedEffect(estado.contrasenaActualizada) {
        if (estado.contrasenaActualizada) {
            vm.consumirActualizada()
            onActualizada()
        }
    }

    val guardar = {
        focusManager.clearFocus()
        errorLocal = when {
            codigo.isBlank() -> "Ingresa el código que recibiste"
            !nueva.matches(regexPassword) ->
                "La contraseña necesita mínimo 8 caracteres, una mayúscula y un número"
            nueva != confirmar -> "Las contraseñas no coinciden"
            else -> null
        }
        if (errorLocal == null) vm.restablecer(correo, codigo.trim(), nueva)
    }

    val mensajeError = errorLocal ?: estado.error
    val limpiar = { errorLocal = null; vm.limpiarMensajes() }

    PantallaRecuperacion(
        titulo = "Nueva contraseña",
        subtitulo = "Ingresa el código que enviamos a $correo y elige tu nueva contraseña.",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it; limpiar() },
            label = { Text("Código de recuperación") },
            singleLine = true,
            isError = mensajeError != null,
            enabled = !estado.enviando,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            colors = recuperacionFieldColors()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = nueva,
            onValueChange = { nueva = it; limpiar() },
            label = { Text("Nueva contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            supportingText = { Text("Mínimo 8 caracteres, 1 mayúscula y 1 número", color = Color(0xFF94A3B8)) },
            singleLine = true,
            isError = mensajeError != null,
            enabled = !estado.enviando,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            colors = recuperacionFieldColors()
        )

        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = confirmar,
            onValueChange = { confirmar = it; limpiar() },
            label = { Text("Confirmar contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = mensajeError != null,
            enabled = !estado.enviando,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { guardar() }),
            colors = recuperacionFieldColors()
        )

        mensajeError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFF87171), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        estado.info?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFF4ADE80), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { guardar() },
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
                Text("Cambiar contraseña", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { vm.solicitarCodigo(correo, reenvio = true) },
            enabled = !estado.enviando,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
        ) {
            Text("¿No te llegó? Reenviar código", color = Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold)
        }
    }
}
