package com.example.huellitasya.features.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.CurvedBackground

@Composable
fun CalendarScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // Fondo curvado
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.6f)
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
                        text = "Calendario",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(209.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gestiona los eventos de tu mascota",
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

            Spacer(modifier = Modifier.height(24.dp))

            // Calendario visual (con el botón cruzado encima)
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                // Contenedor del calendario (332x360)
                Column(
                    modifier = Modifier
                        .width(332.dp)
                        .height(360.dp)
                        .border(1.dp, GreenPrimary, RoundedCornerShape(20.dp))
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header mes
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("<", fontWeight = FontWeight.Bold, fontSize = 16.sp) // Reemplazar con ícono luego
                        Text("September 2026", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text(">", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Días de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        days.forEach {
                            Text(text = it, color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cuadrícula visual de días (Mapeo estático para coincidir con tu diseño)
                    val calendarGrid = listOf(
                        listOf("", "", "", "", "1", "2", "3"),
                        listOf("4", "5", "6", "7", "8", "9", "10"),
                        listOf("11", "12", "13", "14", "15", "16", "17"),
                        listOf("18", "19", "20", "21", "22", "23", "24"),
                        listOf("25", "26", "27", "28", "29", "30", "31")
                    )

                    calendarGrid.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(24.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Botón '+' verde sobrepuesto en el borde inferior
                Box(
                    modifier = Modifier
                        .offset(y = 25.dp) // Lo baja justo la mitad de su tamaño para que pise el borde
                        .size(50.dp)
                        .background(Color(0xFF8DC68F), CircleShape) // Verde clarito como en la foto
                        .clickable { /* TODO: Acción del calendario */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light, modifier = Modifier.offset(y = (-4).dp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Próximos eventos
            Text(
                text = "Proximos eventos",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Gris
            Box(
                modifier = Modifier
                    .width(367.dp)
                    .height(87.dp)
                    .rotate(-180f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF2F2F2))
                    .clickable { /* TODO */ },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_add_placeholder),
                    contentDescription = "Agregar Evento",
                    modifier = Modifier.size(50.dp).rotate(180f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    HuellitasYATheme { CalendarScreen() }
}
