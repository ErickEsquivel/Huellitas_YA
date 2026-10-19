package com.example.huellitasya.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.AddPlaceholderCard
import com.example.huellitasya.core.designsystem.components.CurvedBackground
import com.example.huellitasya.core.designsystem.components.ScreenHeader
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.home.components.PetCard
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun HomeRoute(
    onAddPetClick: () -> Unit,
    onPetClick: (petId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    HomeScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAddPetClick = onAddPetClick,
        onPetClick = onPetClick,
    )
}

/** Figma "HOME 4" sin mascotas (3050:2045), "HOME 1" con una (145:1126) y "HOME 2" con varias (19:100). */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onAddPetClick: () -> Unit,
    onPetClick: (petId: String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        CurvedBackground(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.55f)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 45.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.home_title),
                    subtitle = stringResource(R.string.home_subtitle),
                    notificationsDescription = stringResource(R.string.home_notifications),
                    modifier = Modifier.padding(bottom = 22.dp),
                )
            }
            if (!state.isLoading) {
                items(state.pets, key = { it.id }) { pet ->
                    PetCard(pet = pet, onClick = { onPetClick(pet.id) })
                }
                item(key = "add") {
                    AddPlaceholderCard(
                        contentDescription = stringResource(R.string.home_add_pet),
                        onClick = onAddPetClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun HomeScreenPreview() {
    HuellitasYATheme {
        HomeScreen(
            state = HomeUiState(
                pets = listOf(
                    Pet("1", "Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", LocalDate.now().minusMonths(3), PetSex.MALE, 5.0, null),
                    Pet("2", "Maximiliano Cuarto", PetSpecies.DOG, "Jack Russell Terrier", LocalDate.now().minusMonths(8), PetSex.MALE, 15.0, null),
                ),
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onAddPetClick = {},
            onPetClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun HomeScreenEmptyPreview() {
    HuellitasYATheme {
        HomeScreen(
            state = HomeUiState(isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onAddPetClick = {},
            onPetClick = {},
        )
    }
}
