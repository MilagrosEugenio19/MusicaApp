package com.example.musica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musica.Cancion
import com.example.musica.LyricsFetcher
import com.example.musica.Pantalla
import com.example.musica.formato
import com.example.musica.ui.components.AlbumArt
import com.example.musica.ui.components.TopNavHeader
import com.example.musica.ui.theme.AccentPink
import com.example.musica.ui.theme.AccentPurple
import com.example.musica.ui.theme.DarkBackground
import com.example.musica.ui.theme.SurfaceCard

private val FondoLetra = Color(0xFF0F1715)

@Composable
fun LyricsScreen(
    cancion: Cancion,
    reproduciendo: Boolean,
    posicion: Long,
    duracion: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onVolverInicio: () -> Unit,
    onVerCancion: () -> Unit
) {
    val context = LocalContext.current
    var letraActual by remember(cancion.id) { mutableStateOf("Buscando letra automáticamente...") }
    var cargandoLetra by remember(cancion.id) { mutableStateOf(true) }

    LaunchedEffect(cancion.id) {
        cargandoLetra = true
        letraActual = LyricsFetcher.obtenerLetra(context, cancion)
        cargandoLetra = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        FondoLetra,
                        DarkBackground,
                        FondoLetra
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Unified Top Navigation Bar
        TopNavHeader(
            pantallaActual = Pantalla.LETRA,
            tieneCancionActual = true,
            onIrInicio = onVolverInicio,
            onIrReproductor = onVerCancion,
            onIrLetra = { },
            onVolverInicio = onVolverInicio
        )

        Spacer(Modifier.height(12.dp))

        // Track Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(cancion = cancion, tamano = 44)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = cancion.titulo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = cancion.artista,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Scrollable Lyrics Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (cargandoLetra) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentPurple)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Buscando letra...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = letraActual,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 20.sp,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bottom Compact Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onPlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentPurple),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    imageVector = if (reproduciendo) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (reproduciendo) "Pausar" else "Reproducir",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = formato(posicion),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Slider(
                value = if (duracion > 0) (posicion.toFloat() / duracion.toFloat()).coerceIn(0f, 1f) else 0f,
                onValueChange = { onSeek((it * duracion).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = AccentPink,
                    activeTrackColor = AccentPurple,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )

            Text(
                text = formato(duracion),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
