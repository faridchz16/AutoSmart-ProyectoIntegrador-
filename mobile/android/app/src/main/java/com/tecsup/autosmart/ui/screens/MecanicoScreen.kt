package com.tecsup.autosmart.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R

@Composable
fun MecanicoScreen(
    onLogout: () -> Unit,
    onPerfilClick: (() -> Unit)? = null   // se conectará cuando exista la pantalla "Mi perfil"
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
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // ---------- Encabezado ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.xd),
                        contentDescription = "Logo AutoSmart",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AutoSmart",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Panel de mecánico",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Salir",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

            // ---------- Saludo ----------
            Text(
                text = "Hola, Mecánico",
                fontSize = if (isLandscape) 24.sp else 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Administra tu perfil profesional y mantén tus especialidades al día.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 28.dp))

            // ---------- Opciones ----------
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OpcionPanel(
                        emoji = "👤",
                        titulo = "Mi perfil profesional",
                        descripcion = "Tus especialidades y años de experiencia.",
                        etiqueta = if (onPerfilClick == null) "Próximamente" else null,
                        onClick = onPerfilClick,
                        modifier = Modifier.weight(1f)
                    )
                    OpcionPanel(
                        emoji = "🔧",
                        titulo = "Órdenes de trabajo",
                        descripcion = "Las órdenes asignadas a ti.",
                        etiqueta = "Próximamente",
                        onClick = null,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OpcionPanel(
                    emoji = "👤",
                    titulo = "Mi perfil profesional",
                    descripcion = "Tus especialidades y años de experiencia.",
                    etiqueta = if (onPerfilClick == null) "Próximamente" else null,
                    onClick = onPerfilClick,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OpcionPanel(
                    emoji = "🔧",
                    titulo = "Órdenes de trabajo",
                    descripcion = "Las órdenes asignadas a ti.",
                    etiqueta = "Próximamente",
                    onClick = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Tarjeta de opción del panel: icono, título, descripción y, si aplica, una etiqueta. */
@Composable
private fun OpcionPanel(
    emoji: String,
    titulo: String,
    descripcion: String,
    etiqueta: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .clip(forma)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), forma)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1D4ED8).copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = descripcion,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF94A3B8)
            )
        }

        if (etiqueta != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = etiqueta,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF60A5FA),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1D4ED8).copy(alpha = 0.20f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else if (onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "›",
                fontSize = 26.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}