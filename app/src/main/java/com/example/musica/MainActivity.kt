package com.example.musica

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musica.ui.screens.HomeScreen
import com.example.musica.ui.screens.LyricsScreen
import com.example.musica.ui.screens.PlayerScreen
import com.example.musica.ui.theme.MusicaTheme
import kotlinx.coroutines.delay

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
            MusicaTheme {
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
        pantalla == Pantalla.INICIO || cancionActual == null -> HomeScreen(
            canciones = canciones,
            permisoConcedido = permisoConcedido,
            cancionActual = cancionActual,
            reproduciendo = reproduciendo,
            posicion = posicion,
            duracion = duracion,
            onPedirPermiso = { pedirPermisos() },
            onCancionClick = { indice ->
                reproducir(indice)
                pantalla = Pantalla.REPRODUCTOR
            },
            onIrReproductor = { pantalla = Pantalla.REPRODUCTOR },
            onIrLetra = { pantalla = Pantalla.LETRA },
            onMiniReproductorClick = { pantalla = Pantalla.REPRODUCTOR },
            onPlayPause = alternarPlayPause
        )

        pantalla == Pantalla.REPRODUCTOR -> PlayerScreen(
            cancion = cancionActual,
            reproduciendo = reproduciendo,
            posicion = posicion,
            duracion = duracion,
            onSeek = buscar,
            onPlayPause = alternarPlayPause,
            onSiguiente = siguiente,
            onAnterior = anterior,
            onVolverInicio = { pantalla = Pantalla.INICIO },
            onVerLetra = { pantalla = Pantalla.LETRA }
        )

        else -> LyricsScreen(
            cancion = cancionActual,
            reproduciendo = reproduciendo,
            posicion = posicion,
            duracion = duracion,
            onSeek = buscar,
            onPlayPause = alternarPlayPause,
            onVolverInicio = { pantalla = Pantalla.INICIO },
            onVerCancion = { pantalla = Pantalla.REPRODUCTOR }
        )
    }
}
