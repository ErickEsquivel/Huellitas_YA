package com.example.huellitasya.feature.auth.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenHighlight
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import kotlin.math.roundToInt

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

/**
 * Foto de cabecera recortada según [spec], escalada al ancho real de la pantalla.
 *
 * Sin restricción de alto ocupa el alto de Figma. Si el padre le da menos alto, la foto se
 * desplaza hacia arriba para que la ola blanca inferior siga visible al encogerse.
 */
@Composable
internal fun AuthHero(spec: AuthHeroSpec, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val scale = maxWidth.value / DESIGN_WIDTH
        val designHeight = (spec.visibleHeight * scale).dp
        val height = if (constraints.hasBoundedHeight) minOf(maxHeight, designHeight) else designHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clipToBounds()
        ) {
            Image(
                painter = painterResource(spec.image),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopStart,
                modifier = Modifier
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .offset(
                        x = (spec.imageLeft * scale).dp,
                        y = (spec.imageTop * scale).dp - (designHeight - height),
                    )
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

/** Por debajo de este alto de viewport (sin teclado) el formulario usa tipografía y espacios compactos. */
private val CompactViewportHeight = 640.dp

/** Alto mínimo de la cabecera con el teclado cerrado; por debajo ya no se reconoce la foto. */
private val HeaderMinHeight = 96.dp

/** Espacio mínimo entre el formulario y el botón principal. */
private val FooterMinGap = 16.dp

/**
 * Plantilla de Login, Registro y Recuperar contraseña (Figma): foto con ola, botón volver,
 * insignia, título y subtítulo; debajo el formulario y, pegado abajo, el botón principal.
 *
 * Se adapta sin scroll (spec 002, "Adaptación a pantalla y teclado"):
 * - La cabecera ocupa el espacio que sobra: crece hasta el alto de Figma en pantallas altas y
 *   se encoge en las bajas.
 * - Con el teclado abierto la cabecera y el subtítulo se ocultan con animación, el formulario
 *   pasa a modo compacto y el botón queda justo encima del teclado.
 * - Solo si el contenido no cabe ni así (pantallas diminutas o fuente del sistema muy grande)
 *   se puede desplazar, como respaldo.
 *
 * @param content recibe `isCompact` para que cada pantalla reduzca sus espaciados.
 * @param forceKeyboardLayout muestra el modo "teclado abierto" (solo para previews).
 */
@OptIn(ExperimentalLayoutApi::class)
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
    compactTitleFontSize: TextUnit = 20.sp,
    subtitleTopSpacing: Dp = 0.dp,
    forceKeyboardLayout: Boolean = false,
    content: @Composable ColumnScope.(isCompact: Boolean) -> Unit,
) {
    val isKeyboardOpen = forceKeyboardLayout || WindowInsets.isImeVisible
    val headerFraction by animateFloatAsState(
        targetValue = if (isKeyboardOpen) 0f else 1f,
        label = "authHeaderFraction",
    )
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // El viewport medido excluye el teclado (y sigue su animación) o, sin teclado, la barra de
    // navegación del sistema: la app dibuja edge-to-edge y el botón no debe quedar debajo.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom))
    ) {
        val scale = maxWidth.value / DESIGN_WIDTH
        val isCompact = isKeyboardOpen || maxHeight < CompactViewportHeight
        val heroHeight = (hero.visibleHeight * scale).dp
        val badgeOverlap = ((hero.visibleHeight - badge.top) * scale).dp
        val badgeBelowHero = (badge.size.height - badgeOverlap).coerceAtLeast(0.dp)

        AuthAdaptiveLayout(
            viewportHeight = maxHeight,
            headerMaxHeight = heroHeight + badgeBelowHero,
            headerFraction = headerFraction,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            header = { AuthHeader(hero = hero, badge = badge, badgeBelowHero = badgeBelowHero) },
            body = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sin cabecera, el título baja lo justo para no quedar bajo la flecha de volver.
                    Spacer(modifier = Modifier.height((statusBarTop + 44.dp) * (1f - headerFraction)))
                    val titleSize = if (isCompact) compactTitleFontSize else titleFontSize
                    Text(
                        text = title,
                        color = Color.Black,
                        fontSize = titleSize,
                        // El estilo base fija 24 sp de interlineado: con títulos grandes las líneas se pisaban.
                        lineHeight = titleSize * 1.2f,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    AnimatedVisibility(
                        visible = !isKeyboardOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Text(
                            text = subtitle,
                            color = GrayText,
                            fontSize = if (isCompact) 16.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 48.dp, end = 48.dp, top = subtitleTopSpacing)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp)
                    ) {
                        content(isCompact)
                    }
                }
            },
            footer = {
                Box(modifier = Modifier.padding(bottom = if (isCompact) 12.dp else 39.dp)) {
                    bottomButton()
                }
            },
        )

        AuthBackButton(
            onClick = onBackClick,
            // Sobre la foto se usa el color de Figma; sin foto, negro para que se vea sobre el blanco.
            tint = if (headerFraction > 0.5f) backTint else Color.Black,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 17.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Foto + insignia. La insignia se apoya en la ola (solapamiento de Figma) y desaparece si no hay sitio. */
@Composable
private fun AuthHeader(hero: AuthHeroSpec, badge: AuthBadgeSpec, badgeBelowHero: Dp) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val showBadge = maxHeight >= badge.size.height + HeaderMinHeight / 2
        AuthHero(
            spec = hero,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBadge) badgeBelowHero else 0.dp)
        )
        if (showBadge) {
            Image(
                painter = painterResource(badge.image),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(badge.size)
            )
        }
    }
}

/**
 * Coloca cabecera, cuerpo y pie en el alto del viewport sin desbordarse:
 * 1. mide primero el cuerpo y el pie;
 * 2. la cabecera recibe el espacio sobrante, entre [HeaderMinHeight] y [headerMaxHeight],
 *    multiplicado por [headerFraction] (0 = oculta, con el teclado abierto);
 * 3. el pie va pegado abajo. Si nada cabe, el alto total crece y el scroll del padre hace de respaldo.
 */
@Composable
private fun AuthAdaptiveLayout(
    viewportHeight: Dp,
    headerMaxHeight: Dp,
    headerFraction: Float,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(header, body, footer), modifier = modifier) { measurables, constraints ->
        val (headerMeasurables, bodyMeasurables, footerMeasurables) = measurables
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)

        val bodyPlaceable = bodyMeasurables.single().measure(loose)
        val footerPlaceable = footerMeasurables.single().measure(loose)

        val viewport = viewportHeight.roundToPx()
        val gap = FooterMinGap.roundToPx()
        val maxHeader = headerMaxHeight.roundToPx()
        val minHeader = HeaderMinHeight.roundToPx().coerceAtMost(maxHeader)
        val freeSpace = viewport - bodyPlaceable.height - footerPlaceable.height - gap
        val headerHeight = (freeSpace.coerceIn(minHeader, maxHeader) * headerFraction).roundToInt()
        val headerPlaceable = headerMeasurables.single().measure(Constraints.fixed(width, headerHeight))

        val contentHeight = headerHeight + bodyPlaceable.height + gap + footerPlaceable.height
        val totalHeight = maxOf(viewport, contentHeight)

        layout(width, totalHeight) {
            headerPlaceable.place(0, 0)
            bodyPlaceable.place((width - bodyPlaceable.width) / 2, headerHeight)
            footerPlaceable.place((width - footerPlaceable.width) / 2, totalHeight - footerPlaceable.height)
        }
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
        ) { _ ->
            TextDivider(text = "o")
        }
    }
}
