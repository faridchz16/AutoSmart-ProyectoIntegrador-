package com.tecsup.autosmart.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070C14),
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    )
                )
            )
    ) {
        if (isLandscape) {
            // HORIZONTAL: izquierda logo + frase, derecha botones
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        LogoYFrase(logoSize = 130.dp, fontSize = 18.sp, lineHeight = 24.sp)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        BotonPrincipalBienvenida(
                            texto = "Iniciar sesión",
                            onClick = onNavigateToLogin
                        )
                        BotonSecundarioBienvenida(
                            texto = "Registrarse",
                            onClick = onNavigateToRegister
                        )
                    }
                }
            }
        } else {
            // VERTICAL: logo y frase al centro, botones abajo
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp, horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                LogoYFrase(logoSize = 200.dp, fontSize = 20.sp, lineHeight = 28.sp)

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BotonPrincipalBienvenida(
                        texto = "Iniciar sesión",
                        onClick = onNavigateToLogin
                    )
                    BotonSecundarioBienvenida(
                        texto = "Registrarse",
                        onClick = onNavigateToRegister
                    )
                }
            }
        }
    }
}

/** Logo con la frase casi pegada debajo, en negrita. */
@Composable
private fun LogoYFrase(
    logoSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_autosmart_logo),
            contentDescription = "Logo AutoSmart",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(logoSize)
        )

        Text(
            text = "Gestiona tus vehículos fácil y rápido ",
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE2E8F0),
            textAlign = TextAlign.Center
        )
    }
}

/** Botón azul relleno con animación al presionar. */
@Composable
private fun BotonPrincipalBienvenida(texto: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "escalaPrincipal"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(escala),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) Color(0xFF3B82F6) else Color(0xFF1D4ED8),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = texto,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Botón con borde y animación al presionar. */
@Composable
private fun BotonSecundarioBienvenida(texto: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "escalaSecundario"
    )

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(escala),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isPressed) Color(0xFF1E293B) else Color.Transparent,
            contentColor = Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isPressed) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = texto,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}