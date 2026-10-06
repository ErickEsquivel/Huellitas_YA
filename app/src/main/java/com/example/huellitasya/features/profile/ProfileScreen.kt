package com.example.huellitasya.features.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.HuellitasButton

@Composable
fun ProfileScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // Fondo curvado (Mucho más bajo como pediste)
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.3f)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header (Textos y Campana)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Perfil",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(209.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gestiona los eventos de tu mascota", // Copié el subtítulo de la imagen
                        color = GrayText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(266.dp)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_bell_placeholder),
                    contentDescription = "Notificaciones",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Imagen de Perfil Circular
            Image(
                painter = painterResource(id = R.drawable.img_profile_placeholder),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre de usuario
            Text(
                text = "Santiago Cruz",
                color = Color.Black,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold, // 700
                modifier = Modifier.width(164.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Lista de información
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileInfoItem(label = "Correo electronico", value = "Sa.company@hotmail.com", isLink = true)
                Spacer(modifier = Modifier.height(16.dp))
                ProfileInfoItem(label = "Teléfono", value = "+57 3124456311")
                Spacer(modifier = Modifier.height(16.dp))
                ProfileInfoItem(label = "Dirección", value = "61a Sur27 Cra. 104a Bis")
                Spacer(modifier = Modifier.height(16.dp))
                ProfileInfoItem(label = "Fecha de nacimiento", value = "15/01/2005")
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // Botón Editar
            HuellitasButton(
                text = "Editar información",
                onClick = { /* TODO: Navegar a edición */ },
                // En la descripción pusiste 279x48, por ahora HuellitasButton tiene fijo 273x46
                // Podríamos parametrizar el tamaño en el futuro si lo necesitas.
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// Subcomponente para cada campo de información
@Composable
fun ProfileInfoItem(label: String, value: String, isLink: Boolean = false) {
    Column {
        Text(
            text = label,
            color = GrayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold, // 700
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold, // 700
            textDecoration = if (isLink) TextDecoration.Underline else TextDecoration.None
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    HuellitasYATheme { ProfileScreen() }
}
