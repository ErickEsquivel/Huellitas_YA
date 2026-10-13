package com.example.huellitasya.feature.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Fila de la checklist de "Crear nueva contraseña": ✗ naranja si falta, ✓ verde si se cumple. */
@Composable
internal fun PasswordRequirementRow(
    text: String,
    isMet: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(if (isMet) R.string.auth_newpassword_rule_met else R.string.auth_newpassword_rule_unmet)
    Row(
        modifier = modifier.semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ambos íconos ocupan la misma columna (24 dp) para que los textos queden alineados.
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(if (isMet) R.drawable.ic_password_rule_met else R.drawable.ic_password_rule_unmet),
                contentDescription = null,
                modifier = if (isMet) Modifier.size(16.5.dp) else Modifier.size(width = 24.dp, height = 20.dp),
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            color = Color.Black.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordRequirementRowPreview() {
    HuellitasYATheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PasswordRequirementRow(text = "Mínimo 8 caracteres", isMet = true)
            PasswordRequirementRow(text = "Una letra mayúscula", isMet = false)
        }
    }
}
