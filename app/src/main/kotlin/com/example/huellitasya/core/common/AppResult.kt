package com.example.huellitasya.core.common

/** Marca los tipos de error de dominio que viajan dentro de [AppResult]. */
interface AppError

/** Resultado de una operación que puede fallar con un error tipado (en lugar de lanzar excepciones). */
sealed interface AppResult<out D, out E : AppError> {
    data class Success<out D>(val data: D) : AppResult<D, Nothing>
    data class Failure<out E : AppError>(val error: E) : AppResult<Nothing, E>
}

inline fun <D, E : AppError, R> AppResult<D, E>.map(transform: (D) -> R): AppResult<R, E> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Failure -> this
    }

inline fun <D, E : AppError> AppResult<D, E>.onSuccess(action: (D) -> Unit): AppResult<D, E> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <D, E : AppError> AppResult<D, E>.onFailure(action: (E) -> Unit): AppResult<D, E> {
    if (this is AppResult.Failure) action(error)
    return this
}
