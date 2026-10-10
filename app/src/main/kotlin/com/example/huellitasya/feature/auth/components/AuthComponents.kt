package com.example.huellitasya.feature.auth.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenHighlight
import com.example.huellitasya.core.designsystem.HuellitasYATheme

/** Ancho del frame de Figma ("Android Compact", 412 dp). Las medidas del encabezado escalan respecto a él. */
private const val DESIGN_WIDTH = 412f

/**
 * Cómo se recorta la foto de cabecera, copiado del frame de Figma (en dp de diseño):
 * la imagen se dibuja con [imageWidth] de ancho, desplazada ([imageLeft], [imageTop]) y
 * se muestra solo hasta [visibleHeight]. La ola blanca inferior ya viene en la foto.
 */
internal data class AuthHeroSpec(
    @param:DrawableRes val image: Int,
    val imageWidth: Float,
    val imageLeft: Float,
    val imageTop: Float,
    val visibleHeight: Float,
)

/** Insignia circular bajo la foto (portapapeles, candado...). Posición en dp de diseño. */
internal data class AuthBadgeSpec(
    @param:DrawableRes val image: Int,
    val top: Float,
    val size: DpSize,
)

/** "Título" + parte resaltada en verde ("¡Bienvenido de nuevo" + "!"). */
internal fun highlightedTitle(text: String, highlight: String = ""): AnnotatedString =
    buildAnnotatedString {
        append(text)
        withStyle(SpanStyle(color = GreenHighlight)) { append(highlight) }
    }

/** Logo + "Huellitas YA!" con el "YA!" en verde. Compartido por Presentación y Bienvenido. */
@Composable
internal fun AppTitle(
    modifier: Modifier = Modifier,
    logoSize: DpSize = DpSize(126.dp, 108.dp),
    spacing: Dp = 16.dp,
    fontSize: TextUnit = 48.sp,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo_huellitas),
            contentDescription = stringResource(R.string.auth_logo_description),
            modifier = Modifier.size(logoSize)
        )
        Spacer(modifier = Modifier.height(spacing))
        Text(
            text = highlightedTitle("Huellitas ", "YA!"),
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
    }
}

/** Foto de cabecera recortada según [spec], escalada al ancho real de la pantalla. */
@Composable
internal fun AuthHero(spec: AuthHeroSpec, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val scale = maxWidth.value / DESIGN_WIDTH
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((spec.visibleHeight * scale).dp)
                .clipToBounds()
        ) {
            Image(
                painter = painterResource(spec.image),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopStart,
                modifier = Modifier
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .offset(x = (spec.imageLeft * scale).dp, y = (spec.imageTop * scale).dp)
                    .width((spec.imageWidth * scale).dp)
            )
        }
    }
}

/** Flecha "<" de volver (Figma: chevron girado 90°). */
@Composable
internal fun AuthBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Black,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_back_chevron),
            contentDescription = stringResource(R.string.auth_back),
            tint = tint,
            modifier = Modifier
                .requiredSize(width = 16.dp, height = 14.dp)
                .rotate(90f)
        )
    }
}

/**
 * Plantilla de Login, Registro y Recuperar contraseña (Figma): foto con ola, botón volver,
 * insignia, título y subtítulo; debajo el formulario y, pegado abajo, el botón principal.
 * Todo es desplazable para que el teclado no tape los campos.
 */
@Composable
internal fun AuthFormScaffold(
    hero: AuthHeroSpec,
    badge: AuthBadgeSpec,
    title: AnnotatedString,
    subtitle: String,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    bottomButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backTint: Color = Color.Black,
    titleFontSize: TextUnit = 24.sp,
    titleTopSpacing: Dp = 0.dp,
    subtitleTopSpacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val scale = maxWidth.value / DESIGN_WIDTH
        val viewportHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((badge.top * scale).dp + badge.size.height))
                ) {
                    AuthHero(spec = hero)
                    Image(
                        painter = painterResource(badge.image),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (badge.top * scale).dp)
                            .size(badge.size)
                    )
                    AuthBackButton(
                        onClick = onBackClick,
                        tint = backTint,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 17.dp)
                    )
                }
                Spacer(modifier = Modifier.height(titleTopSpacing))
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(subtitleTopSpacing))
                Text(
                    text = subtitle,
                    color = GrayText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp)
                ) {
                    content()
                }
            }

            Box(modifier = Modifier.padding(top = 24.dp, bottom = 39.dp)) {
                bottomButton()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
        )
    }
}

/** Separador "──── o ────" (Figma: líneas negras al 30 %). */
@Composable
internal fun TextDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.width(319.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Black.copy(alpha = 0.3f))
        Text(
            text = text,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 17.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Black.copy(alpha = 0.3f))
    }
}

/** Separador con huella bajo "¡Bienvenido!" (Figma: líneas negras al 90 % + huella verde). */
@Composable
internal fun PawDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.width(77.dp), color = Color.Black.copy(alpha = 0.9f))
        Image(
            painter = painterResource(R.drawable.ic_paw_divider),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(width = 40.dp, height = 43.dp)
                .padding(horizontal = 5.dp, vertical = 7.dp)
        )
        HorizontalDivider(modifier = Modifier.width(77.dp), color = Color.Black.copy(alpha = 0.9f))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun AuthFormScaffoldPreview() {
    HuellitasYATheme {
        AuthFormScaffold(
            hero = AuthHeroSpec(R.drawable.img_auth_login_hero, 440f, -14f, -104.5f, 353f),
            badge = AuthBadgeSpec(R.drawable.img_auth_badge_clipboard, 323f, DpSize(141.dp, 87.dp)),
            title = highlightedTitle("¡Bienvenido de nuevo", "!"),
            subtitle = "Inicia sesión para continuar cuidando a tu mascota",
            onBackClick = {},
            snackbarHostState = SnackbarHostState(),
            bottomButton = {},
        ) {
            TextDivider(text = "o")
        }
    }
}
