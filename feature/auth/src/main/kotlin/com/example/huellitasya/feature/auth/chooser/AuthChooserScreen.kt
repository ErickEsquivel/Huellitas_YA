package com.example.huellitasya.feature.auth.chooser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.core.designsystem.GrayText
import com.example.huellitasya.core.designsystem.GreenAction
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.MintSurface
import com.example.huellitasya.core.designsystem.components.GoogleOptionButton
import com.example.huellitasya.core.designsystem.components.HuellitasOptionButton
import com.example.huellitasya.core.designsystem.components.LoadingOverlay
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.auth.R
import com.example.huellitasya.feature.auth.components.AppTitle
import com.example.huellitasya.feature.auth.components.AuthHero
import com.example.huellitasya.feature.auth.components.AuthHeroSpec
import com.example.huellitasya.feature.auth.components.PawDivider
import com.example.huellitasya.feature.auth.components.TextDivider
import com.example.huellitasya.feature.auth.google.rememberGoogleIdTokenRequester
import kotlinx.coroutines.launch

// Figma "BIENVENIDO" (43:3)
private val ChooserHero = AuthHeroSpec(
    image = R.drawable.img_auth_welcome_hero,
    imageWidth = 428.2f,
    imageLeft = -7.1f,
    imageTop = -22.6f,
    visibleHeight = 531f,
)

@Composable
internal fun AuthChooserRoute(
    googleServerClientId: String,
    onEmailClick: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: AuthChooserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleRequester = rememberGoogleIdTokenRequester(googleServerClientId)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AuthChooserEvent.LaunchGoogleSignIn -> scope.launch {
                viewModel.onGoogleIdTokenResult(googleRequester.requestIdToken())
            }
            is AuthChooserEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    AuthChooserScreen(
        isLoading = uiState.isLoading,
        snackbarHostState = snackbarHostState,
        onEmailClick = onEmailClick,
        onGoogleClick = viewModel::onGoogleClick,
        onRegisterClick = onRegisterClick,
    )
}

@Composable
internal fun AuthChooserScreen(
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onEmailClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val scale = maxWidth.value / 412f
        val viewportHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AuthHero(spec = ChooserHero)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (57 * scale).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppTitle(spacing = 0.dp, modifier = Modifier.offset(y = (-8).dp))
                        Text(
                            text = stringResource(R.string.auth_chooser_subtitle),
                            color = GrayText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(288.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(11.dp))
                Text(
                    text = stringResource(R.string.auth_chooser_title),
                    color = Color.Black,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                PawDivider()
                Spacer(modifier = Modifier.height(25.dp))

                HuellitasOptionButton(
                    text = stringResource(R.string.auth_chooser_email),
                    icon = painterResource(R.drawable.ic_auth_mail),
                    iconSize = DpSize(29.dp, 20.dp),
                    iconColumnStart = 42.dp,
                    containerColor = GreenAction,
                    enabled = !isLoading,
                    onClick = onEmailClick
                )
                Spacer(modifier = Modifier.height(11.dp))
                GoogleOptionButton(
                    text = stringResource(R.string.auth_chooser_google),
                    enabled = !isLoading,
                    onClick = onGoogleClick
                )
            }

            Column(
                modifier = Modifier.padding(top = 32.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextDivider(text = stringResource(R.string.auth_chooser_or))
                Spacer(modifier = Modifier.height(14.dp))
                HuellitasOptionButton(
                    text = stringResource(R.string.auth_chooser_register),
                    icon = painterResource(R.drawable.ic_register_person),
                    iconSize = DpSize(23.75.dp, 23.75.dp),
                    iconColumnStart = 44.6.dp,
                    containerColor = MintSurface.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f)),
                    contentColor = Color.Black,
                    fontWeight = FontWeight.Bold,
                    enabled = !isLoading,
                    onClick = onRegisterClick
                )
            }
        }

        LoadingOverlay(isVisible = isLoading)
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun AuthChooserScreenPreview() {
    HuellitasYATheme {
        AuthChooserScreen(
            isLoading = false,
            snackbarHostState = SnackbarHostState(),
            onEmailClick = {},
            onGoogleClick = {},
            onRegisterClick = {},
        )
    }
}
