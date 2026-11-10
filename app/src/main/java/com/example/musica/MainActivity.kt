package com.example.musica

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ---------- Datos ----------

data class Cancion(
    val id: Long,
    val titulo: String,
    val artista: String,
    val albumId: Long,
    val uri: Uri,
    val letra: String = "Aquí va la letra de la canción.\n\nPuedes pegar la letra completa en este campo."
)

enum class Pantalla { INICIO, REPRODUCTOR, LETRA }

private val FondoOscuro = Color(0xFF0B1030)
private val FondoLetra = Color(0xFF1B2623)
private val Rosa = Color(0xFFE91E8C)
private val Morado = Color(0xFF7B2FF7)

/** Lee las canciones guardadas en el celular. */
fun cargarCanciones(context: Context): List<Cancion> {
    val lista = mutableListOf<Cancion>()
    val coleccion = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val proyeccion = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID
    )
    // Solo música de más de 30 segundos (evita grabaciones y tonos)
    val filtro = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"

    context.contentResolver.query(
        coleccion, proyeccion, filtro, null, "${MediaStore.Audio.Media.TITLE} ASC"
    )?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val tituloCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistaCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            lista.add(
                Cancion(
                    id = id,
                    titulo = c.getString(tituloCol) ?: "Sin título",
                    artista = c.getString(artistaCol)?.takeIf { it != "<unknown>" } ?: "Artista desconocido",
                    albumId = c.getLong(albumCol),
                    uri = ContentUris.withAppendedId(coleccion, id)
                )
            )
        }
    }
    return lista
}

fun Cancion.aMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(titulo)
                .setArtist(artista)
                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/$albumId"))
                .build()
        )
        .build()

fun formato(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}

// ---------- Activity ----------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppMusica()
            }
        }
    }
}

// ---------- App: permisos, conexión con el servicio y navegación ----------

@Composable
fun AppMusica() {
    val context = LocalContext.current

    var pantalla by remember { mutableStateOf(Pantalla.INICIO) }
    var canciones by remember { mutableStateOf<List<Cancion>>(emptyList()) }
    var permisoConcedido by remember { mutableStateOf(false) }

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var reproduciendo by remember { mutableStateOf(false) }
    var indiceActual by remember { mutableStateOf(-1) }
    var posicion by remember { mutableStateOf(0L) }
    var duracion by remember { mutableStateOf(0L) }

    // ----- Permisos -----
    val permisoAudio = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado[permisoAudio] == true) {
            permisoConcedido = true
            canciones = cargarCanciones(context)
        }
    }

    fun pedirPermisos() {
        val necesarios = mutableListOf(permisoAudio)
        if (Build.VERSION.SDK_INT >= 33) necesarios.add(Manifest.permission.POST_NOTIFICATIONS)
        val faltantes = necesarios.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ContextCompat.checkSelfPermission(context, permisoAudio) == PackageManager.PERMISSION_GRANTED) {
            permisoConcedido = true
            canciones = cargarCanciones(context)
        }
        if (faltantes.isNotEmpty()) lanzador.launch(faltantes.toTypedArray())
    }

    LaunchedEffect(Unit) { pedirPermisos() }

    // ----- Conexión con el servicio de música -----
    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, ServicioMusica::class.java))
        val futuro = MediaController.Builder(context, token).buildAsync()

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                reproduciendo = player.isPlaying
                indiceActual = if (player.mediaItemCount > 0) player.currentMediaItemIndex else -1
                duracion = player.duration.coerceAtLeast(0L)
                posicion = player.currentPosition
            }
        }

        futuro.addListener({
            try {
                val c = futuro.get()
                c.addListener(listener)
                controller = c
                // Si la música ya estaba sonando, nos ponemos al día
                reproduciendo = c.isPlaying
                indiceActual = if (c.mediaItemCount > 0) c.currentMediaItemIndex else -1
                duracion = c.duration.coerceAtLeast(0L)
                posicion = c.currentPosition
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            MediaController.releaseFuture(futuro)
            controller = null
        }
    }

    // Actualiza la barra de progreso mientras suena
    LaunchedEffect(controller, reproduciendo) {
        while (reproduciendo) {
            controller?.let {
                posicion = it.currentPosition
                duracion = it.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // ----- Acciones -----
    val cancionActual = canciones.getOrNull(indiceActual)

    fun reproducir(indice: Int) {
        controller?.let { c ->
            if (!(indice == indiceActual && c.mediaItemCount > 0)) {
                c.setMediaItems(canciones.map { it.aMediaItem() }, indice, 0L)
                c.prepare()
            }
            c.play()
        }
    }

    val alternarPlayPause: () -> Unit = {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }
    val siguiente: () -> Unit = { controller?.seekToNextMediaItem() }
    val anterior: () -> Unit = { controller?.seekToPreviousMediaItem() }
    val buscar: (Long) -> Unit = { ms -> controller?.seekTo(ms); posicion = ms }

    // Botón "atrás" del celular
    BackHandler(enabled = pantalla != Pantalla.INICIO) {
        pantalla = if (pantalla == Pantalla.LETRA) Pantalla.REPRODUCTOR else Pantalla.INICIO
    }

    // ----- Pantallas -----
    when {
        pantalla == Pantalla.INICIO || cancionActual == null -> PantallaInicio(
            canciones = canciones,
            permisoConcedido = permisoConcedido,
            cancionActual = cancionActual,
            reproduciendo = reproduciendo,
            onPedirPermiso = { pedirPermisos() },
            onCancionClick = { indice ->
                reproducir(indice)
                pantalla = Pantalla.REPRODUCTOR
            },
            onMiniReproductorClick = { pantalla = Pantalla.REPRODUCTOR },
            onPlayPause = alternarPlayPause
        )

        pantalla == Pantalla.REPRODUCTOR -> PantallaReproductor(
            cancion = cancionActual,
            reproduciendo = reproduciendo,
            posicion = posicion,
            duracion = duracion,
            onSeek = buscar,
            onPlayPause = alternarPlayPause,
            onSiguiente = siguiente,
            onAnterior = anterior,
            onVolver = { pantalla = Pantalla.INICIO },
            onVerLetra = { pantalla = Pantalla.LETRA }
        )

        else -> PantallaLetra(
            cancion = cancionActual,
            reproduciendo = reproduciendo,
            posicion = posicion,
            duracion = duracion,
            onSeek = buscar,
            onPlayPause = alternarPlayPause,
            onVolver = { pantalla = Pantalla.INICIO },
            onVerCancion = { pantalla = Pantalla.REPRODUCTOR }
        )
    }
}

// ---------- Pantalla 1: Inicio ----------

@Composable
fun PantallaInicio(
    canciones: List<Cancion>,
    permisoConcedido: Boolean,
    cancionActual: Cancion?,
    reproduciendo: Boolean,
    onPedirPermiso: () -> Unit,
    onCancionClick: (Int) -> Unit,
    onMiniReproductorClick: () -> Unit,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Canciones",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
            )

            when {
                !permisoConcedido -> Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Necesito permiso para leer la música de tu celular.",
                        color = Color.White, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onPedirPermiso) { Text("Dar permiso") }
                }

                canciones.isEmpty() -> Text(
                    "No se encontraron canciones en este celular.",
                    color = Color.Gray,
                    modifier = Modifier.padding(24.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    itemsIndexed(canciones) { indice, cancion ->
                        FilaCancion(cancion = cancion, onClick = { onCancionClick(indice) })
                    }
                }
            }
        }

        // Mini reproductor
        if (cancionActual != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.horizontalGradient(listOf(Rosa, Morado)))
                    .clickable { onMiniReproductorClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Portada(cancion = cancionActual, tamano = 40)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cancionActual.titulo, color = Color.White,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        cancionActual.artista, color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp, maxLines = 1
                    )
                }
                TextButton(onClick = onPlayPause) {
                    Text(if (reproduciendo) "⏸" else "▶", fontSize = 22.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun FilaCancion(cancion: Cancion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Portada(cancion = cancion, tamano = 48)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                cancion.titulo, color = Color.White, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(cancion.artista, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
        }
        Text("⋮", color = Color.Gray, fontSize = 20.sp)
    }
}

// ---------- Pantalla 2: Reproductor ----------

@Composable
fun PantallaReproductor(
    cancion: Cancion,
    reproduciendo: Boolean,
    posicion: Long,
    duracion: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSiguiente: () -> Unit,
    onAnterior: () -> Unit,
    onVolver: () -> Unit,
    onVerLetra: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BarraSuperior(
            seleccionada = "Canción",
            onVolver = onVolver,
            onCancion = {},
            onLetra = onVerLetra
        )

        Spacer(Modifier.height(24.dp))
        Portada(cancion = cancion, tamano = 300)
        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cancion.titulo, color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(cancion.artista, color = Color.Gray, fontSize = 16.sp, maxLines = 1)
            }
            Text("♡", color = Color.White, fontSize = 24.sp)
        }

        Slider(
            value = if (duracion > 0) posicion.toFloat() / duracion else 0f,
            onValueChange = { onSeek((it * duracion).toLong()) }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formato(posicion), color = Color.Gray, fontSize = 12.sp)
            Text(formato(duracion), color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(Modifier.height(8.dp))
        ControlesReproduccion(
            reproduciendo = reproduciendo,
            onPlayPause = onPlayPause,
            onSiguiente = onSiguiente,
            onAnterior = onAnterior
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ---------- Pantalla 3: Letra ----------

@Composable
fun PantallaLetra(
    cancion: Cancion,
    reproduciendo: Boolean,
    posicion: Long,
    duracion: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onVolver: () -> Unit,
    onVerCancion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoLetra)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        BarraSuperior(
            seleccionada = "Letra",
            onVolver = onVolver,
            onCancion = onVerCancion,
            onLetra = {}
        )

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Portada(cancion = cancion, tamano = 40)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(cancion.titulo, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(cancion.artista, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Text(
                text = cancion.letra,
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPlayPause) {
                Text(if (reproduciendo) "⏸" else "▶", fontSize = 26.sp, color = Color.White)
            }
            Text(formato(posicion), color = Color.Gray, fontSize = 12.sp)
            Slider(
                value = if (duracion > 0) posicion.toFloat() / duracion else 0f,
                onValueChange = { onSeek((it * duracion).toLong()) },
                modifier = Modifier.weight(1f)
            )
            Text(formato(duracion), color = Color.Gray, fontSize = 12.sp)
        }
    }
}

// ---------- Componentes reutilizables ----------

@Composable
fun BarraSuperior(
    seleccionada: String,
    onVolver: () -> Unit,
    onCancion: () -> Unit,
    onLetra: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onVolver) {
            Text("⌄", fontSize = 28.sp, color = Color.White)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onCancion) {
            Text(
                "Canción",
                color = if (seleccionada == "Canción") Color.White else Color.Gray,
                fontWeight = if (seleccionada == "Canción") FontWeight.Bold else FontWeight.Normal
            )
        }
        Text("|", color = Color.Gray)
        TextButton(onClick = onLetra) {
            Text(
                "Letra",
                color = if (seleccionada == "Letra") Color.White else Color.Gray,
                fontWeight = if (seleccionada == "Letra") FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
fun ControlesReproduccion(
    reproduciendo: Boolean,
    onPlayPause: () -> Unit,
    onSiguiente: () -> Unit,
    onAnterior: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onAnterior) { Text("⏮", fontSize = 28.sp, color = Color.White) }
        FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
            Text(if (reproduciendo) "⏸" else "▶", fontSize = 28.sp)
        }
        TextButton(onClick = onSiguiente) { Text("⏭", fontSize = 28.sp, color = Color.White) }
    }
}

/** Portada real del álbum si existe; si no, un cuadro de color con una nota. */
@Composable
fun Portada(cancion: Cancion?, tamano: Int) {
    val context = LocalContext.current
    val imagen by produceState<Bitmap?>(initialValue = null, cancion?.id) {
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

    Box(
        modifier = Modifier
            .size(tamano.dp)
            .clip(RoundedCornerShape(if (tamano > 100) 16.dp else 8.dp))
            .background(Color(0xFF4DB6AC)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = imagen
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("🎵", fontSize = (tamano / 2.5).sp)
        }
    }
}