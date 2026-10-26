package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError

/** Descarga un archivo de un registro médico a la carpeta Descargas del teléfono (spec 006). */
interface RecordDownloader {
    /** Encola la descarga; el sistema muestra su avance en una notificación. */
    fun download(url: String, fileName: String, contentType: String): AppResult<Unit, DataError>
}
