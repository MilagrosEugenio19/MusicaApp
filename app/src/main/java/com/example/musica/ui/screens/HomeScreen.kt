package com.example.musica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musica.Cancion
import com.example.musica.Pantalla
import com.example.musica.ui.components.AlbumArt
import com.example.musica.ui.components.MiniPlayer
import com.example.musica.ui.components.SongRow
import com.example.musica.ui.components.TopNavHeader
import com.example.musica.ui.theme.AccentPurple
import com.example.musica.ui.theme.DarkBackground
import com.example.musica.ui.theme.SurfaceCard

@Composable
fun HomeScreen(
    canciones: List<Cancion>,
    permisoConcedido: Boolean,
    cancionActual: Cancion?,
    reproduciendo: Boolean,
    posicion: Long,
    duracion: Long,
    onPedirPermiso: () -> Unit,
    onCancionClick: (Int) -> Unit,
    onIrReproductor: () -> Unit,
    onIrLetra: () -> Unit,
    onMiniReproductorClick: () -> Unit,
    onPlayPause: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }

    val cancionesFiltradas = if (busqueda.isBlank()) {
        canciones
    } else {
        canciones.filter {
            it.titulo.contains(busqueda, ignoreCase = true) ||
                    it.artista.contains(busqueda, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Top Header Bar
            TopNavHeader(
                pantallaActual = Pantalla.INICIO,
                tieneCancionActual = cancionActual != null,
                onIrInicio = { },
                onIrReproductor = onIrReproductor,
                onIrLetra = onIrLetra,
                onVolverInicio = { }
            )

            // Header Info & Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tu Biblioteca",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (canciones.isNotEmpty()) {
                        Text(
                            text = "${canciones.size} canciones encontradas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Modern Search Input
            if (permisoConcedido && canciones.isNotEmpty()) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar canción o artista...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Buscar",
                            tint = AccentPurple
                        )
                    },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { busqueda = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Limpiar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Body Content
            when {
                !permisoConcedido -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Text(
                                    text = "Permiso requerido",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Necesitamos acceso a tu música local para poder reproducirla.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = onPedirPermiso,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Conceder Permiso", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                canciones.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay canciones disponibles en tu dispositivo.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = if (cancionActual != null) 90.dp else 16.dp)
                    ) {
                        // Quick Play Featured Row (top 5 songs as cards)
                        if (busqueda.isBlank() && canciones.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(
                                        text = "Destacadas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        items(canciones.take(5).size) { idx ->
                                            val song = canciones[idx]
                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                                modifier = Modifier
                                                    .width(130.dp)
                                                    .padding(end = 12.dp)
                                                    .clickable { onCancionClick(idx) }
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    AlbumArt(cancion = song, tamano = 110)
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = song.titulo,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = song.artista,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Todas las canciones",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Full Song List
                        itemsIndexed(cancionesFiltradas) { _, cancion ->
                            val indiceOriginal = canciones.indexOf(cancion)
                            val isSelected = cancionActual?.id == cancion.id

                            SongRow(
                                cancion = cancion,
                                isSelected = isSelected,
                                isPlaying = isSelected && reproduciendo,
                                onClick = { onCancionClick(if (indiceOriginal != -1) indiceOriginal else 0) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Mini Player
        if (cancionActual != null) {
            MiniPlayer(
                cancion = cancionActual,
                reproduciendo = reproduciendo,
                posicion = posicion,
                duracion = duracion,
                onClick = onMiniReproductorClick,
                onPlayPause = onPlayPause,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
