package com.example.musica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musica.Pantalla
import com.example.musica.ui.theme.AccentPurple

@Composable
fun TopNavHeader(
    pantallaActual: Pantalla,
    tieneCancionActual: Boolean,
    onIrInicio: () -> Unit,
    onIrReproductor: () -> Unit,
    onIrLetra: () -> Unit,
    onVolverInicio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (pantallaActual == Pantalla.INICIO) {
            Spacer(Modifier.width(40.dp))
        } else {
            IconButton(onClick = onVolverInicio) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        // Sleek Pill Tab Switcher
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Canciones (Library)
            PillTab(
                titulo = "Canciones",
                isSelected = pantallaActual == Pantalla.INICIO,
                onClick = onIrInicio
            )

            // Tab 2: Canción (Player)
            if (tieneCancionActual) {
                Spacer(Modifier.width(3.dp))
                PillTab(
                    titulo = "Reproductor",
                    isSelected = pantallaActual == Pantalla.REPRODUCTOR,
                    onClick = onIrReproductor
                )

                // Tab 3: Letra
                Spacer(Modifier.width(3.dp))
                PillTab(
                    titulo = "Letra",
                    isSelected = pantallaActual == Pantalla.LETRA,
                    onClick = onIrLetra
                )
            }
        }

        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun PillTab(
    titulo: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) AccentPurple else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = titulo,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
