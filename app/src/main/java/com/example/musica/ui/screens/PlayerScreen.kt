package com.example.musica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musica.Cancion
import com.example.musica.Pantalla
import com.example.musica.ui.components.AlbumArt
import com.example.musica.ui.components.AndroidMediaSlider
import com.example.musica.ui.components.PlayerControls
import com.example.musica.ui.components.TopNavHeader
import com.example.musica.ui.theme.DarkBackground
import com.example.musica.ui.theme.SurfaceVariantDark

@Composable
fun PlayerScreen(
    cancion: Cancion,
    reproduciendo: Boolean,
    posicion: Long,
    duracion: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSiguiente: () -> Unit,
    onAnterior: () -> Unit,
    onVolverInicio: () -> Unit,
    onVerLetra: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkBackground,
                        SurfaceVariantDark,
                        DarkBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Unified Top Navigation Bar
        TopNavHeader(
            pantallaActual = Pantalla.REPRODUCTOR,
            tieneCancionActual = true,
            onIrInicio = onVolverInicio,
            onIrReproductor = { },
            onIrLetra = onVerLetra,
            onVolverInicio = onVolverInicio
        )

        Spacer(Modifier.weight(0.5f))

        // Large Hero Album Artwork
        AlbumArt(
            cancion = cancion,
            tamano = 290,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(Modifier.weight(0.5f))

        // Title and Artist
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = cancion.titulo,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = cancion.artista,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(20.dp))

        // Android 13/14 Wavy Animated Media Slider
        AndroidMediaSlider(
            posicion = posicion,
            duracion = duracion,
            reproduciendo = reproduciendo,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // Player Controls
        PlayerControls(
            reproduciendo = reproduciendo,
            onPlayPause = onPlayPause,
            onSiguiente = onSiguiente,
            onAnterior = onAnterior
        )

        Spacer(Modifier.height(16.dp))
    }
}
