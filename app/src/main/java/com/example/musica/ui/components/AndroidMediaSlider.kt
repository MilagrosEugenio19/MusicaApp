package com.example.musica.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musica.formato
import com.example.musica.ui.theme.AccentPink
import com.example.musica.ui.theme.AccentPurple
import kotlin.math.sin

@Composable
fun AndroidMediaSlider(
    posicion: Long,
    duracion: Long,
    reproduciendo: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isScrubbing) {
        scrubFraction
    } else {
        if (duracion > 0) (posicion.toFloat() / duracion.toFloat()).coerceIn(0f, 1f) else 0f
    }

    // Animated wave phase for Android 13 squiggly media bar style
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(duracion) {
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((newFraction * duracion).toLong())
                    }
                }
                .pointerInput(duracion) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek((scrubFraction * duracion).toLong())
                            isScrubbing = false
                        },
                        onDragCancel = { isScrubbing = false },
                        onHorizontalDrag = { change, _ ->
                            scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                }
        ) {
            val width = size.width
            val centerY = size.height / 2f
            val progressWidth = width * currentFraction
            val strokeWidth = 5.dp.toPx()

            // 1. Inactive Track Line (Background)
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(progressWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // 2. Active Track Line (Wavy if playing, smooth straight if paused)
            if (reproduciendo && progressWidth > 20f) {
                val wavePath = Path()
                val waveLength = 40f
                val waveAmplitude = 4.dp.toPx()

                var x = 0f
                wavePath.moveTo(0f, centerY)
                while (x <= progressWidth) {
                    val y = centerY + sin((x / waveLength) * 2f * Math.PI.toFloat() + wavePhase) * waveAmplitude
                    wavePath.lineTo(x, y)
                    x += 4f
                }

                drawPath(
                    path = wavePath,
                    color = AccentPurple,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                drawLine(
                    color = AccentPurple,
                    start = Offset(0f, centerY),
                    end = Offset(progressWidth, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Thumb Circle / Pill
            drawCircle(
                color = AccentPink,
                radius = if (isScrubbing) 9.dp.toPx() else 7.dp.toPx(),
                center = Offset(progressWidth.coerceIn(0f, width), centerY)
            )
        }

        Spacer(Modifier.height(2.dp))

        // Timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayMs = if (isScrubbing) (scrubFraction * duracion).toLong() else posicion
            Text(
                text = formato(displayMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
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
