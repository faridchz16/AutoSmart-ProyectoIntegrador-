package com.tecsup.autosmart.ui.cliente

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tecsup.autosmart.R

/** Recuerda, por usuario, si ya vio el anuncio de ARI (se muestra solo la primera vez). */
object AnuncioAri {
    private fun prefs(context: Context) = context.getSharedPreferences("anuncios", Context.MODE_PRIVATE)
    fun visto(context: Context, idUsuario: Int): Boolean = prefs(context).getBoolean("ari_$idUsuario", false)
    fun marcarVisto(context: Context, idUsuario: Int) {
        prefs(context).edit().putBoolean("ari_$idUsuario", true).apply()
    }
}

private val AzulClaro = Color(0xFF93C5FD)

/** Ventana de bienvenida que presenta a ARI, el asistente con IA. Se cierra con la X o con "Probar ahora". */
@Composable
fun AnuncioAriDialog(onProbar: () -> Unit, onCerrar: () -> Unit) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val forma = RoundedCornerShape(28.dp)
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .clip(forma)
                .background(Brush.linearGradient(listOf(Color(0xFF08184A), Color(0xFF0A3AB0), Color(0xFF1150E8))))
                .border(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.8f), forma)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Etiqueta "Nuevo" y botón cerrar
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7))))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nuevo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onCerrar,
                        modifier = Modifier
                            .size(38.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                // Figura de ARI con un halo detrás
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFF60A5FA).copy(alpha = 0.45f), Color.Transparent)))
                    )
                    Image(
                        painterResource(R.drawable.img_ari_figura),
                        contentDescription = "ARI, asistente de AutoSmart",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(190.dp).height(124.dp)
                    )
                }

                Text(
                    buildAnnotatedString {
                        append("¡Conoce a ARI,\nla IA de ")
                        withStyle(SpanStyle(color = AzulClaro, fontStyle = FontStyle.Italic)) { append("AutoSmart!") }
                    },
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tu asistente automotriz inteligente que te ayuda a:",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(14.dp))
                Beneficio(Icons.Outlined.Build, "Diagnosticar fallas con una foto")
                Beneficio(Icons.Outlined.EventAvailable, "Recomendar servicios y agendar")
                Beneficio(Icons.Outlined.VerifiedUser, "Dar tips de mantenimiento")
                Beneficio(Icons.Outlined.ChatBubbleOutline, "Resolver tus dudas al instante")
                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = onProbar,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1150E8))
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Probar ahora", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun Beneficio(icono: ImageVector, texto: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF1D4ED8)),
                contentAlignment = Alignment.Center
            ) { Icon(icono, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(texto, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
