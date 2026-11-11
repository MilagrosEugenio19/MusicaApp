package com.example.musica.ui.components

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.musica.Cancion
import com.example.musica.ui.theme.AccentPink
import com.example.musica.ui.theme.AccentPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumArt(
    cancion: Cancion?,
    tamano: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, cancion?.id) {
        value = if (cancion != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.loadThumbnail(cancion.uri, Size(512, 512), null)
                } catch (e: Exception) {
                    null
                }
            }
        } else null
    }

    val cornerRadius = if (tamano > 100) 24.dp else 12.dp
    val shadowElevation = if (tamano > 100) 12.dp else 2.dp

    Box(
        modifier = modifier
            .size(tamano.dp)
            .shadow(shadowElevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(AccentPurple.copy(alpha = 0.8f), AccentPink.copy(alpha = 0.8f)))),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = cancion?.titulo ?: "Portada de álbum",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size((tamano * 0.45).dp)
            )
        }
    }
}
