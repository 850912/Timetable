package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hufeng943.timetable.presentation.ui.screens.common.ErrorScreen
import com.hufeng943.timetable.presentation.ui.screens.common.LoadingScreen
import com.hufeng943.timetable.presentation.viewmodel.AppError
import com.hufeng943.timetable.presentation.viewmodel.UiState

@Composable
fun <T> HandleEditUiState(
    uiState: UiState<T>,
    emptyAsSuccess: T? = null,
    emptyContent: @Composable () -> Unit = { ErrorScreen(AppError.UnexpectedEmpty()) },
    successContent: @Composable (T) -> Unit
) {
    // Keep state swaps layout-stable on Wear OS 6. Scaling transitions can overlap
    // with Lazy transformations while the item height is being recalculated.
    AnimatedContent(
        targetState = uiState,
        contentKey = { state -> state::class },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        label = "UiStateTransition"
    ){ targetState ->
        when (targetState) {
            is UiState.Loading -> LoadingScreen()

            is UiState.Error -> ErrorScreen(targetState.throwable)
            is UiState.Empty -> if (emptyAsSuccess != null) {
                successContent(emptyAsSuccess)
            } else {
                emptyContent()
            }

            is UiState.Success -> successContent(targetState.data)
        }
    }
}
