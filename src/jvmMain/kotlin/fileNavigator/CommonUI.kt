package fileNavigator

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScrollableBox(
    contentAlignment: Alignment = Alignment.TopStart,
    modifier: Modifier,
    inner: @Composable (Modifier) -> Unit
) = Box(
    contentAlignment = contentAlignment,
    modifier = modifier
) {
    val vScrollState = rememberScrollState(0)
    val hScrollState = rememberScrollState(0)

    inner(
        Modifier.fillMaxSize()
            .padding(end = 8.dp, bottom = 8.dp)
            .verticalScroll(vScrollState)
            .horizontalScroll(hScrollState)
    )

    VerticalScrollbar(
        rememberScrollbarAdapter(vScrollState),
        modifier = Modifier.fillMaxHeight()
            .align(Alignment.CenterEnd)
    )
    HorizontalScrollbar(
        rememberScrollbarAdapter(hScrollState),
        modifier = Modifier.fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(end = 8.dp)
    )
}
