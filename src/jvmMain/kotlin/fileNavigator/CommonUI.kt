package fileNavigator

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class ScrollBoxState(
    val vertical: ScrollState,
    val horizontal: ScrollState,
    private val scope: CoroutineScope
) {
    fun scrollBy(dx: Float = 0f, dy: Float = 0f) = scope.launch {
        if (dx != 0f) horizontal.scrollBy(dx)
        if (dy != 0f) vertical.scrollBy(dy)
    }
}

@Composable
fun rememberScrollBoxState() =
    ScrollBoxState(
        vertical = rememberScrollState(0),
        horizontal = rememberScrollState(0),
        scope = rememberCoroutineScope()
    )

@Composable
fun ScrollableBox(
    state: ScrollBoxState,
    contentAlignment: Alignment = Alignment.TopStart,
    modifier: Modifier,
    inner: @Composable (Modifier) -> Unit
) = Box(
    contentAlignment = contentAlignment,
    modifier = modifier
        .onPreviewKeyEvent(state::scrollByKeys)
) {
    inner(
        Modifier.fillMaxSize()
            .padding(end = 8.dp, bottom = 8.dp)
            .verticalScroll(state.vertical)
            .horizontalScroll(state.horizontal)
    )

    VerticalScrollbar(
        rememberScrollbarAdapter(state.vertical),
        modifier = Modifier.fillMaxHeight()
            .align(Alignment.CenterEnd)
    )
    HorizontalScrollbar(
        rememberScrollbarAdapter(state.horizontal),
        modifier = Modifier.fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(end = 8.dp)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ScrollBoxState.scrollByKeys(event: KeyEvent): Boolean =
    event.type == KeyEventType.KeyDown && (mapOf(
        Key.DirectionLeft to { this.scrollBy(dx = -50f) },
        Key.DirectionRight to { this.scrollBy(dx = 50f) },
        Key.DirectionUp to { this.scrollBy(dy = -50f) },
        Key.DirectionDown to { this.scrollBy(dy = 50f) }
    )[event.key]
        ?.let { it(); true }
        ?: false)

@Composable
fun Modifier.highlightFocus(source: MutableInteractionSource) =
    this.border(
        width = 3.dp,
        color = if (source.collectIsFocusedAsState().value)
            MaterialTheme.colors.primaryVariant
        else MaterialTheme.colors.background
    )
