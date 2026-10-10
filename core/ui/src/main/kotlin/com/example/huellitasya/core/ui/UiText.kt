package com.example.huellitasya.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Texto que un ViewModel entrega a la UI sin depender de `Context`.
 * La UI lo resuelve con [asString].
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    class Resource(@param:StringRes val id: Int, vararg val args: Any) : UiText {
        override fun equals(other: Any?): Boolean =
            other is Resource && id == other.id && args.contentEquals(other.args)

        override fun hashCode(): Int = 31 * id + args.contentHashCode()
    }

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id, *args)
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args)
    }
}
