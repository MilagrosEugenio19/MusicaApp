package com.example.musica

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object LyricsFetcher {

    suspend fun obtenerLetra(context: Context, cancion: Cancion): String {
        return withContext(Dispatchers.IO) {
            // 1. Probar metadatos locales incrustados en el archivo MP3
            val letraLocal = obtenerLetraLocal(context, cancion.uri)
            if (!letraLocal.isNullOrBlank()) {
                return@withContext letraLocal
            }

            // 2. Probar API gratuita LRCLIB por internet
            val letraOnline = obtenerLetraLRCLIB(cancion.titulo, cancion.artista)
            if (!letraOnline.isNullOrBlank()) {
                return@withContext letraOnline
            }

            // 3. Fallback
            "No se encontró la letra de esta canción."
        }
    }

    private fun obtenerLetraLocal(context: Context, uri: Uri): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            // METADATA_KEY_LYRICS es constante 1000 en MediaMetadataRetriever
            val lyrics = retriever.extractMetadata(1000)
            retriever.release()
            lyrics?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun obtenerLetraLRCLIB(titulo: String, artista: String): String? {
        return try {
            val cleanTitle = titulo.replace(Regex("(?i)\\([^)]*\\)|\\[[^\\]]*\\]"), "").trim()
            val cleanArtist = if (artista == "Artista desconocido" || artista == "<unknown>") "" else artista.trim()

            val urlString = "https://lrclib.net/api/get?track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonText)

                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotBlank()) return plainLyrics

                val syncedLyrics = json.optString("syncedLyrics", "")
                if (syncedLyrics.isNotBlank()) {
                    return syncedLyrics.replace(Regex("\\[\\d+:\\d+\\.\\d+\\]"), "").trim()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
