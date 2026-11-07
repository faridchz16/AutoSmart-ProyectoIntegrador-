package com.tecsup.autosmart.ui.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    BoxWithConstraints(
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
        val horizontal = maxWidth > maxHeight && maxWidth >= 600.dp
        val botones: @Composable () -> Unit = {
            Column(
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BotonPrincipalBienvenida("Iniciar sesión", onNavigateToLogin)
                BotonSecundarioBienvenida("Registrarse", onNavigateToRegister)
            }
        }
        if (horizontal) {
            Row(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) { MarcaBienvenida(compacto = true) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { botones() }
            }
        } else {
            val altura = maxHeight
            val compacto = maxHeight < 640.dp || maxWidth < 360.dp
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = altura).padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
                        .padding(top = if (compacto) 12.dp else altura * 0.14f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { MarcaBienvenida(compacto) }
                Spacer(Modifier.height(40.dp))
                Box(Modifier.padding(bottom = 24.dp)) { botones() }
            }
        }
    }
}

@Composable
private fun MarcaBienvenida(compacto: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_autosmart_logo),
            contentDescription = "Logo AutoSmart",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(if (compacto) 110.dp else 120.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "AutoSmart",
            color = Color.White,
            fontSize = if (compacto) 30.sp else 34.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Gestiona tus vehículos fácil y rápido",
            fontSize = if (compacto) 18.sp else 20.sp,
            lineHeight = if (compacto) 25.sp else 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE2E8F0),
            textAlign = TextAlign.Center,
            maxLines = 2
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