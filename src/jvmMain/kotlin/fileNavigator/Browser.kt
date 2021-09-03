package fileNavigator

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.nio.file.Path

class BrowserState(
    val selected: FSPreviewItem?,
    val toggle: (FSPreviewItem) -> Unit,
    val select: (FSPreviewItem) -> Unit,
    val items: List<FSPreviewItem>,
    val interactionSource: MutableInteractionSource,
) {
    fun selectFirst() {
        items.firstOrNull()?.apply(select)
    }

    fun selectLast() {
        items.lastOrNull()?.apply(select)
    }

    fun selectPrevious() {
        when (selected) {
            null -> selectFirst()
            else -> items.asSequence().zipWithNext()
                .find { (_, next) ->
                    next == selected
                }?.let { (previous, _) ->
                    select(previous)
                }
        }
    }

    fun selectNext() {
        when (selected) {
            null -> selectFirst()
            else -> items.asSequence().zipWithNext()
                .find { (previous, _) ->
                    previous == selected
                }?.let { (_, next) ->
                    select(next)
                }
        }
    }

    fun toggleSelected() {
        selected?.apply(toggle)
    }
}

@Composable
fun rememberBrowserState(
    root: Path
): BrowserState {
    var selected by remember { mutableStateOf<FSPreviewItem?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val (items, updateItems) = remember {
        mutableStateOf<List<FSPreviewItem>>(listOf())
    }
    val scope = rememberCoroutineScope()
    val fs = remember {
        FS(
            scope = scope,
            root = root,
            onChange = updateItems
        )
    }

    return BrowserState(
        items = items,
        selected = selected,
        toggle = { scope.launch {
            fs.toggle(it)
            // TODO: move selection if it was inside of the just folded dir
        }},
        select = { selected = it },
        interactionSource = interactionSource
    )
}

@Composable
fun Browser(
    state: BrowserState,
    modifier: Modifier = Modifier
) = Box(
    modifier = modifier.background(color = MaterialTheme.colors.background)
        .onPreviewKeyEvent(state::navigateByKeys)
        .focusable(interactionSource = state.interactionSource)
        .highlightFocus(state.interactionSource)
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = state.items, key = { it.path }) {
            Row {
                Pad(it.level)
                if (it.state == PreviewItemState.File) FileView(it, state)
                else FolderView(it, state)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun BrowserState.navigateByKeys(event: KeyEvent): Boolean =
    event.type == KeyEventType.KeyDown && (mapOf(
        Key.MoveHome to { selectFirst() },
        Key.MoveEnd to { selectLast() },
        Key.DirectionUp to { selectPrevious() },
        Key.DirectionDown to { selectNext() },
        Key.Spacebar to { toggleSelected() },
    )[event.key]
        ?.let { it(); true }
        ?: false)

@Composable
fun Pad(level: Int) = Spacer(
    Modifier.size(
        width = (20 * (level - 1)).dp,
        // ^ root level "0" is invisible so no offset needed
        height = 0.dp
    )
)

@Composable
fun FolderView(
    item: FSPreviewItem,
    state: BrowserState,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .highlightIf(state.selected == item)
            .clickable { state.select(item) }
    ) {
        Icon(
            when (item.state) {
                PreviewItemState.OpenedFolder -> Icons.Rounded.ArrowDropDown
                else -> Icons.Rounded.ArrowForward
            },
            contentDescription = null,
            modifier = Modifier.clickable { state.toggle(item) }
        )
        Text(item.name)
    }
}

@Composable
fun FileView(
    item: FSPreviewItem,
    state: BrowserState,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .highlightIf(state.selected == item)
        .clickable { state.select(item) }
) {
    Icon(Icons.Rounded.Check, contentDescription = null)
    Text(item.name)
}

fun Modifier.highlightIf(condition: Boolean): Modifier =
    if (condition)
        this.background(color = Color.LightGray)
    else this
